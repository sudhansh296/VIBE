const { sequelize, User, Wallet, Transaction, LevelRule } = require('../models');
const settings = require('./settings');

let rulesCache = null; let rulesAt = 0;
/** The level table, newest copy at most 30 s old (the admin may change it). */
async function rules() {
  if (!rulesCache || Date.now() - rulesAt > 30_000) { rulesCache = await LevelRule.findAll({ order: [['level', 'ASC']] }); rulesAt = Date.now(); }
  return rulesCache;
}
const levelFor = (list, xp) => list.reduce((lv, r) => (xp >= r.xp ? r.level : lv), 1);

/** Adds XP to a person; reaching a new level gives that level's coins (each level once) and a notice. Never throws. */
async function addXp(userId, amount) {
  if (!userId || !(amount > 0)) return;
  try {
    const list = await rules();
    const up = await sequelize.transaction(async (t) => {
      const u = await User.findByPk(userId, { transaction: t, lock: t.LOCK.UPDATE });
      if (!u) return null;
      const xp = u.xp + Math.floor(amount);
      const from = u.level; const to = Math.max(from, levelFor(list, xp));
      await u.update({ xp, level: to }, { transaction: t });
      if (to <= from) return null;
      const reward = list.filter((r) => r.level > from && r.level <= to).reduce((s, r) => s + r.rewardCoins, 0);
      if (reward > 0) {
        const w = await Wallet.findByPk(userId, { transaction: t, lock: t.LOCK.UPDATE });
        await w.update({ coins: Number(w.coins) + reward }, { transaction: t });
        await Transaction.create({ userId, wallet: 'coins', kind: 'level_reward', coins: reward, balanceAfter: Number(w.coins), ref: { level: to } }, { transaction: t });
      }
      return { to, reward };
    });
    if (up) await require('./notify').notify(userId, 'level', `You reached Level ${up.to}!`, up.reward > 0 ? `+${up.reward.toLocaleString('en-IN')} coins added` : null, { level: up.to });
  } catch (e) { console.error('[xp]', e.message); }
}

/** XP for an action, from the admin settings. */
async function xpFor(key, times = 1) { return Number(await settings.get(key)) * times; }

/** Where a person stands: level, XP, XP of this level and of the next one. */
async function progress(u) {
  const list = await rules();
  const cur = list.find((r) => r.level === u.level) || list[0];
  const next = list.find((r) => r.level === u.level + 1) || null;
  return { level: u.level, xp: u.xp, levelXp: cur ? cur.xp : 0, nextXp: next ? next.xp : null, nextReward: next ? next.rewardCoins : 0 };
}

module.exports = { addXp, xpFor, progress, rules };
