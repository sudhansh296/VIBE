const express = require('express');
const { z } = require('zod');
const rateLimit = require('express-rate-limit');
const cfg = require('../config');
const { CoinPack, Payment, Wallet, Transaction, User, Media } = require('../models');
const settings = require('../services/settings');
const razorpay = require('../services/razorpay');
const wallet = require('../services/wallet');
const { requireAuth, requireRole } = require('../middleware/auth');
const { bad, forbidden, notFound, wrap, rateLimitHandler } = require('../utils/errors');

const r = express.Router();

const packView = (p) => ({ id: p.id, coins: p.coins, bonusCoins: p.bonusCoins, totalCoins: p.coins + p.bonusCoins, priceInr: p.pricePaise / 100, pricePaise: p.pricePaise, tag: p.tag || null });
const paymentView = (p) => ({
  orderId: p.orderId, paymentId: p.paymentId || null, status: p.status, coins: p.coins, amountInr: p.amountPaise / 100,
  createdAt: p.createdAt, paidAt: p.paidAt || null, failReason: p.status === 'failed' ? p.failReason : null,
});

// ---------------------------------------------------------------- packs and balance
r.get('/wallet/packs', requireAuth, wrap(async (req, res) => {
  const packs = await CoinPack.findAll({ where: { active: true }, order: [['sort', 'ASC'], ['pricePaise', 'ASC']] });
  res.json({ packs: packs.map(packView), purchaseEnabled: !!(await settings.get('coin_purchase_enabled')) });
}));

r.get('/wallet', requireAuth, wrap(async (req, res) => {
  const w = await Wallet.findByPk(req.user.id);
  res.json({ coins: w ? Number(w.coins) : 0, earnings: req.user.role === 'host' && w ? Number(w.earnings) : undefined });
}));

// what each ledger row means, in words for the history list
async function titled(rows) {
  const ids = new Set();
  rows.forEach((t) => { const x = t.ref || {}; if (x.hostId) ids.add(String(x.hostId)); if (x.buyerId) ids.add(String(x.buyerId)); });
  const users = ids.size ? await User.findAll({ where: { id: [...ids] }, attributes: ['id', 'name'] }) : [];
  const name = new Map(users.map((u) => [String(u.id), u.name || 'someone']));
  const mediaIds = rows.map((t) => t.ref && t.ref.mediaId).filter(Boolean);
  const media = mediaIds.length ? await Media.findAll({ where: { id: mediaIds }, attributes: ['id', 'kind'] }) : [];
  const kindOf = new Map(media.map((m) => [String(m.id), m.kind]));
  return rows.map((t) => {
    const x = t.ref || {};
    const what = kindOf.get(String(x.mediaId)) || 'photo';
    const title = {
      signup_bonus: 'Welcome bonus',
      recharge: 'Bought coins',
      checkin: `Daily check-in · day ${x.day || ''}`,
      level_reward: `Level ${x.level || ''} reward`,
      invite_reward: 'Invite reward',
      withdraw: `Withdrawal request · ${Number(x.beans || 0).toLocaleString('en-IN')} beans`,
      withdraw_returned: `Withdrawal returned · ${Number(x.beans || 0).toLocaleString('en-IN')} beans`,
      vip: `VIP · ${x.planName || 'plan'}${x.days ? ` · ${x.days} days` : ''}${t.coins > 0 ? ' · bonus coins' : ''}`,
      media_unlock: `Unlocked a ${what} · ${name.get(String(x.hostId)) || 'host'}`,
      media_earn: `${name.get(String(x.buyerId)) || 'Someone'} unlocked your ${what}`,
      message_spend: `Message · ${name.get(String(x.hostId)) || 'host'}`,
      message_earn: `Message from ${name.get(String(x.buyerId)) || 'someone'}`,
      call_spend: `Video call · ${name.get(String(x.hostId)) || 'host'}`,
      call_earn: `Video call · ${name.get(String(x.buyerId)) || 'someone'}`,
      call_refund: `Refund · ${name.get(String(x.hostId)) || 'host'} ended the call`,
      call_earn_reversed: `Last minute returned · you ended the call with ${name.get(String(x.buyerId)) || 'someone'}`,
      gift_spend: `Gift · ${x.quantity > 1 ? `${x.quantity}× ` : ''}${x.giftName || 'gift'} to ${name.get(String(x.hostId)) || 'host'}`,
      gift_earn: `${name.get(String(x.buyerId)) || 'Someone'} sent you ${x.quantity > 1 ? `${x.quantity}× ` : 'a '}${x.giftName || 'gift'}`,
    }[t.kind] || t.kind.replace(/_/g, ' ');
    return { id: Number(t.id), kind: t.kind, title, coins: Number(t.coins), balanceAfter: Number(t.balanceAfter), at: t.createdAt, orderId: x.orderId || null };
  });
}

