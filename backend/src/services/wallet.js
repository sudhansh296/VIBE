const { sequelize, Payment, Wallet, Transaction } = require('../models');

/**
 * Credits a proven purchase. One database transaction with the payment row locked, so the app's confirmation and the webhook
 * arriving at the same moment still credit the coins only once. Returns { payment, credited, coins }.
 */
async function creditPurchase(orderId, paymentId, via) {
  return sequelize.transaction(async (t) => {
    const p = await Payment.findOne({ where: { orderId }, transaction: t, lock: t.LOCK.UPDATE });
    if (!p) return { payment: null, credited: false };
    const w = await Wallet.findByPk(p.userId, { transaction: t, lock: t.LOCK.UPDATE });
    if (p.status === 'paid') return { payment: p, credited: false, coins: Number(w.coins) };
    const coins = Number(w.coins) + p.coins;
    await w.update({ coins }, { transaction: t });
    await p.update({ status: 'paid', paymentId, paidAt: new Date(), paidVia: via, failReason: null }, { transaction: t });
    const inv = await require('../routes/rewards').rewardInvite(t, p.userId);
    if (inv) t.afterCommit(async () => {
      const { notify } = require('./notify');
      await notify(inv.friend.id, 'invite', `Invite reward: +${inv.coins} coins`, 'Thanks for joining with a friend\'s code');
      await notify(inv.inviterId, 'invite', `${inv.friend.name || 'Your friend'} joined with your code`, `+${inv.coins} coins added`);
    });
    if (p.vipPlanId) {
      // VIP: the days are added to what is left (buying again extends it), then the plan's bonus coins
      const { User, VipPlan } = require('../models');
      const u = await User.findByPk(p.userId, { transaction: t, lock: t.LOCK.UPDATE });
      const from = u.vipUntil && new Date(u.vipUntil) > new Date() ? new Date(u.vipUntil) : new Date();
      const until = new Date(from.getTime() + p.vipDays * 86_400_000);
      await u.update({ vipUntil: until }, { transaction: t });
      const plan = await VipPlan.findByPk(p.vipPlanId, { transaction: t });
      await Transaction.create({
        userId: p.userId, wallet: 'coins', kind: 'vip', coins: p.coins, balanceAfter: coins,
        ref: { orderId, paymentId, amountPaise: p.amountPaise, vipPlanId: p.vipPlanId, planName: plan ? plan.name : null, days: p.vipDays, until },
      }, { transaction: t });
      t.afterCommit(() => require('./notify').notify(p.userId, 'vip', `You are VIP until ${until.toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })}`, p.coins > 0 ? `+${p.coins.toLocaleString('en-IN')} bonus coins added` : null));
      return { payment: p, credited: true, coins, vipUntil: until };
    }
    await Transaction.create({
      userId: p.userId, wallet: 'coins', kind: 'recharge', coins: p.coins, balanceAfter: coins,
      ref: { orderId, paymentId, amountPaise: p.amountPaise, packId: p.packId },
    }, { transaction: t });
    t.afterCommit(() => require('./notify').notify(p.userId, 'coins', `${p.coins.toLocaleString('en-IN')} coins added`, 'Thanks for your purchase'));
    return { payment: p, credited: true, coins };
  });
}

/** A payment that did not go through. A paid one is never turned back to failed. */
async function markFailed(orderId, reason) {
  const p = await Payment.findOne({ where: { orderId } });
  if (p && p.status === 'created') await p.update({ status: 'failed', failReason: String(reason || 'Payment failed').slice(0, 200) });
  return p;
}

module.exports = { creditPurchase, markFailed };
