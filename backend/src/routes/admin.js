const express = require('express');
const { z } = require('zod');
const { User, HostApplication, HostProfile, Media, CoinPack, Payment, Gift } = require('../models');
const settings = require('../services/settings');
const hostReview = require('../services/hostReview');
const { requireAuth, requireRole } = require('../middleware/auth');
const files = require('../utils/files');
const pii = require('../utils/pii');
const { bad, notFound, conflict, wrap } = require('../utils/errors');
const { limits, syncDpFromMain } = require('./media');

// The admin panel (Phase 6) will call these. Only role "admin" gets in, and every request is checked again on the server.
const r = express.Router();
r.use('/admin', requireAuth, requireRole('admin'));

async function userByPublicId(publicId) {
  const u = await User.findOne({ where: { publicId } });
  if (!u) throw notFound('user_not_found', 'User not found');
  return u;
}
const pid = z.coerce.number().int().positive();

r.get('/admin/applications', wrap(async (req, res) => {
  const status = z.enum(['pending', 'approved', 'rejected']).default('pending').parse(req.query.status || 'pending');
  const users = await User.findAll({ where: { hostStatus: status }, order: [['updatedAt', 'ASC']], limit: 100 });
  const apps = await HostApplication.findAll({ where: { userId: users.map((u) => u.id) } });
  const byUser = new Map(apps.map((a) => [String(a.userId), a]));
  res.json({
    applications: users.map((u) => {
      const a = byUser.get(String(u.id));
      return { userId: Number(u.publicId), name: u.name, city: u.city, submittedAt: a && a.submittedAt, status: u.hostStatus };
    }),
  });
}));

/** The full application, with the real PAN. Only an admin sees this. */
r.get('/admin/applications/:id', wrap(async (req, res) => {
  const u = await userByPublicId(pid.parse(req.params.id));
  const a = await HostApplication.findByPk(u.id);
  if (!a) throw notFound('no_application', 'No application');
  const media = await Media.findAll({ where: { userId: u.id }, order: [['id', 'ASC']] });
  res.json({
    user: { id: Number(u.publicId), name: u.name, gender: u.gender, dob: u.dob, city: u.city, phone: u.phone, status: u.hostStatus },
    application: {
      bio: a.bio, languages: a.languages, panName: a.panName, pan: a.panEnc ? pii.decrypt(a.panEnc) : null, idType: a.idType,
      idNumber: a.idNumberEnc ? pii.decrypt(a.idNumberEnc) : null, // Aadhaar: only XXXX XXXX 1234
      agencyCode: a.agencyCode, submittedAt: a.submittedAt, rejectReason: a.rejectReason,
      selfieUrl: a.selfiePath ? `/admin/applications/${u.publicId}/file/selfie` : null,
      idUrl: a.idPath ? `/admin/applications/${u.publicId}/file/id` : null,
    },
    media: media.map((m) => ({ id: Number(m.id), kind: m.kind, durationSec: m.durationSec, url: `/media/${m.id}/file` })),
  });
}));

r.get('/admin/applications/:id/file/:kind', wrap(async (req, res) => {
  const u = await userByPublicId(pid.parse(req.params.id));
  const a = await HostApplication.findByPk(u.id);
  const rel = a && (req.params.kind === 'selfie' ? a.selfiePath : req.params.kind === 'id' ? a.idPath : null);
  if (!rel) throw notFound('file_not_found', 'Not found');
  res.set('Cache-Control', 'no-store');
  res.set('X-Content-Type-Options', 'nosniff');
  res.sendFile(files.absPath(rel));
}));

r.post('/admin/applications/:id/approve', wrap(async (req, res) => {
  await hostReview.approve(await userByPublicId(pid.parse(req.params.id)), req.user.id);
  res.json({ ok: true, status: 'approved' });
}));

r.post('/admin/applications/:id/reject', wrap(async (req, res) => {
  const { reason } = z.object({ reason: z.string().trim().min(3).max(200) }).strict().parse(req.body);
  await hostReview.reject(await userByPublicId(pid.parse(req.params.id)), reason, req.user.id);
  res.json({ ok: true, status: 'rejected' });
}));

// ---------------------------------------------------------------- settings: every number that decides money or limits
r.get('/admin/settings', wrap(async (req, res) => {
  res.json({ settings: await settings.listAll() });
}));

r.put('/admin/settings/:key', wrap(async (req, res) => {
  const body = z.object({ value: z.any(), confirm: z.boolean().optional() }).strict().parse(req.body);
  if (body.value === undefined) throw bad('invalid_value', 'Send a value');
  if (settings.DANGEROUS.has(req.params.key) && body.confirm !== true) {
    throw bad('confirm_needed', 'This can stop the app for everyone. Send confirm: true to be sure.');
  }
  try { await settings.update(req.params.key, body.value, req.user.id); } catch (e) {
    if (e instanceof settings.SettingError) throw bad(e.code, e.message);
    throw e;
  }
  res.json({ ok: true, settings: await settings.listAll() });
}));

// ---------------------------------------------------------------- coin packs (prices in rupees here; stored in paise)
const packIn = z.object({
  coins: z.number().int().min(1).max(10_000_000),
  bonusCoins: z.number().int().min(0).max(10_000_000).default(0),
  priceInr: z.number().positive().max(100_000).refine((v) => Math.abs(v * 100 - Math.round(v * 100)) < 1e-6, 'At most 2 decimals (paise)'),
  tag: z.string().trim().max(20).nullable().optional(),
  active: z.boolean().default(true),
  sort: z.number().int().min(0).max(1000).default(0),
}).strict();
const packOut = (p) => ({ id: p.id, coins: p.coins, bonusCoins: p.bonusCoins, priceInr: p.pricePaise / 100, tag: p.tag, active: p.active, sort: p.sort });

