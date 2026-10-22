const jwt = require('jsonwebtoken');
const cfg = require('../config');
const { User, Session } = require('../models');
const { unauthorized, forbidden, wrap } = require('../utils/errors');

// Reads "Authorization: Bearer <access token>", checks it and loads the user. The user is always re-read from the
// database, so a blocked or deleted account stops working at once even if its token has not expired yet.
const requireAuth = wrap(async (req, res, next) => {
  const h = req.headers.authorization || '';
  const token = h.startsWith('Bearer ') ? h.slice(7) : null;
  if (!token) throw unauthorized();
  let payload;
  try { payload = jwt.verify(token, cfg.jwt.accessSecret, { algorithms: ['HS256'] }); } catch { throw unauthorized('token_expired', 'Session expired'); }
  // the session behind this token must still be open (sign out, "log out of other devices" or a delete end it at once)
  const session = payload.sid ? await Session.findByPk(payload.sid, { attributes: ['id', 'userId', 'revokedAt'] }) : null;
  if (!session || session.revokedAt || String(session.userId) !== String(payload.sub)) throw unauthorized('session_ended', 'Please log in again');
  const user = await User.findByPk(payload.sub);
  if (!user || user.status === 'deleted') throw unauthorized();
  if (user.status === 'blocked') throw forbidden('account_blocked', 'This account has been blocked');
  req.user = user;
  req.sessionId = session.id; // the login (phone) this request comes from
  next();
});

const requireRole = (...roles) => (req, res, next) => (roles.includes(req.user.role) ? next() : next(forbidden()));

module.exports = { requireAuth, requireRole };
