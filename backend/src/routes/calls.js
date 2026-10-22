const express = require('express');
const crypto = require('crypto');
const { z } = require('zod');
const { Op } = require('sequelize');
const { sequelize, User, HostProfile, Call } = require('../models');
const { requireAuth } = require('../middleware/auth');
const { bad, forbidden, notFound, conflict, wrap } = require('../utils/errors');

const r = express.Router();

/**
 * Where the phones find each other for the video: STUN (public address) and, when set, our TURN server (relay for networks
 * that block direct connections). TURN passwords are short-lived (coturn "use-auth-secret"): nothing secret is in the app.
 */
r.get('/calls/ice', requireAuth, wrap(async (req, res) => {
  const servers = [{ urls: (process.env.STUN_URLS || 'stun:stun.l.google.com:19302,stun:stun1.l.google.com:19302').split(',') }];
  if (process.env.TURN_URLS && process.env.TURN_SECRET) {
    const username = `${Math.floor(Date.now() / 1000) + 6 * 3600}:${req.user.publicId}`;   // valid for 6 hours
    const credential = crypto.createHmac('sha1', process.env.TURN_SECRET).update(username).digest('base64');
    servers.push({ urls: process.env.TURN_URLS.split(','), username, credential });
  }
  res.json({ iceServers: servers });
}));

/** Call history (Messages > Calls): the other person, how it went, how long, coins (user) or earnings (host). */
r.get('/calls', requireAuth, wrap(async (req, res) => {
  const me = req.user;
  const page = z.coerce.number().int().min(1).max(500).default(1).parse(req.query.page);
  const PAGE = 30;
  const mine = me.role === 'host' ? 'hostId' : 'userId';
  const rows = await Call.findAll({ where: { [mine]: me.id, status: 'ended' }, order: [['createdAt', 'DESC']], limit: PAGE + 1, offset: (page - 1) * PAGE });
  const peerIds = rows.slice(0, PAGE).map((c) => (me.role === 'host' ? c.userId : c.hostId));
  const users = await User.findAll({ where: { id: peerIds } });
  const { cards } = require('./social');
  const byId = new Map((await cards(users, me)).map((c) => [c.id, c]));
  const userById = new Map(users.map((u) => [String(u.id), u]));
  const items = rows.slice(0, PAGE).map((c) => {
    const u = userById.get(String(me.role === 'host' ? c.userId : c.hostId));
    return {
      id: c.id, peer: u ? byId.get(Number(u.publicId)) : null, kind: c.kind, outgoing: me.role !== 'host',
      result: c.seconds > 0 ? 'answered' : (c.endReason === 'declined' ? 'declined' : c.endReason === 'cancelled' ? 'cancelled' : 'missed'),
      seconds: c.seconds, coins: me.role === 'host' ? undefined : c.coinsCharged, earned: me.role === 'host' ? c.hostEarned : undefined,
      rate: c.rate, at: c.createdAt, rated: c.rating != null,
    };
  });
  res.json({ items, nextPage: rows.length > PAGE ? page + 1 : null });
}));

/** The user rates a finished call once (1-5 stars and a few tags). The host's average shows on their profile. */
r.post('/calls/:id/rate', requireAuth, wrap(async (req, res) => {
  const body = z.object({ stars: z.number().int().min(1).max(5), tags: z.array(z.string().trim().min(1).max(30)).max(6).optional() }).strict().parse(req.body);
  const c = await Call.findByPk(String(req.params.id).slice(0, 36));
  if (!c || String(c.userId) !== String(req.user.id)) throw notFound('call_not_found', 'Call not found');
  if (c.status !== 'ended' || c.seconds <= 0) throw bad('not_rateable', 'Only a finished call can be rated');
  if (c.rating != null) throw conflict('already_rated', 'You already rated this call');
  await sequelize.transaction(async (t) => {
    await c.update({ rating: body.stars, ratingTags: body.tags || [] }, { transaction: t });
    await HostProfile.increment({ ratingSum: body.stars, ratingCount: 1 }, { where: { userId: c.hostId }, transaction: t });
  });
  res.json({ ok: true });
}));

module.exports = r;
