const express = require('express');
const { z } = require('zod');
const { Op, fn, col, where } = require('sequelize');
const { User, HostProfile, HostApplication, Follow, Visit, Block, Call, Gift, sequelize } = require('../models');
const { canInteract } = require('../services/rules');
const { requireAuth } = require('../middleware/auth');
const { bad, notFound, forbidden, wrap } = require('../utils/errors');
const { publicUser } = require('./me');
const { publicUserMediaList } = require('./media');
const { ageFromDob } = require('../utils/age');

const r = express.Router();

const NEW_DAYS = 7;

/** People the viewer has a block with (either direction): never shown in any list. */
async function blockedWith(viewerId) {
  const rows = await Block.findAll({ where: { [Op.or]: [{ blockerId: viewerId }, { blockedId: viewerId }] }, attributes: ['blockerId', 'blockedId'] });
  return rows.map((b) => (String(b.blockerId) === String(viewerId) ? b.blockedId : b.blockerId));
}

const dobFor = (years) => {
  const d = new Date(); d.setUTCFullYear(d.getUTCFullYear() - years);
  return d.toISOString().slice(0, 10);
};

/** A card for one person. A host's card also carries the price and the online switch. */
async function cards(users, viewer) {
  if (!users.length) return [];
  const ids = users.map((u) => u.id);
  const [profiles, apps, followerRows, mine] = await Promise.all([
    HostProfile.findAll({ where: { userId: ids } }),
    HostApplication.findAll({ where: { userId: ids }, attributes: ['userId', 'reviewedAt'] }),
    Follow.findAll({ where: { followedId: ids }, attributes: ['followedId', [fn('COUNT', col('id')), 'n']], group: ['followedId'], raw: true }),
    Follow.findAll({ where: { followerId: viewer.id, followedId: ids }, attributes: ['followedId'], raw: true }),
  ]);
  const prof = new Map(profiles.map((p) => [String(p.userId), p]));
  const rev = new Map(apps.map((a) => [String(a.userId), a.reviewedAt]));
  const fol = new Map(followerRows.map((f) => [String(f.followedId), Number(f.n)]));
  const mineSet = new Set(mine.map((f) => String(f.followedId)));
  const out = [];
  for (const u of users) {
    const card = { ...(await publicUser(u)), followers: fol.get(String(u.id)) || 0, following: mineSet.has(String(u.id)) };
    if (u.role === 'host') {
      const p = prof.get(String(u.id));
      const approvedAt = rev.get(String(u.id));
      Object.assign(card, {
        callRate: p ? await require('../services/vip').callRateFor(viewer, p.callRate) : null,
        online: !!(p && p.isOnline),
        busy: require('../services/ringing').isBusy(u.id),
        rating: p && p.ratingCount ? Math.round((p.ratingSum / p.ratingCount) * 10) / 10 : null,
        ratingCount: p ? p.ratingCount : 0,
        isNew: !!(approvedAt && Date.now() - new Date(approvedAt).getTime() < NEW_DAYS * 86_400_000),
      });
    }
    else if (viewer.role === 'host') {
      Object.assign(card, { online: require('../realtime').isConnected(u.id), busy: require('../services/ringing').isBusy(u.id) });
    }
    out.push(card);
  }
  return out;
}

const Q = z.object({
  tab: z.enum(['discover', 'following', 'new']).default('discover'),
  gender: z.enum(['male', 'female', 'other']).optional(),
  online: z.enum(['1', '0']).optional(),
  ageMin: z.coerce.number().int().min(18).max(99).optional(),
  ageMax: z.coerce.number().int().min(18).max(99).optional(),
  maxPrice: z.coerce.number().int().min(1).max(100000).optional(),
  languages: z.string().max(200).optional(),
  q: z.string().trim().max(30).optional(),
  page: z.coerce.number().int().min(1).max(500).default(1),
}).strict();

/**
 * The home list. A user sees approved hosts; a host sees users; nobody sees a host-to-host list.
 * People with a block (either way) and the viewer himself never appear.
 */
