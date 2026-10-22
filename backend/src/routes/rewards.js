const express = require('express');
const crypto = require('crypto');
const { z } = require('zod');
const { sequelize, User, Wallet, Transaction, Payment } = require('../models');
const settings = require('../services/settings');
const levels = require('../services/levels');
const { requireAuth } = require('../middleware/auth');
const { bad, conflict, notFound, wrap } = require('../utils/errors');

/**
 * Free rewards: daily check-in (7-day round, missing a day restarts), levels (XP -> level badge + coins per level) and
 * invites (both get coins once the invited friend buys coins for the first time). All numbers are admin settings.
 */
const r = express.Router();

// the day changes at midnight India time
const dayIST = (d = new Date()) => new Date(d.getTime() + 330 * 60_000).toISOString().slice(0, 10);
const yesterdayIST = () => dayIST(new Date(Date.now() - 86_400_000));
async function checkinRewards() { const out = []; for (let i = 1; i <= 7; i++) out.push(Number(await settings.get(`checkin_day${i}`))); return out; }

/** Today's state: which day of the round is next and whether today is already collected. */
function checkinState(u) {
  const today = dayIST();
  if (u.checkinLast === today) return { done: u.checkinStreak, collectedToday: true };
  const goesOn = u.checkinLast === yesterdayIST() && u.checkinStreak < 7;
  return { done: goesOn ? u.checkinStreak : 0, collectedToday: false };
}

r.get('/checkin', requireAuth, wrap(async (req, res) => {
  const s = checkinState(req.user);
  res.json({ rewards: await checkinRewards(), done: s.done, collectedToday: s.collectedToday, xp: await levels.xpFor('xp_checkin') });
}));

r.post('/checkin', requireAuth, wrap(async (req, res) => {
  const rewards = await checkinRewards();
  const out = await sequelize.transaction(async (t) => {
    const u = await User.findByPk(req.user.id, { transaction: t, lock: t.LOCK.UPDATE });
    const s = checkinState(u);
    if (s.collectedToday) throw conflict('already_collected', 'Already collected today. Come back tomorrow!');
    const day = s.done + 1;
    const coins = rewards[day - 1];
    await u.update({ checkinStreak: day, checkinLast: dayIST() }, { transaction: t });
    const w = await Wallet.findByPk(u.id, { transaction: t, lock: t.LOCK.UPDATE });
    if (coins > 0) {
      await w.update({ coins: Number(w.coins) + coins }, { transaction: t });
      await Transaction.create({ userId: u.id, wallet: 'coins', kind: 'checkin', coins, balanceAfter: Number(w.coins), ref: { day } }, { transaction: t });
    }
    return { day, coins, balance: Number(w.coins) };
  });
  await levels.addXp(req.user.id, await levels.xpFor('xp_checkin'));
  res.json(out);
}));

// ---------------------------------------------------------------- levels
r.get('/levels', requireAuth, wrap(async (req, res) => {
  const u = await User.findByPk(req.user.id);
  const list = await levels.rules();
  res.json({
    ...(await levels.progress(u)),
    rules: list.map((x) => ({ level: x.level, xp: x.xp, rewardCoins: x.rewardCoins })),
    earn: {
      callMinute: await levels.xpFor('xp_call_minute'), message: await levels.xpFor('xp_message'),
      giftPer10Coins: await levels.xpFor('xp_gift_per_10_coins'), checkin: await levels.xpFor('xp_checkin'),
    },
  });
}));

// ---------------------------------------------------------------- invite
async function myCode(u) {
  if (u.referralCode) return u.referralCode;
  for (let i = 0; i < 5; i++) {
    const code = crypto.randomBytes(4).toString('base64').replace(/[^A-Z0-9]/gi, '').toUpperCase().slice(0, 6).padEnd(6, 'X');
    try { await u.update({ referralCode: code }); return code; } catch { /* taken: try another */ }
  }
  throw conflict('try_again', 'Please try again');
}

/** The shareable invite link: opens the app with the code filled in (or the Play Store, which passes the code on). */
async function inviteLink(code) {
  return `${String(await settings.get('invite_link_base')).replace(/\/+$/, '')}/i/${code}`;
}

const esc = (s) => String(s).replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));

