const express = require('express');
const { z } = require('zod');
const { UserSetting, HostProfile } = require('../models');
const settings = require('../services/settings');
const { requireAuth, requireRole } = require('../middleware/auth');
const { bad, wrap } = require('../utils/errors');

const r = express.Router();

// ---------------------------------------------------------------- notification switches (any user)
const shapeSettings = (s) => ({ notifyMessages: s.notifyMessages, notifyMissedCalls: s.notifyMissedCalls, notifyOffers: s.notifyOffers });

r.get('/me/settings', requireAuth, wrap(async (req, res) => {
  const [s] = await UserSetting.findOrCreate({ where: { userId: req.user.id } });
  res.json({ settings: shapeSettings(s) });
}));

r.put('/me/settings', requireAuth, wrap(async (req, res) => {
  const body = z.object({
    notifyMessages: z.boolean().optional(),
    notifyMissedCalls: z.boolean().optional(),
    notifyOffers: z.boolean().optional(),
  }).strict().parse(req.body);
  const [s] = await UserSetting.findOrCreate({ where: { userId: req.user.id } });
  await s.update(body);
  res.json({ settings: shapeSettings(s) });
}));

// ---------------------------------------------------------------- host prices (hosts only, inside the admin's range)
async function limits() {
  return {
    callRateMin: Number(await settings.get('host_call_rate_min')),
    callRateMax: Number(await settings.get('host_call_rate_max')),
    messagePriceMin: Number(await settings.get('host_message_price_min')),
    messagePriceMax: Number(await settings.get('host_message_price_max')),
    sharePct: Number(await settings.get('host_share_pct')),
  };
}

async function hostProfile(userId) {
  const [p] = await HostProfile.findOrCreate({
    where: { userId },
    defaults: { callRate: Number(await settings.get('host_call_rate_default')), messagePrice: Number(await settings.get('host_message_price_default')) },
  });
  return p;
}

r.get('/host/rates', requireAuth, requireRole('host'), wrap(async (req, res) => {
  const p = await hostProfile(req.user.id);
  res.json({ callRate: p.callRate, messagePrice: p.messagePrice, limits: await limits() });
}));

r.put('/host/rates', requireAuth, requireRole('host'), wrap(async (req, res) => {
  const body = z.object({
    callRate: z.number().int().optional(),
    messagePrice: z.number().int().optional(),
  }).strict().parse(req.body);
  const l = await limits();
  if (body.callRate !== undefined && (body.callRate < l.callRateMin || body.callRate > l.callRateMax)) {
    throw bad('rate_out_of_range', `Video call price must be between ${l.callRateMin} and ${l.callRateMax} coins per minute`);
  }
  if (body.messagePrice !== undefined && (body.messagePrice < l.messagePriceMin || body.messagePrice > l.messagePriceMax)) {
    throw bad('price_out_of_range', `Message price must be between ${l.messagePriceMin} and ${l.messagePriceMax} coins`);
  }
  const p = await hostProfile(req.user.id);
  await p.update(body);
  res.json({ callRate: p.callRate, messagePrice: p.messagePrice, limits: l });
}));

module.exports = r;