r.get('/discover', requireAuth, wrap(async (req, res) => {
  const f = Q.parse(req.query);
  const viewer = req.user;
  const wantRole = viewer.role === 'host' ? 'user' : 'host';
  const PAGE = 20;
  const hide = [viewer.id, ...(await blockedWith(viewer.id))];
  const w = { [Op.and]: [{ id: { [Op.notIn]: hide } }, { status: 'active' }, { role: wantRole }, { name: { [Op.ne]: null } }] };
  if (wantRole === 'host') w[Op.and].push({ hostStatus: 'approved' });
  if (f.gender) w[Op.and].push({ gender: f.gender });
  if (f.ageMax) w[Op.and].push({ dob: { [Op.gt]: dobFor(f.ageMax + 1) } });
  if (f.ageMin) w[Op.and].push({ dob: { [Op.lte]: dobFor(f.ageMin) } });
  if (f.q) w[Op.and].push({ [Op.or]: [{ name: { [Op.like]: `%${f.q.replace(/[%_]/g, '')}%` } }, { city: { [Op.like]: `%${f.q.replace(/[%_]/g, '')}%` } }] });
  if (f.languages) {
    for (const lang of f.languages.split(',').map((x) => x.trim()).filter(Boolean).slice(0, 6)) {
      w[Op.and].push(where(col('User.languages'), Op.like, `%"${lang.replace(/["%_\\]/g, '')}"%`));
    }
  }
  if (f.tab === 'following') {
    const ids = (await Follow.findAll({ where: { followerId: viewer.id }, attributes: ['followedId'] })).map((x) => x.followedId);
    w[Op.and].push({ id: { [Op.in]: ids.length ? ids : [0] } });
  }
  const include = [];
  const hostWhere = {};
  if (wantRole === 'host') {
    if (f.online === '1') hostWhere.isOnline = true;
    if (f.maxPrice) hostWhere.callRate = { [Op.lte]: f.maxPrice };
    include.push({ model: HostProfile, required: Object.keys(hostWhere).length > 0, where: Object.keys(hostWhere).length ? hostWhere : undefined });
  }
  if (f.tab === 'new' && wantRole === 'host') {
    include.push({ model: HostApplication, required: true, where: { reviewedAt: { [Op.gt]: new Date(Date.now() - NEW_DAYS * 86_400_000) } }, attributes: [] });
  }
  const rows = await User.findAll({
    where: w, include, limit: PAGE + 1, offset: (f.page - 1) * PAGE,
    order: wantRole === 'host' ? [[HostProfile, 'isOnline', 'DESC'], [HostProfile, 'onlineAt', 'DESC'], ['id', 'ASC']] : [['lastSeenAt', 'DESC'], ['id', 'ASC']],
    subQuery: false,
  });
  const more = rows.length > PAGE;
  const page = rows.slice(0, PAGE);
  res.json({ people: await cards(page, viewer), nextPage: more ? f.page + 1 : null });
}));

// ---------------------------------------------------------------- one profile (and the visit it counts as)
const pid = z.coerce.number().int().positive();
async function target(req) {
  const u = await User.findOne({ where: { publicId: pid.parse(req.params.id), status: 'active' } });
  if (!u) throw notFound('user_not_found', 'Not found');
  // only a user <-> host pair can ever look at each other (never user-to-user, never host-to-host)
  if (req.user.role !== 'host' && u.role !== 'host') throw notFound('user_not_found', 'Not found');
  const c = await canInteract(req.user, u);
  if (!c.ok) throw notFound('user_not_found', 'Not found'); // blocked / host-to-host / inactive look exactly like "does not exist"
  if (u.role === 'host' && u.hostStatus !== 'approved') throw notFound('user_not_found', 'Not found');
  return u;
}

r.get('/profile/:id', requireAuth, wrap(async (req, res) => {
  const u = await target(req);
  const day = new Date().toISOString().slice(0, 10);
  await Visit.findOrCreate({ where: { visitedId: u.id, visitorId: req.user.id, day } });
  const [card] = await cards([u], req.user);
  const out = { person: { ...card, bio: u.bio || '' } };
  if (u.role === 'host') {
    out.media = await publicUserMediaList(u.id, req.user.id, false);
    out.stats = await hostStats(u.id);
  }
  res.json(out);
}));

// tags users pick after a good call; only these show on a profile ("What people say")
const GOOD_TAGS = ['Friendly', 'Great vibe', 'Funny', 'Smart', 'Good listener', 'Respectful'];

/** A host's public numbers: calls taken, answer rate, gifts received, what people say. */
async function hostStats(hostId) {
  // calls users made to this host (not the host's own calls, not ones the caller cancelled before an answer)
  const asked = await Call.findAll({ where: { hostId, byHost: false, status: 'ended', endReason: { [Op.ne]: 'cancelled' } }, attributes: ['acceptedAt', 'seconds', 'ratingTags'] });
  const answered = asked.filter((c) => c.acceptedAt).length;
  const calls = await Call.count({ where: { hostId, status: 'ended', seconds: { [Op.gt]: 0 } } });
  const tagCount = new Map();
  const rated = await Call.findAll({ where: { hostId, rating: { [Op.gte]: 4 } }, attributes: ['ratingTags'] });
  for (const c of rated) for (const t of c.ratingTags || []) if (GOOD_TAGS.includes(t)) tagCount.set(t, (tagCount.get(t) || 0) + 1);
  const rows = await sequelize.query(
    `SELECT m.giftId AS giftId, SUM(m.quantity) AS n FROM messages m JOIN conversations c ON c.id = m.conversationId
     WHERE c.hostId = :hostId AND m.kind = 'gift' AND m.senderId <> :hostId AND m.giftId IS NOT NULL GROUP BY m.giftId ORDER BY n DESC`,
    { replacements: { hostId }, type: sequelize.QueryTypes.SELECT },
  );
  const giftRows = rows.length ? await Gift.findAll({ where: { id: rows.map((r) => r.giftId) } }) : [];
  const byId = new Map(giftRows.map((g) => [g.id, g]));
  const gifts = rows.map((r) => { const g = byId.get(r.giftId); return g && { name: g.name, emoji: g.emoji || null, count: Number(r.n) }; }).filter(Boolean);
  return {
    calls,
    answerRate: asked.length ? Math.round((answered / asked.length) * 100) : null,   // null = nobody asked yet
    giftsTotal: gifts.reduce((s, g) => s + g.count, 0),
    gifts: gifts.slice(0, 8),
    tags: [...tagCount.entries()].sort((a, b) => b[1] - a[1]).map(([tag, count]) => ({ tag, count })),
  };
}

