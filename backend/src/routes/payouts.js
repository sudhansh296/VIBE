const express = require('express');
const { z } = require('zod');
const { sequelize, User, Wallet, HostProfile, Transaction, Withdrawal } = require('../models');
const settings = require('../services/settings');
const { requireAuth, requireRole } = require('../middleware/auth');
const pii = require('../utils/pii');
const { notify } = require('../services/notify');
const { bad, forbidden, notFound, conflict, wrap } = require('../utils/errors');

/**
 * Host withdrawals. The app shows them in BEANS only: beans = earned coins / coins_per_bean (admin setting). No rupee value
 * anywhere: the owner / admin decide what beans are worth and pay the host's UPI outside the app, then mark it paid here.
 */
const r = express.Router();
const UPI = /^[\w.\-]{2,}@[a-zA-Z][\w]{1,}$/;
const maskUpi = (upi) => { const [n, b] = String(upi).split('@'); return `${n.slice(0, 2)}${'•'.repeat(Math.max(2, n.length - 2))}@${b}`; };
const view = (w) => ({ id: Number(w.id), beans: w.beans, status: w.status, note: w.note || null, at: w.createdAt, decidedAt: w.decidedAt || null });

async function hostOnly(req) {
  if (req.user.role !== 'host' || req.user.hostStatus !== 'approved') throw forbidden('hosts_only', 'Only hosts can withdraw');
}

/** The withdraw page: available beans, the minimum, where it goes, and the requests so far. */
r.get('/host/payout', requireAuth, wrap(async (req, res) => {
  await hostOnly(req);
  const [w, p, perBean, min] = await Promise.all([
    Wallet.findByPk(req.user.id), HostProfile.findOne({ where: { userId: req.user.id } }),
    settings.get('coins_per_bean'), settings.get('withdraw_min_beans'),
  ]);
  const rows = await Withdrawal.findAll({ where: { hostId: req.user.id }, order: [['id', 'DESC']], limit: 50 });
  const upi = p && p.payoutUpiEnc ? pii.decrypt(p.payoutUpiEnc) : null;
  res.json({
    beans: Math.floor(Number(w ? w.earnings : 0) / Number(perBean)), minBeans: Number(min),
    upi: upi ? maskUpi(upi) : null, upiName: p ? p.payoutName || null : null,
    pending: rows.some((x) => x.status === 'pending'), history: rows.map(view),
  });
}));

/** UPI ID + name, saved once (changed any time when nothing is pending). */
r.put('/host/payout', requireAuth, wrap(async (req, res) => {
  await hostOnly(req);
  const body = z.object({ upi: z.string().trim().min(5).max(80), name: z.string().trim().min(3).max(80) }).strict().parse(req.body);
  if (!UPI.test(body.upi)) throw bad('invalid_upi', 'Enter a valid UPI ID (name@bank)');
  const [p] = await HostProfile.findOrCreate({ where: { userId: req.user.id }, defaults: { callRate: 60, messagePrice: 200 } });
  await p.update({ payoutUpiEnc: pii.encrypt(body.upi.toLowerCase()), payoutName: body.name });
  res.json({ upi: maskUpi(body.upi.toLowerCase()), upiName: body.name });
}));

