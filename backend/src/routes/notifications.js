const express = require('express');
const { Op } = require('sequelize');
const { Notification } = require('../models');
const { requireAuth } = require('../middleware/auth');
const { wrap } = require('../utils/errors');
const { view } = require('../services/notify');

const r = express.Router();

/** The bell list, newest first (40 at a time), and how many are unread. */
r.get('/notifications', requireAuth, wrap(async (req, res) => {
  const before = Number(req.query.before) || null;
  const rows = await Notification.findAll({ where: { userId: req.user.id, ...(before ? { id: { [Op.lt]: before } } : {}) }, order: [['id', 'DESC']], limit: 41 });
  const unread = await Notification.count({ where: { userId: req.user.id, readAt: null } });
  res.json({ items: rows.slice(0, 40).map(view), more: rows.length > 40, unread });
}));

r.get('/notifications/unread', requireAuth, wrap(async (req, res) => {
  res.json({ unread: await Notification.count({ where: { userId: req.user.id, readAt: null } }) });
}));

/** Opening the list marks everything read. */
r.post('/notifications/read', requireAuth, wrap(async (req, res) => {
  const [n] = await Notification.update({ readAt: new Date() }, { where: { userId: req.user.id, readAt: null } });
  res.json({ ok: true, marked: n });
}));

/** Swipe away one line. */
r.delete('/notifications/:id', requireAuth, wrap(async (req, res) => {
  const n = await Notification.destroy({ where: { id: Number(req.params.id) || 0, userId: req.user.id } });
  res.json({ ok: true, removed: n });
}));

module.exports = r;
