const express = require('express');
const { z } = require('zod');
const { Op } = require('sequelize');
const rateLimit = require('express-rate-limit');
const { User, Block, Report } = require('../models');
const settings = require('../services/settings');
const { requireAuth } = require('../middleware/auth');
const { bad, notFound, wrap, rateLimitHandler } = require('../utils/errors');
const { publicUser } = require('./me');

const r = express.Router();

// The app only knows public ids (the 8-digit number), never our database ids.
async function byPublicId(publicId) {
  const u = await User.findOne({ where: { publicId, status: { [Op.ne]: 'deleted' } } });
  if (!u) throw notFound('user_not_found', 'User not found');
  return u;
}
const idParam = z.coerce.number().int().positive();

r.get('/blocks', requireAuth, wrap(async (req, res) => {
  const rows = await Block.findAll({ where: { blockerId: req.user.id }, order: [['createdAt', 'DESC']] });
  const users = await User.findAll({ where: { id: rows.map((b) => b.blockedId) } });
  const byId = new Map(users.map((u) => [String(u.id), u]));
  const list = [];
  for (const b of rows) {
    const u = byId.get(String(b.blockedId));
    if (u) list.push({ ...(await publicUser(u)), blockedAt: b.createdAt });
  }
  res.json({ blocks: list });
}));

r.post('/blocks/:id', requireAuth, wrap(async (req, res) => {
  const target = await byPublicId(idParam.parse(req.params.id));
  if (target.id === req.user.id) throw bad('self', 'You cannot block yourself');
  await Block.findOrCreate({ where: { blockerId: req.user.id, blockedId: target.id } });
  res.json({ ok: true });
}));

r.delete('/blocks/:id', requireAuth, wrap(async (req, res) => {
  const target = await byPublicId(idParam.parse(req.params.id));
  await Block.destroy({ where: { blockerId: req.user.id, blockedId: target.id } });
  res.json({ ok: true });
}));

const reportLimit = rateLimit({ windowMs: 60 * 60_000, limit: 20, keyGenerator: (req) => String(req.user.id), standardHeaders: true, legacyHeaders: false, handler: rateLimitHandler });

r.post('/reports', requireAuth, reportLimit, wrap(async (req, res) => {
  const reasons = await settings.get('report_reasons');
  const body = z.object({
    targetId: z.coerce.number().int().positive(),
    reason: z.string(),
    detail: z.string().trim().max(500).optional(),
  }).strict().parse(req.body);
  if (!reasons.includes(body.reason)) throw bad('invalid_reason', 'Pick a reason from the list');
  const target = await byPublicId(body.targetId);
  if (target.id === req.user.id) throw bad('self', 'You cannot report yourself');
  // one open report per pair per day is enough; repeats are ignored quietly
  const recent = await Report.findOne({ where: { reporterId: req.user.id, targetId: target.id, createdAt: { [Op.gt]: new Date(Date.now() - 86_400_000) } } });
  if (!recent) await Report.create({ reporterId: req.user.id, targetId: target.id, reason: body.reason, detail: body.detail || null });
  res.json({ ok: true });
}));

module.exports = r;