// ---------------------------------------------------------------- follow
r.post('/follow/:id', requireAuth, wrap(async (req, res) => {
  const u = await target(req);
  const [, created] = await Follow.findOrCreate({ where: { followerId: req.user.id, followedId: u.id } });
  if (created && u.role === 'host') await require('../services/notify').notify(u.id, 'follower', `${req.user.name || 'Someone'} started following you`, null, { peerId: Number(req.user.publicId) });
  res.json({ following: true, followers: await Follow.count({ where: { followedId: u.id } }) });
}));
r.delete('/follow/:id', requireAuth, wrap(async (req, res) => {
  const u = await User.findOne({ where: { publicId: pid.parse(req.params.id) } });
  if (!u) throw notFound('user_not_found', 'Not found');
  await Follow.destroy({ where: { followerId: req.user.id, followedId: u.id } });
  res.json({ following: false, followers: await Follow.count({ where: { followedId: u.id } }) });
}));

async function listPeople(req, res, mineIsFollower) {
  const rows = await Follow.findAll({ where: mineIsFollower ? { followerId: req.user.id } : { followedId: req.user.id }, order: [['id', 'DESC']], limit: 200 });
  const ids = rows.map((x) => (mineIsFollower ? x.followedId : x.followerId));
  const hide = new Set((await blockedWith(req.user.id)).map(String));
  const users = (await User.findAll({ where: { id: ids, status: 'active' } })).filter((u) => !hide.has(String(u.id)) && (u.role !== 'host' || u.hostStatus === 'approved'));
  const order = new Map(ids.map((id, i) => [String(id), i]));
  users.sort((a, b) => order.get(String(a.id)) - order.get(String(b.id)));
  res.json({ people: await cards(users, req.user) });
}
r.get('/me/following', requireAuth, wrap((req, res) => listPeople(req, res, true)));
r.get('/me/followers', requireAuth, wrap((req, res) => listPeople(req, res, false)));

// ---------------------------------------------------------------- the three numbers on the Me page
r.get('/me/counts', requireAuth, wrap(async (req, res) => {
  const hide = (await blockedWith(req.user.id)).map(String);
  const notHidden = (col) => (hide.length ? { [col]: { [Op.notIn]: hide } } : {});
  const [following, followers, visitors] = await Promise.all([
    Follow.count({ where: { followerId: req.user.id, ...notHidden('followedId') } }),
    Follow.count({ where: { followedId: req.user.id, ...notHidden('followerId') } }),
    Visit.count({ where: { visitedId: req.user.id, ...notHidden('visitorId') }, distinct: true, col: 'visitorId' }),
  ]);
  res.json({ following, followers, visitors });
}));

// ---------------------------------------------------------------- visitors (always shown: it keeps people motivated)
r.get('/me/visitors', requireAuth, wrap(async (req, res) => {
  const hide = new Set((await blockedWith(req.user.id)).map(String));
  const rows = await Visit.findAll({ where: { visitedId: req.user.id }, order: [['id', 'DESC']], limit: 300 });
  const seen = new Map();
  for (const v of rows) if (!seen.has(String(v.visitorId))) seen.set(String(v.visitorId), v);
  const users = (await User.findAll({ where: { id: [...seen.keys()], status: 'active' } })).filter((u) => !hide.has(String(u.id)));
  const byId = new Map(users.map((u) => [String(u.id), u]));
  const list = [];
  for (const [uid, v] of seen) {
    const u = byId.get(uid);
    if (!u) continue;
    const times = rows.filter((x) => String(x.visitorId) === uid).length;
    list.push({ ...(await publicUser(u)), visitedAt: v.createdAt, visits: times });
  }
  if (req.user.role !== 'host' && !require('../services/vip').isVip(req.user)) {
    // not VIP: only how many, never who
    return res.json({ total: list.length, locked: true, visitors: [] });
  }
  res.json({ total: list.length, locked: false, visitors: list });
}));

module.exports = r;
module.exports.cards = cards;
