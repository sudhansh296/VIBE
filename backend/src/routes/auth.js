const express = require('express');
const rateLimit = require('express-rate-limit');
const { z } = require('zod');
const auth = require('../services/auth');
const { normalizePhone } = require('../utils/phone');
const { bad, wrap, rateLimitHandler } = require('../utils/errors');
const cfg = require('../config');
const { publicUser } = require('./me');
const { requireAuth } = require('../middleware/auth');

const r = express.Router();

// login endpoints are the favourite target of bots: keep them tight
const loginLimit = rateLimit({ windowMs: 15 * 60_000, limit: cfg.loginRateLimit, standardHeaders: true, legacyHeaders: false, handler: rateLimitHandler });

async function finishLogin(req, res, identity) {
  const { user, isNew } = await auth.findOrCreateUser(identity);
  const tokens = await auth.issueTokens(user, req.headers['user-agent']);
  res.json({ ...tokens, isNewUser: isNew, user: await publicUser(user, true) });
}

/** Real login: the app sends the Firebase ID token it got after phone OTP or Google sign-in. */
r.post('/firebase', loginLimit, wrap(async (req, res) => {
  const { idToken } = z.object({ idToken: z.string().min(20).max(5000) }).parse(req.body);
  const id = await auth.verifyFirebaseToken(idToken);
  // Firebase already verified this number (E.164, e.g. +919876543210), so it is stored as given
  const phone = id.phone && /^\+\d{8,15}$/.test(id.phone) ? id.phone : null;
  await finishLogin(req, res, { firebaseUid: id.firebaseUid, phone });
}));

/** Local testing only (DEV_AUTH=true and not production): any phone, code 123456. */
r.post('/dev/request-otp', loginLimit, wrap(async (req, res) => {
  const phone = normalizePhone(z.object({ phone: z.string() }).parse(req.body).phone);
  if (!phone) throw bad('invalid_phone', 'Enter a valid 10-digit mobile number');
  auth.devRequestOtp(phone);
  res.json({ sent: true, phone });
}));

r.post('/dev/verify-otp', loginLimit, wrap(async (req, res) => {
  const body = z.object({ phone: z.string(), code: z.string().regex(/^\d{6}$/, 'Enter the 6-digit code') }).parse(req.body);
  const phone = normalizePhone(body.phone);
  if (!phone) throw bad('invalid_phone', 'Enter a valid 10-digit mobile number');
  auth.devCheckOtp(phone, body.code);
  await finishLogin(req, res, { phone });
}));

/** Swap a refresh token for a new pair (the old refresh token stops working). */
r.post('/refresh', loginLimit, wrap(async (req, res) => {
  const { refreshToken } = z.object({ refreshToken: z.string().min(20).max(200) }).parse(req.body);
  const { user, accessToken, refreshToken: next } = await auth.rotateRefresh(refreshToken, req.headers['user-agent']);
  res.json({ accessToken, refreshToken: next, user: await publicUser(user, true) });
}));

/** Ends every other session of this account (other phones log out at once); this phone stays logged in. */
r.post('/logout-others', requireAuth, wrap(async (req, res) => {
  const n = await auth.revokeOthers(req.user.id, req.sessionId);
  res.json({ ok: true, loggedOut: n });
}));

r.post('/logout', wrap(async (req, res) => {
  const { refreshToken } = z.object({ refreshToken: z.string().max(200) }).parse(req.body);
  await auth.revokeRefresh(refreshToken);
  res.json({ ok: true });
}));

module.exports = r;