/** Ask for a withdrawal of [beans]: the coins are held at once; one request at a time. */
r.post('/host/withdrawals', requireAuth, wrap(async (req, res) => {
  await hostOnly(req);
  const { beans } = z.object({ beans: z.number().int().positive().max(100000000) }).strict().parse(req.body);
  const [perBean, min] = [Number(await settings.get('coins_per_bean')), Number(await settings.get('withdraw_min_beans'))];
  if (beans < min) throw bad('below_minimum', `The smallest withdrawal is ${min.toLocaleString('en-IN')} beans`);
  const out = await sequelize.transaction(async (t) => {
    const p = await HostProfile.findOne({ where: { userId: req.user.id }, transaction: t });
    if (!p || !p.payoutUpiEnc) throw bad('no_upi', 'Add your UPI ID first');
    if (await Withdrawal.count({ where: { hostId: req.user.id, status: 'pending' }, transaction: t })) throw conflict('pending_exists', 'You already have a request in progress');
    const w = await Wallet.findByPk(req.user.id, { transaction: t, lock: t.LOCK.UPDATE });
    const coins = beans * perBean;
    if (Number(w.earnings) < coins) throw bad('not_enough', 'More than you have');
    await w.update({ earnings: Number(w.earnings) - coins }, { transaction: t });
    const wd = await Withdrawal.create({ hostId: req.user.id, beans, coins, upiEnc: p.payoutUpiEnc, upiName: p.payoutName }, { transaction: t });
    await Transaction.create({ userId: req.user.id, wallet: 'earnings', kind: 'withdraw', coins: -coins, balanceAfter: Number(w.earnings), ref: { withdrawalId: Number(wd.id), beans } }, { transaction: t });
    return { wd, left: Math.floor(Number(w.earnings) / perBean) };
  });
  res.json({ withdrawal: view(out.wd), beans: out.left });
}));

// ---------------------------------------------------------------- admin: the payout queue
r.use('/admin/withdrawals', requireAuth, requireRole('admin'));

r.get('/admin/withdrawals', wrap(async (req, res) => {
  const status = ['pending', 'paid', 'rejected'].includes(req.query.status) ? req.query.status : 'pending';
  const rows = await Withdrawal.findAll({ where: { status }, order: [['id', status === 'pending' ? 'ASC' : 'DESC']], limit: 200 });
  const hosts = rows.length ? await User.findAll({ where: { id: rows.map((x) => x.hostId) }, attributes: ['id', 'publicId', 'name'] }) : [];
  const byId = new Map(hosts.map((u) => [String(u.id), u]));
  res.json({
    withdrawals: rows.map((w) => ({
      ...view(w), coins: w.coins, upi: pii.decrypt(w.upiEnc), upiName: w.upiName,
      host: byId.get(String(w.hostId)) ? { id: Number(byId.get(String(w.hostId)).publicId), name: byId.get(String(w.hostId)).name } : null,
    })),
  });
}));

async function decide(req, res, paid) {
  const { note } = z.object({ note: z.string().trim().max(200).optional() }).strict().parse(req.body || {});
  const wd = await sequelize.transaction(async (t) => {
    const w = await Withdrawal.findByPk(req.params.id, { transaction: t, lock: t.LOCK.UPDATE });
    if (!w) throw notFound('withdrawal_not_found', 'Not found');
    if (w.status !== 'pending') throw conflict('already_decided', 'This request is already done');
    await w.update({ status: paid ? 'paid' : 'rejected', note: note || null, decidedAt: new Date(), decidedBy: req.user.id }, { transaction: t });
    if (!paid) { // rejected: the held coins go back to the host
      const wallet = await Wallet.findByPk(w.hostId, { transaction: t, lock: t.LOCK.UPDATE });
      await wallet.update({ earnings: Number(wallet.earnings) + w.coins }, { transaction: t });
      await Transaction.create({ userId: w.hostId, wallet: 'earnings', kind: 'withdraw_returned', coins: w.coins, balanceAfter: Number(wallet.earnings), ref: { withdrawalId: Number(w.id), beans: w.beans } }, { transaction: t });
    }
    return w;
  });
  await notify(wd.hostId, paid ? 'withdraw_paid' : 'withdraw_rejected',
    paid ? `Withdrawal of ${wd.beans.toLocaleString('en-IN')} beans paid` : `Withdrawal of ${wd.beans.toLocaleString('en-IN')} beans was not paid`,
    paid ? 'Sent to your UPI account' : (note || 'Your beans are back in your balance'), { withdrawalId: Number(wd.id) });
  res.json({ withdrawal: view(wd) });
}
r.post('/admin/withdrawals/:id/paid', wrap((req, res) => decide(req, res, true)));
r.post('/admin/withdrawals/:id/reject', wrap((req, res) => decide(req, res, false)));

module.exports = r;
