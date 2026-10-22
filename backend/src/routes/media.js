const express = require('express');
const multer = require('multer');
const { z } = require('zod');
const { User, Wallet, Transaction, Media, MediaUnlock, HostProfile, sequelize } = require('../models');
const settings = require('../services/settings');
const { canInteract } = require('../services/rules');
const { requireAuth } = require('../middleware/auth');
const files = require('../utils/files');
const { bad, forbidden, notFound, conflict, wrap, ApiError } = require('../utils/errors');
const fs = require('fs');

const r = express.Router();

// ---------------------------------------------------------------- limits and ranks
async function limits() {
  const g = async (k) => Number(await settings.get(k));
  return {
    maxPhotos: await g('media_max_photos'), maxVideos: await g('media_max_videos'),
    freePhotos: await g('media_free_photos'), freeVideos: await g('media_free_videos'),
    pricePhoto: await g('media_price_photo'), priceVideo: await g('media_price_video'),
    maxPhotoMb: await g('media_max_photo_mb'), maxVideoMb: await g('media_max_video_mb'),
    maxVideoSeconds: await g('media_max_video_seconds'),
    minPhotos: await g('host_min_photos'), minVideos: await g('host_min_videos'),
  };
}

/** A person's media in display order: the main photo first, then the oldest. Rank decides what is always free. */
async function ownMedia(userId) {
  const rows = await Media.findAll({ where: { userId }, order: [['id', 'ASC']] });
  const photos = rows.filter((m) => m.kind === 'photo').sort((a, b) => Number(b.isMain) - Number(a.isMain) || a.id - b.id);
  const videos = rows.filter((m) => m.kind === 'video');
  return { photos, videos };
}

const isAlwaysFree = (list, m, l) => list.findIndex((x) => x.id === m.id) < (m.kind === 'photo' ? l.freePhotos : l.freeVideos); // compare ids: m may be a separately loaded row
const isLocked = (list, m, l) => m.locked && !isAlwaysFree(list, m, l);
const priceOf = (m, l) => (m.kind === 'photo' ? l.pricePhoto : l.priceVideo);

/** What the app may know about one file. `unlocked` = the viewer already paid (or owns it). */
function view(list, m, l, { unlocked = false, owner = false } = {}) {
  const locked = isLocked(list, m, l);
  return {
    id: Number(m.id), kind: m.kind, durationSec: m.durationSec, isMain: m.isMain,
    free: isAlwaysFree(list, m, l),
    locked: locked && !unlocked && !owner,   // true = the viewer must pay before the file opens
    lockedByHost: locked,                    // owner's own switch (so the owner can show the lock)
    price: locked ? priceOf(m, l) : 0,
    url: `/media/${m.id}/file`,
  };
}

async function publicUserMediaList(ownerId, viewerId, owner) {
  const l = await limits();
  const { photos, videos } = await ownMedia(ownerId);
  const unlocked = owner ? new Set() : new Set((await MediaUnlock.findAll({ where: { userId: viewerId } })).map((u) => String(u.mediaId)));
  const map = (list) => list.map((m) => view(list, m, l, { unlocked: unlocked.has(String(m.id)), owner }));
  return { photos: map(photos), videos: map(videos), limits: l };
}

// ---------------------------------------------------------------- the owner's own gallery
r.get('/me/media', requireAuth, wrap(async (req, res) => {
  res.json(await publicUserMediaList(req.user.id, req.user.id, true));
}));

const upload = multer({ storage: multer.memoryStorage(), limits: { fileSize: 60 * 1024 * 1024, files: 1 } });

// the DP is a public copy of the main photo (the original stays private); it follows "Set as main photo"
async function syncDpFromMain(user) {
  const { photos } = await ownMedia(user.id);
  const main = photos.find((p) => p.isMain);
  const old = user.dpPath;
  if (!main) { await user.update({ dpPath: null }); files.remove(old); return; }
  const ext = main.path.split('.').pop();
  const rel = `dp/${user.id}_${Date.now()}.${ext}`;
  files.save(rel, fs.readFileSync(files.absPath(main.path)));
  await user.update({ dpPath: rel });
  if (old && old.startsWith('dp/')) files.remove(old);
}