// The page behind an invite link (no login). It tries to open the app with the code; without the app it goes to the
// Play Store with the code as the install referrer, so the app still gets it after installing.
r.get('/i/:code', wrap(async (req, res) => {
  const code = String(req.params.code).toUpperCase();
  if (!/^[A-Z0-9]{4,12}$/.test(code)) return res.status(404).type('text/plain').send('Invite link not found');
  const store = String(await settings.get('play_store_url'));
  const storeUrl = `${store}${store.includes('?') ? '&' : '?'}referrer=${encodeURIComponent(`invite=${code}`)}`;
  const open = `intent://invite/${code}#Intent;scheme=vibecallme;package=com.vibe.callme;S.browser_fallback_url=${encodeURIComponent(storeUrl)};end`;
  const nonce = crypto.randomBytes(12).toString('base64');
  res.set('Content-Security-Policy', `default-src 'none'; style-src 'unsafe-inline'; script-src 'nonce-${nonce}'`);
  res.set('Cache-Control', 'no-store');
  res.type('html').send(`<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Join me on VIBE</title><style>
body{margin:0;font-family:system-ui,sans-serif;background:#fff0f6;color:#2a1030;display:flex;min-height:100vh;align-items:center;justify-content:center;text-align:center}
.c{padding:32px 24px;max-width:360px}.l{font-size:34px;font-weight:800;color:#ff3d8b;letter-spacing:1px}
.code{margin:18px auto;padding:12px 20px;border:2px dashed #ff3d8b;border-radius:14px;font-size:24px;font-weight:700;letter-spacing:4px;display:inline-block}
a.b{display:block;margin-top:14px;padding:15px;border-radius:28px;background:linear-gradient(90deg,#ff3d8b,#ff7aa8);color:#fff;font-weight:700;text-decoration:none}
a.s{display:block;margin-top:14px;color:#ff3d8b;font-weight:600}p{color:#7a5a75;font-size:14px}</style></head>
<body><div class="c"><div class="l">VIBE</div><h2>Your friend invited you</h2>
<p>Join with this code. After your first coin purchase you both get free coins.</p>
<div class="code">${esc(code)}</div>
<a class="b" href="${esc(open)}">Open VIBE</a><a class="s" href="${esc(storeUrl)}">Get the app</a>
<p>No app yet? Install it, and the code is added for you.</p></div>
<script nonce="${nonce}">if (/android/i.test(navigator.userAgent)) location.href = ${JSON.stringify(open)};</script></body></html>`);
}));

r.get('/invite', requireAuth, wrap(async (req, res) => {
  const u = await User.findByPk(req.user.id);
  const [invited, rewarded] = await Promise.all([
    User.count({ where: { referredBy: u.id } }), User.count({ where: { referredBy: u.id, referralRewarded: true } }),
  ]);
  const paid = await Payment.count({ where: { userId: u.id, status: 'paid' } });
  res.json({
    code: await myCode(u), rewardCoins: Number(await settings.get('invite_reward_coins')), invited, rewarded,
    link: await inviteLink(await myCode(u)),
    // a friend's code can be entered once, and only before the first purchase
    canEnterCode: !u.referredBy && paid === 0 && u.role !== 'host', enteredCode: !!u.referredBy,
  });
}));

r.post('/invite/apply', requireAuth, wrap(async (req, res) => {
  const { code } = z.object({ code: z.string().trim().min(4).max(12) }).strict().parse(req.body);
  const u = await User.findByPk(req.user.id);
  if (u.referredBy) throw conflict('already_used', 'You have already entered a code');
  if (u.role === 'host') throw bad('hosts_no_invite', 'Invite codes are for users');
  if (await Payment.count({ where: { userId: u.id, status: 'paid' } })) throw bad('too_late', 'Codes can only be entered before your first purchase');
  const from = await User.findOne({ where: { referralCode: code.toUpperCase() } });
  if (!from || from.status !== 'active') throw notFound('code_not_found', 'This code does not exist');
  if (String(from.id) === String(u.id)) throw bad('own_code', 'You cannot use your own code');
  await u.update({ referredBy: from.id });
  res.json({ ok: true, rewardCoins: Number(await settings.get('invite_reward_coins')) });
}));

/** Called inside the payment's transaction: the friend's first purchase pays both people once. */
async function rewardInvite(t, userId) {
  const u = await User.findByPk(userId, { transaction: t, lock: t.LOCK.UPDATE });
  if (!u || !u.referredBy || u.referralRewarded) return null;
  const coins = Number(await settings.get('invite_reward_coins'));
  await u.update({ referralRewarded: true }, { transaction: t });
  if (coins <= 0) return null;
  const pay = async (id, ref) => {
    const w = await Wallet.findByPk(id, { transaction: t, lock: t.LOCK.UPDATE });
    if (!w) return;
    await w.update({ coins: Number(w.coins) + coins }, { transaction: t });
    await Transaction.create({ userId: id, wallet: 'coins', kind: 'invite_reward', coins, balanceAfter: Number(w.coins), ref }, { transaction: t });
  };
  await pay(u.id, { inviterId: Number(u.referredBy) });
  await pay(u.referredBy, { friendId: Number(u.id) });
  return { friend: u, inviterId: u.referredBy, coins };
}

module.exports = r;
module.exports.rewardInvite = rewardInvite;
module.exports.dayIST = dayIST;
