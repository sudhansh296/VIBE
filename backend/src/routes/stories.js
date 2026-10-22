const express = require('express');
const multer = require('multer');
const { z } = require('zod');
const { Op } = require('sequelize');
const { User, Story, StoryView } = require('../models');
const settings = require('../services/settings');
const { canInteract } = require('../services/rules');
const { requireAuth } = require('../middleware/auth');
const files = require('../utils/files');
const { bad, forbidden, notFound, wrap } = require('../utils/errors');

/**
 * Status (stories): an approved host posts a photo or a video (as long as the profile video limit); it stays up for
 * status_hours. Users see the statuses of hosts they may talk to (same rules as profiles: not blocked, no host-to-host).
 * Files are served only to people who may see them; the poster sees who viewed each one.
 */
const r = express.Router();
const upload = multer({ storage: multer.memoryStorage(), limits: { fileSize: 60 * 1024 * 1024, files: 1 } });

const live = () => ({ expiresAt: { [Op.gt]: new Date() } });
const view = (s, seen = false) => ({
  id: Number(s.id), kind: s.kind, url: `/stories/${s.id}/file`, durationSec: s.durationSec || null, caption: s.caption || null,
  at: s.createdAt, expiresAt: s.expiresAt, seen,
});
const isPoster = (u) => u.role === 'host' && u.hostStatus === 'approved';

/** Removes my expired statuses (files too). */
async function sweep(userId) {
  const old = await Story.findAll({ where: { userId, expiresAt: { [Op.lte]: new Date() } } });
  for (const s of old) { files.remove(s.path); await StoryView.destroy({ where: { storyId: s.id } }); await s.destroy(); }
}

/** The status row on Home: hosts with something up, the ones with unseen statuses first. */
r.get('/stories', requireAuth, wrap(async (req, res) => {
  const rows = await Story.findAll({ where: live(), order: [['id', 'ASC']], limit: 1000 });
  const byUser = new Map();
  for (const s of rows) { if (!byUser.has(String(s.userId))) byUser.set(String(s.userId), []); byUser.get(String(s.userId)).push(s); }
  byUser.delete(String(req.user.id));                                   // my own are under "My status"
  const users = byUser.size ? await User.findAll({ where: { id: [...byUser.keys()], status: 'active', role: 'host', hostStatus: 'approved' } }) : [];
  const seen = rows.length ? new Set((await StoryView.findAll({ where: { viewerId: req.user.id, storyId: rows.map((s) => s.id) }, attributes: ['storyId'] })).map((v) => String(v.storyId))) : new Set();
  const out = [];
  for (const u of users) {
    if (!(await canInteract(req.user, u)).ok) continue;
    const [card] = await require('./social').cards([u], req.user);
    const list = byUser.get(String(u.id)).map((s) => view(s, seen.has(String(s.id))));
    out.push({ person: card, stories: list, allSeen: list.every((x) => x.seen), last: list[list.length - 1].at });
  }
  out.sort((a, b) => (a.allSeen - b.allSeen) || (new Date(b.last) - new Date(a.last)));
  res.json({ people: out.map(({ last, ...x }) => x), canPost: isPoster(req.user) });
}));

/** My statuses (a host): what is up now and how many saw each one. */
r.get('/stories/mine', requireAuth, wrap(async (req, res) => {
  await sweep(req.user.id);
  const rows = await Story.findAll({ where: { userId: req.user.id, ...live() }, order: [['id', 'ASC']] });
  const counts = new Map();
  for (const s of rows) counts.set(String(s.id), await StoryView.count({ where: { storyId: s.id } }));
  res.json({ stories: rows.map((s) => ({ ...view(s, true), views: counts.get(String(s.id)) || 0 })), canPost: isPoster(req.user) });
}));

