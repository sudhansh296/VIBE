const settings = require('./settings');

/** VIP rules. A user is VIP while vipUntil is in the future (hosts are never VIP). */
const isVip = (u) => !!(u && u.role !== 'host' && u.vipUntil && new Date(u.vipUntil).getTime() > Date.now());

/** What a call costs [user] per minute: VIP users pay vip_call_discount_pct less (at least 1 coin). */
async function callRateFor(user, rate) {
  if (!isVip(user)) return rate;
  const off = Number(await settings.get('vip_call_discount_pct'));
  return Math.max(1, Math.round((rate * (100 - off)) / 100));
}

module.exports = { isVip, callRateFor };