r.post('/me/media', requireAuth, upload.single('file'), wrap(async (req, res) => {
  if (!req.file) throw bad('no_file', 'Choose a photo or video');
  const l = await limits();
  const kind = files.sniff(req.file.buffer);
  if (!kind) throw bad('unsupported_file', 'Only JPG, PNG, WEBP photos or MP4 videos');
  const mb = req.file.size / (1024 * 1024);
  let durationSec = null;
  if (kind.kind === 'photo') {
    if (mb > l.maxPhotoMb) throw bad('file_too_large', `A photo can be at most ${l.maxPhotoMb} MB`);
  } else {
    if (mb > l.maxVideoMb) throw bad('file_too_large', `A video can be at most ${l.maxVideoMb} MB`);
    durationSec = files.mp4DurationSeconds(req.file.buffer);
    if (durationSec === null) throw bad('unreadable_video', 'Could not read this video, try recording it again');
    if (durationSec > l.maxVideoSeconds) throw bad('video_too_long', `A video can be at most ${l.maxVideoSeconds} seconds`);
    if (durationSec < 1) throw bad('video_too_short', 'This video is too short');
  }
  const have = await Media.count({ where: { userId: req.user.id, kind: kind.kind } });
  if (have >= (kind.kind === 'photo' ? l.maxPhotos : l.maxVideos)) {
    throw bad('limit_reached', `You can have at most ${kind.kind === 'photo' ? l.maxPhotos : l.maxVideos} ${kind.kind}s`);
  }
  const m = await Media.create({ userId: req.user.id, kind: kind.kind, path: 'pending', mime: kind.mime, sizeBytes: req.file.size, durationSec });
  const rel = `media/${req.user.id}/${m.id}.${kind.ext}`;
  files.save(rel, req.file.buffer);
  await m.update({ path: rel });
  // the first photo becomes the main photo (and so the DP)
  if (kind.kind === 'photo' && !(await Media.findOne({ where: { userId: req.user.id, kind: 'photo', isMain: true } }))) {
    await m.update({ isMain: true });
    await syncDpFromMain(req.user);
  }
  res.json({ ...(await publicUserMediaList(req.user.id, req.user.id, true)), added: Number(m.id) });
}));

async function ownById(req) {
  const id = z.coerce.number().int().positive().parse(req.params.id);
  const m = await Media.findOne({ where: { id, userId: req.user.id } });
  if (!m) throw notFound('media_not_found', 'Not found');
  return m;
}

/** Lock / unlock a file (users pay coins to open a locked one). The always-free ones cannot be locked. */
r.put('/me/media/:id', requireAuth, wrap(async (req, res) => {
  const { locked } = z.object({ locked: z.boolean() }).strict().parse(req.body);
  const m = await ownById(req);
  const l = await limits();
  const { photos, videos } = await ownMedia(req.user.id);
  const list = m.kind === 'photo' ? photos : videos;
  if (locked && isAlwaysFree(list, m, l)) throw forbidden('always_free', `The first ${m.kind === 'photo' ? l.freePhotos + ' photos' : l.freeVideos + ' video'} are always free`);
  await m.update({ locked });
  res.json(await publicUserMediaList(req.user.id, req.user.id, true));
}));

r.post('/me/media/:id/main', requireAuth, wrap(async (req, res) => {
  const m = await ownById(req);
  if (m.kind !== 'photo') throw bad('not_a_photo', 'Only a photo can be the main photo');
  const l = await limits();
  const { photos } = await ownMedia(req.user.id);
  if (isLocked(photos, m, l)) throw bad('locked_photo', 'Make this photo free first, the main photo is public');
  await Media.update({ isMain: false }, { where: { userId: req.user.id, kind: 'photo' } });
  await m.update({ isMain: true, locked: false });
  await syncDpFromMain(req.user);
  res.json({ ...(await publicUserMediaList(req.user.id, req.user.id, true)), user: undefined });
}));

r.delete('/me/media/:id', requireAuth, wrap(async (req, res) => {
  const m = await ownById(req);
  const l = await limits();
  if (req.user.role === 'host') {
    const n = await Media.count({ where: { userId: req.user.id, kind: m.kind } });
    const min = m.kind === 'photo' ? l.minPhotos : l.minVideos;
    if (n <= min) throw forbidden('minimum_media', `A host must keep at least ${min} ${m.kind}${min > 1 ? 's' : ''}. Add another first, then delete this one.`);
  }
  const wasMain = m.isMain;
  files.remove(m.path);
  await MediaUnlock.destroy({ where: { mediaId: m.id } });
  await m.destroy();
  if (wasMain) {
    const { photos } = await ownMedia(req.user.id);
    if (photos[0]) await photos[0].update({ isMain: true, locked: false });
    await syncDpFromMain(req.user);
  }
  res.json(await publicUserMediaList(req.user.id, req.user.id, true));
}));