/** Puts the minute-by-minute rows of one call together into one line ("Video call · Riya · 3 min"). */
function mergeCalls(items, rows) {
  const out = [];
  items.forEach((it, i) => {
    const callId = rows[i].ref && rows[i].ref.callId;
    const prev = out[out.length - 1];
    if (callId && prev && prev._callId === callId && prev.kind === it.kind) {
      prev.coins += it.coins; prev._minutes += 1; prev.title = `${prev._base} · ${prev._minutes} min`;
    } else out.push(callId ? { ...it, _callId: callId, _minutes: 1, _base: it.title, title: `${it.title} · 1 min` } : it);
  });
  return out.map(({ _callId, _minutes, _base, ...rest }) => ({ ...rest, callId: _callId || null }));
}

async function history(req, res, walletName) {
  const page = z.coerce.number().int().min(1).max(500).default(1).parse(req.query.page);
  const PAGE = 30;
  const rows = await Transaction.findAll({ where: { userId: req.user.id, wallet: walletName }, order: [['id', 'DESC']], limit: PAGE + 1, offset: (page - 1) * PAGE });
  res.json({ items: mergeCalls(await titled(rows.slice(0, PAGE)), rows.slice(0, PAGE)), nextPage: rows.length > PAGE ? page + 1 : null });
}
r.get('/wallet/history', requireAuth, wrap((req, res) => history(req, res, 'coins')));

// a host's earnings (paying out comes later, Phase 7)
r.get('/host/earnings', requireAuth, requireRole('host'), wrap(async (req, res) => {
  const w = await Wallet.findByPk(req.user.id);
  const page = z.coerce.number().int().min(1).max(500).default(1).parse(req.query.page);
  const PAGE = 30;
  const rows = await Transaction.findAll({ where: { userId: req.user.id, wallet: 'earnings' }, order: [['id', 'DESC']], limit: PAGE + 1, offset: (page - 1) * PAGE });
  res.json({ earnings: w ? Number(w.earnings) : 0, items: mergeCalls(await titled(rows.slice(0, PAGE)), rows.slice(0, PAGE)), nextPage: rows.length > PAGE ? page + 1 : null });
}));

// ---------------------------------------------------------------- buying coins
const orderLimit = rateLimit({ windowMs: 15 * 60_000, limit: 30, standardHeaders: true, legacyHeaders: false, handler: rateLimitHandler });

/** Step 1: the app picks a pack; the server fixes the price and coins and makes a Razorpay order. */
r.post('/wallet/orders', orderLimit, requireAuth, wrap(async (req, res) => {
  const { packId } = z.object({ packId: z.number().int().positive() }).strict().parse(req.body);
  if (!(await settings.get('coin_purchase_enabled'))) throw forbidden('purchases_paused', 'Buying coins is paused for a while. Please try again later.');
  const pack = await CoinPack.findOne({ where: { id: packId, active: true } });
  if (!pack) throw notFound('pack_not_found', 'This pack is no longer available');
  const receipt = `u${req.user.id}_${Date.now()}`;
  let order;
  try { order = await razorpay.createOrder(pack.pricePaise, receipt, { userId: String(req.user.publicId), packId: String(pack.id) }); }
  catch (e) { console.error('[payments]', e.message); throw new (require('../utils/errors').ApiError)(502, 'payment_unavailable', 'Payments are not available right now. Please try again.'); }
  const p = await Payment.create({
    userId: req.user.id, packId: pack.id, coins: pack.coins + pack.bonusCoins, amountPaise: pack.pricePaise, provider: razorpay.provider(), orderId: order.id,
  });
  res.json({
    provider: p.provider, keyId: cfg.razorpay.fake ? null : cfg.razorpay.keyId, orderId: p.orderId, amountPaise: p.amountPaise, currency: 'INR',
    coins: p.coins, name: 'VIBE', description: `${p.coins.toLocaleString('en-IN')} coins`, phone: req.user.phone,
  });
}));

// ---------------------------------------------------------------- VIP
const vipView = (p) => ({ id: p.id, name: p.name, days: p.days, price: p.pricePaise / 100, bonusCoins: p.bonusCoins, tag: p.tag || null });

/** The VIP plans and my VIP state (end date, the call discount now). */
r.get('/vip', requireAuth, wrap(async (req, res) => {
  const { VipPlan } = require('../models');
  const plans = await VipPlan.findAll({ where: { active: true }, order: [['sort', 'ASC'], ['id', 'ASC']] });
  res.json({
    plans: plans.map(vipView), vip: require('../services/vip').isVip(req.user), vipUntil: req.user.vipUntil || null,
    callDiscountPct: Number(await settings.get('vip_call_discount_pct')),
  });
}));

