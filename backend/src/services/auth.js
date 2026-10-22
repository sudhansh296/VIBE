const crypto = require('crypto');
const fs = require('fs');
const path = require('path');
const jwt = require('jsonwebtoken');
const cfg = require('../config');
const { sequelize, User, Wallet, Transaction, Session } = require('../models');
const settings = require('./settings');
const { unauthorized, bad, forbidden } = require('../utils/errors');

const sha256 = (s) => crypto.createHash('sha256').update(s).digest('hex');

// ---------------------------------------------------------------- Firebase (real OTP / Google login)
let firebaseAdmin = null;
function firebase() {
  if (firebaseAdmin) return firebaseAdmin;
  const file = cfg.firebaseServiceAccount && path.resolve(__dirname, '..', '..', cfg.firebaseServiceAccount);
  if (!file || !fs.existsSync(file)) return null;
  const { initializeApp, cert } = require('firebase-admin/app');
  const { getAuth } = require('firebase-admin/auth');
  const app = initializeApp({ credential: cert(JSON.parse(fs.readFileSync(file, 'utf8'))) });
  firebaseAdmin = { auth: () => getAuth(app) };
  return firebaseAdmin;
}
const firebaseReady = () => !!firebase();

// The app logs in with Firebase (phone OTP or Google) and sends us the Firebase ID token. We verify it with Google,
// then issue our own tokens. The app never decides who a user is.
async function verifyFirebaseToken(idToken) {
  const admin = firebase();
  if (!admin) throw new (require('../utils/errors').ApiError)(503, 'firebase_not_configured', 'Login is not set up on the server yet');
  try {
    const d = await admin.auth().verifyIdToken(idToken, true);
    return { firebaseUid: d.uid, phone: d.phone_number || null, email: d.email || null };
  } catch { throw unauthorized('invalid_firebase_token', 'Login failed, try again'); }
}

// ---------------------------------------------------------------- DEV login (local testing only, off in production)
const devOtps = new Map(); // phone -> { code, expires, tries }
function devRequestOtp(phone) {
  if (!cfg.devAuth) throw forbidden('dev_auth_off', 'Not available');
  devOtps.set(phone, { code: '123456', expires: Date.now() + 5 * 60_000, tries: 0 });
  console.log(`[dev-otp] ${phone} -> 123456`);
}
function devCheckOtp(phone, code) {
  if (!cfg.devAuth) throw forbidden('dev_auth_off', 'Not available');
  const e = devOtps.get(phone);
  if (!e || e.expires < Date.now()) throw bad('otp_expired', 'The code has expired, request a new one');
  e.tries += 1;
  if (e.tries > 5) { devOtps.delete(phone); throw bad('otp_locked', 'Too many wrong tries, request a new code'); }
  if (e.code !== String(code)) throw bad('otp_wrong', 'That code is not correct');
  devOtps.delete(phone);
}

// ---------------------------------------------------------------- users
async function newPublicId() {
  for (let i = 0; i < 10; i++) {
    const id = 10_000_000 + crypto.randomInt(0, 90_000_000); // 8 digits
    if (!(await User.findOne({ where: { publicId: id }, attributes: ['id'] }))) return id;
  }
  throw new Error('could not allocate a public id');
}

/** Finds the account for a verified identity, or creates it (with its wallet and the signup bonus). */
async function findOrCreateUser({ firebaseUid, phone }) {
  const where = firebaseUid ? { firebaseUid } : { phone };
  let user = await User.findOne({ where });
  if (!user && firebaseUid && phone) user = await User.findOne({ where: { phone } });
  if (user) {
    if (user.status === 'deleted') throw forbidden('account_deleted', 'This account was deleted');
    if (user.status === 'blocked') throw forbidden('account_blocked', 'This account has been blocked');
    if (firebaseUid && !user.firebaseUid) await user.update({ firebaseUid });
    await user.update({ lastSeenAt: new Date() });
    return { user, isNew: false };
  }
  const publicId = await newPublicId();
  const bonus = Number(await settings.get('signup_bonus_coins')) || 0;
  const created = await sequelize.transaction(async (t) => {
    const u = await User.create({ publicId, phone: phone || null, firebaseUid: firebaseUid || null, lastSeenAt: new Date() }, { transaction: t });
    await Wallet.create({ userId: u.id, coins: bonus }, { transaction: t });
    if (bonus > 0) await Transaction.create({ userId: u.id, kind: 'signup_bonus', coins: bonus, balanceAfter: bonus }, { transaction: t });
    return u;
  });
  return { user: created, isNew: true };
}