// ---------------------------------------------------------------- opening someone's file
/** Loads a file and works out whether the person asking may see it. Never reveals a path. */
async function accessFor(viewer, id) {
  const m = await Media.findByPk(id);
  if (!m) throw notFound('media_not_found', 'Not found');
  const owner = await User.findByPk(m.userId);
  const l = await limits();
  const { photos, videos } = await ownMedia(owner.id);
  const list = m.kind === 'photo' ? photos : videos;
  if (viewer.id === owner.id || viewer.role === 'admin') return { m, owner, l, list, ok: true };
  // strangers only ever see approved hosts' media, and only when the two may interact (not blocked, not host-to-host)
  if (owner.role !== 'host' || owner.hostStatus !== 'approved') throw notFound('media_not_found', 'Not found');
  const c = await canInteract(viewer, owner);
  if (!c.ok) throw notFound('media_not_found', 'Not found');
  if (!isLocked(list, m, l)) return { m, owner, l, list, ok: true };
  const paid = await MediaUnlock.findOne({ where: { userId: viewer.id, mediaId: m.id } });
  return { m, owner, l, list, ok: !!paid };
}

r.get('/media/:id/file', requireAuth, wrap(async (req, res) => {
  const a = await accessFor(req.user, z.coerce.number().int().positive().parse(req.params.id));
  if (!a.ok) throw new ApiError(403, 'locked', `Unlock this ${a.m.kind} for ${priceOf(a.m, a.l)} coins to see it`);
  res.set('Cache-Control', 'private, max-age=3600');
  res.set('X-Content-Type-Options', 'nosniff');
  res.type(a.m.mime);
  res.sendFile(files.absPath(a.m.path)); // sendFile also answers Range requests, so videos can be played and skipped
}));

/** Pay coins to open a locked file. One database transaction: the buyer is charged, the host earns, both are written to the ledger. */
r.post('/media/:id/unlock', requireAuth, wrap(async (req, res) => {
  const id = z.coerce.number().int().positive().parse(req.params.id);
  const a = await accessFor(req.user, id);
  if (a.owner.id === req.user.id) throw bad('own_media', 'This is your own file');
  if (!isLocked(a.list, a.m, a.l)) return res.json({ ok: true, alreadyOpen: true, coins: Number((await Wallet.findByPk(req.user.id)).coins) });
  const price = priceOf(a.m, a.l);
  const share = Number(await settings.get('host_share_pct'));
  const hostGets = Math.floor((price * share) / 100);
  const coinsLeft = await sequelize.transaction(async (t) => {
    const lock = t.LOCK.UPDATE;
    // lock both wallets in a fixed order so two payments at once cannot deadlock or double-spend
    const ids = [req.user.id, a.owner.id].sort((x, y) => Number(x) - Number(y));
    const wallets = {};
    for (const uid of ids) wallets[uid] = await Wallet.findByPk(uid, { transaction: t, lock });
    const buyer = wallets[req.user.id];
    const host = wallets[a.owner.id];
    if (await MediaUnlock.findOne({ where: { userId: req.user.id, mediaId: a.m.id }, transaction: t })) return null; // paid before: no second charge
    if (Number(buyer.coins) < price) throw new ApiError(402, 'not_enough_coins', `You need ${price} coins`);
    await buyer.update({ coins: Number(buyer.coins) - price }, { transaction: t });
    await host.update({ earnings: Number(host.earnings) + hostGets }, { transaction: t });
    await MediaUnlock.create({ userId: req.user.id, mediaId: a.m.id, price }, { transaction: t });
    await Transaction.create({ userId: req.user.id, wallet: 'coins', kind: 'media_unlock', coins: -price, balanceAfter: Number(buyer.coins), ref: { mediaId: Number(a.m.id), hostId: Number(a.owner.id) } }, { transaction: t });
    await Transaction.create({ userId: a.owner.id, wallet: 'earnings', kind: 'media_earn', coins: hostGets, balanceAfter: Number(host.earnings), ref: { mediaId: Number(a.m.id), buyerId: Number(req.user.id), price, sharePct: share } }, { transaction: t });
    return Number(buyer.coins);
  });
  const coins = coinsLeft === null ? Number((await Wallet.findByPk(req.user.id)).coins) : coinsLeft;
  res.json({ ok: true, alreadyOpen: coinsLeft === null, coins, price });
}));

module.exports = r;
module.exports.publicUserMediaList = publicUserMediaList;
module.exports.limits = limits;
module.exports.ownMedia = ownMedia;
module.exports.syncDpFromMain = syncDpFromMain;