/** Post a status: one photo or video (+ an optional caption). */
r.post('/stories', requireAuth, upload.single('file'), wrap(async (req, res) => {
  if (!isPoster(req.user)) throw forbidden('hosts_only', 'Only hosts can post a status');
  if (!req.file) throw bad('no_file', 'Choose a photo or video');
  const caption = z.string().trim().max(150).optional().parse(req.body && req.body.caption ? String(req.body.caption) : undefined);
  const g = async (k) => Number(await settings.get(k));
  const kind = files.sniff(req.file.buffer);
  if (!kind) throw bad('unsupported_file', 'Only JPG, PNG, WEBP photos or MP4 videos');
  const mb = req.file.size / (1024 * 1024);
  let durationSec = null;
  if (kind.kind === 'photo') { if (mb > await g('media_max_photo_mb')) throw bad('file_too_large', `A photo can be at most ${await g('media_max_photo_mb')} MB`); }
  else {
    if (mb > await g('media_max_video_mb')) throw bad('file_too_large', `A video can be at most ${await g('media_max_video_mb')} MB`);
    durationSec = files.mp4DurationSeconds(req.file.buffer);
    if (durationSec === null) throw bad('unreadable_video', 'Could not read this video, try recording it again');
    const max = await g('media_max_video_seconds');
    if (durationSec > max) throw bad('video_too_long', `A status video can be at most ${max} seconds`);
    if (durationSec < 1) throw bad('video_too_short', 'This video is too short');
  }
  await sweep(req.user.id);
  if (await Story.count({ where: { userId: req.user.id, ...live() } }) >= await g('status_max_active')) throw bad('limit_reached', 'You have the most statuses allowed up right now');
  const s = await Story.create({
    userId: req.user.id, kind: kind.kind, path: 'pending', mime: kind.mime, sizeBytes: req.file.size, durationSec, caption: caption || null,
    expiresAt: new Date(Date.now() + (await g('status_hours')) * 3_600_000),
  });
  const rel = `stories/${req.user.id}/${s.id}.${kind.ext}`;
  files.save(rel, req.file.buffer);
  await s.update({ path: rel });
  res.json({ story: { ...view(s, true), views: 0 } });
}));

/** The file, only for the poster and people allowed to see it, only while it is up. */
r.get('/stories/:id/file', requireAuth, wrap(async (req, res) => {
  const s = await Story.findOne({ where: { id: Number(req.params.id) || 0, ...live() } });
  if (!s) throw notFound('story_not_found', 'This status is gone');
  if (String(s.userId) !== String(req.user.id)) {
    const owner = await User.findByPk(s.userId);
    if (!owner || !(await canInteract(req.user, owner)).ok) throw notFound('story_not_found', 'This status is gone');
  }
  res.sendFile(files.absPath(s.path)); // answers Range requests, so videos stream
}));

/** I watched it (the poster's own views do not count). */
r.post('/stories/:id/view', requireAuth, wrap(async (req, res) => {
  const s = await Story.findOne({ where: { id: Number(req.params.id) || 0, ...live() } });
  if (!s) throw notFound('story_not_found', 'This status is gone');
  if (String(s.userId) !== String(req.user.id)) {
    const owner = await User.findByPk(s.userId);
    if (!owner || !(await canInteract(req.user, owner)).ok) throw notFound('story_not_found', 'This status is gone');
    await StoryView.findOrCreate({ where: { storyId: s.id, viewerId: req.user.id } });
  }
  res.json({ ok: true });
}));

/** Who saw my status. */
r.get('/stories/:id/viewers', requireAuth, wrap(async (req, res) => {
  const s = await Story.findOne({ where: { id: Number(req.params.id) || 0, userId: req.user.id } });
  if (!s) throw notFound('story_not_found', 'Not found');
  const views = await StoryView.findAll({ where: { storyId: s.id }, order: [['id', 'DESC']], limit: 500 });
  const users = views.length ? await User.findAll({ where: { id: views.map((v) => v.viewerId), status: 'active' } }) : [];
  const cards = await require('./social').cards(users, req.user);
  const byId = new Map(users.map((u, i) => [String(u.id), cards[i]]));
  res.json({ viewers: views.filter((v) => byId.get(String(v.viewerId))).map((v) => ({ ...byId.get(String(v.viewerId)), viewedAt: v.createdAt })) });
}));

r.delete('/stories/:id', requireAuth, wrap(async (req, res) => {
  const s = await Story.findOne({ where: { id: Number(req.params.id) || 0, userId: req.user.id } });
  if (!s) throw notFound('story_not_found', 'Not found');
  files.remove(s.path); await StoryView.destroy({ where: { storyId: s.id } }); await s.destroy();
  res.json({ ok: true });
}));

module.exports = r;