r.get('/admin/packs', wrap(async (req, res) => {
  res.json({ packs: (await CoinPack.findAll({ order: [['sort', 'ASC'], ['pricePaise', 'ASC']] })).map(packOut) });
}));
r.post('/admin/packs', wrap(async (req, res) => {
  const b = packIn.parse(req.body);
  const p = await CoinPack.create({ coins: b.coins, bonusCoins: b.bonusCoins, pricePaise: Math.round(b.priceInr * 100), tag: b.tag || null, active: b.active, sort: b.sort });
  res.json({ pack: packOut(p) });
}));
// a changed price applies to new orders only: an order already made keeps the price and coins it was made with
r.put('/admin/packs/:id', wrap(async (req, res) => {
  const p = await CoinPack.findByPk(pid.parse(req.params.id));
  if (!p) throw notFound('pack_not_found', 'Pack not found');
  const b = packIn.partial().strict().parse(req.body);
  const patch = { ...b };
  if (b.priceInr !== undefined) { patch.pricePaise = Math.round(b.priceInr * 100); delete patch.priceInr; }
  await p.update(patch);
  res.json({ pack: packOut(p) });
}));

r.get('/admin/payments', wrap(async (req, res) => {
  const rows = await Payment.findAll({ order: [['id', 'DESC']], limit: 200 });
  const users = await User.findAll({ where: { id: [...new Set(rows.map((x) => x.userId))] }, attributes: ['id', 'publicId', 'name'] });
  const byId = new Map(users.map((u) => [String(u.id), u]));
  res.json({
    payments: rows.map((x) => ({
      orderId: x.orderId, paymentId: x.paymentId, status: x.status, amountInr: x.amountPaise / 100, coins: x.coins, provider: x.provider, paidVia: x.paidVia,
      user: byId.get(String(x.userId)) ? { id: Number(byId.get(String(x.userId)).publicId), name: byId.get(String(x.userId)).name } : null, createdAt: x.createdAt, paidAt: x.paidAt,
    })),
  });
}));

// ---------------------------------------------------------------- gifts (picture + animation of any kind, uploaded here)
const giftUpload = require('multer')({ storage: require('multer').memoryStorage(), limits: { fileSize: 20 * 1024 * 1024, files: 2 } })
  .fields([{ name: 'thumb', maxCount: 1 }, { name: 'anim', maxCount: 1 }]);
const giftIn = z.object({
  name: z.string().trim().min(1).max(40),
  emoji: z.string().trim().max(16).nullable().optional(),
  price: z.coerce.number().int().min(1).max(1_000_000),
  category: z.string().trim().min(1).max(30).default('Popular'),
  sort: z.coerce.number().int().min(0).max(10000).default(0),
  active: z.preprocess((v) => (v === 'true' ? true : v === 'false' ? false : v), z.boolean()).default(true),
}).strict();
const { giftView } = require('./chat');

/** Saves the uploaded picture / animation of a gift (checked by their first bytes, never by the name the browser sends). */
function giftFiles(req, g) {
  const patch = {};
  const thumb = req.files && req.files.thumb && req.files.thumb[0];
  const anim = req.files && req.files.anim && req.files.anim[0];
  if (thumb) {
    const t = files.sniff(thumb.buffer);
    if (!t || t.kind !== 'photo') throw bad('bad_thumb', 'The picture must be PNG, JPG or WEBP');
    if (thumb.size > 2 * 1024 * 1024) throw bad('thumb_too_large', 'The picture can be at most 2 MB');
    patch.thumbPath = files.save(`gifts/thumb_${g.id}_${Date.now()}.${t.ext}`, thumb.buffer);
  }
  if (anim) {
    const a = files.sniffAnimation(anim.buffer, anim.originalname);
    if (!a) throw bad('bad_animation', 'The animation must be MP4, SVGA, Lottie JSON, animated WEBP or GIF');
    patch.animPath = files.save(`gifts/anim_${g.id}_${Date.now()}.${a.ext}`, anim.buffer);
    patch.animType = a.type;
  }
  return patch;
}

r.get('/admin/gifts', wrap(async (req, res) => {
  res.json({ gifts: (await Gift.findAll({ order: [['sort', 'ASC'], ['price', 'ASC']] })).map((g) => ({ ...giftView(g), sort: g.sort, active: g.active })) });
}));
r.post('/admin/gifts', giftUpload, wrap(async (req, res) => {
  const b = giftIn.parse(req.body);
  const g = await Gift.create(b);
  await g.update(giftFiles(req, g));
  res.json({ gift: { ...giftView(g), sort: g.sort, active: g.active } });
}));
// a new price applies to gifts sent from now on; old files are removed when replaced
r.put('/admin/gifts/:id', giftUpload, wrap(async (req, res) => {
  const g = await Gift.findByPk(pid.parse(req.params.id));
  if (!g) throw notFound('gift_not_found', 'Gift not found');
  const b = giftIn.partial().strict().parse(req.body);
  const old = { thumb: g.thumbPath, anim: g.animPath };
  const patch = { ...b, ...giftFiles(req, g) };
  await g.update(patch);
  if (patch.thumbPath) files.remove(old.thumb);
  if (patch.animPath) files.remove(old.anim);
  res.json({ gift: { ...giftView(g), sort: g.sort, active: g.active } });
}));

module.exports = r;