// ---------------------------------------------------------------- our own tokens
function signAccess(user, sessionId) {
  return jwt.sign({ sub: String(user.id), sid: String(sessionId), role: user.role }, cfg.jwt.accessSecret, { algorithm: 'HS256', expiresIn: cfg.jwt.accessTtl });
}

async function issueTokens(user, userAgent) {
  const refresh = crypto.randomBytes(48).toString('base64url');
  const session = await Session.create({
    userId: user.id,
    tokenHash: sha256(refresh),
    expiresAt: new Date(Date.now() + cfg.jwt.refreshDays * 86_400_000),
    userAgent: (userAgent || '').slice(0, 200),
  });
  return { accessToken: signAccess(user, session.id), refreshToken: refresh };
}

// Refresh tokens are single use: every refresh returns a new one and revokes the old one. If an old token is shown again
// (it was stolen), every session of that user is revoked.
async function rotateRefresh(refreshToken, userAgent) {
  const row = await Session.findOne({ where: { tokenHash: sha256(refreshToken || '') } });
  if (!row) throw unauthorized('invalid_refresh', 'Please log in again');
  if (row.revokedAt) {
    // an old token after a normal logout just means "log in again"; only a token that was already swapped is theft
    if (row.revokeReason !== 'rotated') throw unauthorized('session_ended', 'Please log in again');
    // ...unless it comes back within a few seconds of the swap: that is the phone retrying a refresh whose answer got
    // lost on a bad network. The pair made by that swap (never received) is cancelled and a fresh pair is given instead.
    const graceMs = Number(process.env.REFRESH_REUSE_GRACE_MS ?? 20_000);
    if (Date.now() - new Date(row.revokedAt).getTime() <= graceMs) {
      const { Op } = require('sequelize');
      await Session.update({ revokedAt: new Date(), revokeReason: 'replaced' }, {   // 'replaced': using it later = log in again
        where: { userId: row.userId, revokedAt: null, createdAt: { [Op.gte]: new Date(new Date(row.revokedAt).getTime() - 1000) } },
      });
      const user = await User.findByPk(row.userId);
      if (!user || user.status !== 'active') throw unauthorized();
      return { user, ...(await issueTokens(user, userAgent)) };
    }
    await Session.update({ revokedAt: new Date(), revokeReason: 'theft' }, { where: { userId: row.userId, revokedAt: null } });
    throw unauthorized('refresh_reused', 'Please log in again');
  }
  if (row.expiresAt < new Date()) throw unauthorized('refresh_expired', 'Please log in again');
  const user = await User.findByPk(row.userId);
  if (!user || user.status !== 'active') throw unauthorized();
  await row.update({ revokedAt: new Date(), revokeReason: 'rotated' });
  return { user, ...(await issueTokens(user, userAgent)) };
}

async function revokeRefresh(refreshToken) {
  const row = await Session.findOne({ where: { tokenHash: sha256(refreshToken || ''), revokedAt: null } });
  await Session.update({ revokedAt: new Date(), revokeReason: 'logout' }, { where: { tokenHash: sha256(refreshToken || ''), revokedAt: null } });
  if (row) require('../realtime').kickSession(row.id);
}

/** "Log out of other devices": ends every session of the user except the one this request comes from. */
async function revokeOthers(userId, currentSessionId) {
  const { Op } = require('sequelize');
  const open = await Session.findAll({ where: { userId, revokedAt: null, id: { [Op.ne]: currentSessionId } }, attributes: ['id'] });
  const [n] = await Session.update({ revokedAt: new Date(), revokeReason: 'others' }, { where: { userId, revokedAt: null, id: { [Op.ne]: currentSessionId } } });
  open.forEach((x) => require('../realtime').kickSession(x.id));
  return n;
}

async function revokeAll(userId) {
  await Session.update({ revokedAt: new Date(), revokeReason: 'all' }, { where: { userId, revokedAt: null } });
  require('../realtime').kickUser(userId);
}

module.exports = {
  firebaseReady, verifyFirebaseToken, devRequestOtp, devCheckOtp,
  findOrCreateUser, issueTokens, rotateRefresh, revokeRefresh, revokeAll, revokeOthers,
};