/** Buying VIP, step 1: the server fixes the price, days and bonus and makes the order (then verify like coins). */
r.post('/vip/orders', orderLimit, requireAuth, wrap(async (req, res) => {
  const { planId } = z.object({ planId: z.number().int().positive() }).strict().parse(req.body);
  if (req.user.role === 'host') throw forbidden('hosts_no_vip', 'VIP is for users');
  const { VipPlan } = require('../models');
  const plan = await VipPlan.findOne({ where: { id: planId, active: true } });
  if (!plan) throw notFound('plan_not_found', 'This plan is no longer available');
  let order;
  try { order = await razorpay.createOrder(plan.pricePaise, `v${req.user.id}_${Date.now()}`, { userId: String(req.user.publicId), vipPlanId: String(plan.id) }); }
  catch (e) { console.error('[payments]', e.message); throw new (require('../utils/errors').ApiError)(502, 'payment_unavailable', 'Payments are not available right now. Please try again.'); }
  const p = await Payment.create({
    userId: req.user.id, vipPlanId: plan.id, vipDays: plan.days, coins: plan.bonusCoins, amountPaise: plan.pricePaise, provider: razorpay.provider(), orderId: order.id,
  });
  res.json({
    provider: p.provider, keyId: cfg.razorpay.fake ? null : cfg.razorpay.keyId, orderId: p.orderId, amountPaise: p.amountPaise, currency: 'INR',
    coins: p.coins, name: 'VIBE', description: `VIBE VIP · ${plan.name}`, phone: req.user.phone, vip: true,
  });
}));

/** Step 2: the app sends what Razorpay handed back. Coins are credited only when the signature proves the payment. */
r.post('/wallet/verify', requireAuth, wrap(async (req, res) => {
  const body = z.object({ orderId: z.string().max(60), paymentId: z.string().max(60), signature: z.string().max(200) }).strict().parse(req.body);
  const p = await Payment.findOne({ where: { orderId: body.orderId } });
  if (!p || String(p.userId) !== String(req.user.id)) throw notFound('order_not_found', 'Order not found');
  if (!razorpay.verifyPayment(body.orderId, body.paymentId, body.signature)) throw bad('bad_signature', 'This payment could not be confirmed');
  const out = await wallet.creditPurchase(body.orderId, body.paymentId, 'app');
  res.json({ ok: true, credited: out.credited, coins: out.coins, payment: paymentView(out.payment) });
}));

/** The app reports a cancelled or failed checkout (only marks it; a later webhook can still prove it paid). */
r.post('/wallet/orders/:orderId/failed', requireAuth, wrap(async (req, res) => {
  const reason = z.object({ reason: z.string().max(200).optional() }).strict().parse(req.body || {}).reason;
  const p = await Payment.findOne({ where: { orderId: req.params.orderId } });
  if (!p || String(p.userId) !== String(req.user.id)) throw notFound('order_not_found', 'Order not found');
  await wallet.markFailed(p.orderId, reason || 'Payment cancelled');
  await p.reload();
  res.json({ payment: paymentView(p) });
}));

/** Status of one purchase (pending screen, receipt). */
r.get('/wallet/payments/:orderId', requireAuth, wrap(async (req, res) => {
  const p = await Payment.findOne({ where: { orderId: req.params.orderId } });
  if (!p || String(p.userId) !== String(req.user.id)) throw notFound('order_not_found', 'Order not found');
  res.json({ payment: paymentView(p) });
}));

r.get('/wallet/payments', requireAuth, wrap(async (req, res) => {
  const rows = await Payment.findAll({ where: { userId: req.user.id }, order: [['id', 'DESC']], limit: 50 });
  res.json({ payments: rows.map(paymentView) });
}));

/** DEV ONLY (pretend Razorpay): what Razorpay's checkout would hand the app after paying. */
r.post('/wallet/dev/pay/:orderId', requireAuth, wrap(async (req, res) => {
  if (!cfg.razorpay.fake || !cfg.devAuth) throw notFound('not_found', 'Not found');
  const p = await Payment.findOne({ where: { orderId: req.params.orderId } });
  if (!p || String(p.userId) !== String(req.user.id)) throw notFound('order_not_found', 'Order not found');
  res.json({ orderId: p.orderId, ...razorpay.fakePay(p.orderId) });
}));

// ---------------------------------------------------------------- Razorpay webhook (coins arrive even if the app was closed mid-payment)
// mounted with a raw body parser in app.js: the signature is over the exact bytes Razorpay sent
async function webhook(req, res) {
  const raw = Buffer.isBuffer(req.body) ? req.body.toString('utf8') : '';
  if (!razorpay.verifyWebhook(raw, req.get('X-Razorpay-Signature') || '')) return res.status(400).json({ error: { code: 'bad_signature', message: 'Bad signature' } });
  let ev;
  try { ev = JSON.parse(raw); } catch { return res.status(400).json({ error: { code: 'bad_body', message: 'Bad body' } }); }
  const pay = ev.payload && ev.payload.payment && ev.payload.payment.entity;
  if (pay && pay.order_id) {
    const p = await Payment.findOne({ where: { orderId: pay.order_id } });
    if (p) {
      if ((ev.event === 'payment.captured' || ev.event === 'order.paid') && Number(pay.amount) === p.amountPaise && pay.currency === p.currency) {
        await wallet.creditPurchase(p.orderId, pay.id, 'webhook');
      } else if (ev.event === 'payment.failed') {
        await wallet.markFailed(p.orderId, pay.error_description || 'Payment failed');
      }
    }
  }
  res.json({ ok: true }); // always 200 for a genuine call, so Razorpay does not keep retrying events we do not use
}
r.webhook = wrap(webhook);

module.exports = r;
