const express = require('express');
const { z } = require('zod');
const { User, SupportTicket } = require('../models');
const { requireAuth, requireRole } = require('../middleware/auth');
const { notify } = require('../services/notify');
const { bad, conflict, notFound, wrap } = require('../utils/errors');

/** Help & support requests: a person writes to VIBE; the admin answers; the answer shows in the app and as a notification. */
const r = express.Router();
const view = (t) => ({ id: Number(t.id), topic: t.topic, text: t.text, status: t.status, reply: t.reply || null, at: t.createdAt, repliedAt: t.repliedAt || null, newReply: t.status === 'answered' && !t.replySeen });

r.get('/support', requireAuth, wrap(async (req, res) => {
  const rows = await SupportTicket.findAll({ where: { userId: req.user.id }, order: [['id', 'DESC']], limit: 50 });
  res.json({ tickets: rows.map(view) });
}));

r.post('/support', requireAuth, wrap(async (req, res) => {
  const body = z.object({ topic: z.string().trim().min(2).max(40), text: z.string().trim().min(5, 'Tell us a little more').max(1000) }).strict().parse(req.body);
  if (await SupportTicket.count({ where: { userId: req.user.id, status: 'open' } }) >= 5) throw conflict('too_many_open', 'You already have 5 open requests. We will answer them soon.');
  const t = await SupportTicket.create({ userId: req.user.id, topic: body.topic, text: body.text });
  res.json({ ticket: view(t) });
}));

/** Opening an answered request marks the answer seen. */
r.post('/support/:id/seen', requireAuth, wrap(async (req, res) => {
  const t = await SupportTicket.findOne({ where: { id: Number(req.params.id) || 0, userId: req.user.id } });
  if (!t) throw notFound('ticket_not_found', 'Not found');
  if (t.status === 'answered' && !t.replySeen) await t.update({ replySeen: true });
  res.json({ ticket: view(t) });
}));

// ---------------------------------------------------------------- admin
r.use('/admin/support', requireAuth, requireRole('admin'));
r.get('/admin/support', wrap(async (req, res) => {
  const status = req.query.status === 'answered' ? 'answered' : 'open';
  const rows = await SupportTicket.findAll({ where: { status }, order: [['id', status === 'open' ? 'ASC' : 'DESC']], limit: 200 });
  const users = rows.length ? await User.findAll({ where: { id: rows.map((x) => x.userId) }, attributes: ['id', 'publicId', 'name', 'role'] }) : [];
  const byId = new Map(users.map((u) => [String(u.id), u]));
  res.json({ tickets: rows.map((t) => ({ ...view(t), from: byId.get(String(t.userId)) ? { id: Number(byId.get(String(t.userId)).publicId), name: byId.get(String(t.userId)).name, role: byId.get(String(t.userId)).role } : null })) });
}));
r.post('/admin/support/:id/reply', wrap(async (req, res) => {
  const { text } = z.object({ text: z.string().trim().min(2).max(1000) }).strict().parse(req.body);
  const t = await SupportTicket.findByPk(req.params.id);
  if (!t) throw notFound('ticket_not_found', 'Not found');
  if (t.status === 'answered') throw bad('already_answered', 'Already answered');
  await t.update({ status: 'answered', reply: text, repliedAt: new Date(), replySeen: false });
  await notify(t.userId, 'support', 'VIBE support replied', text.slice(0, 140), { ticketId: Number(t.id) });
  res.json({ ticket: view(t) });
}));

module.exports = r;
