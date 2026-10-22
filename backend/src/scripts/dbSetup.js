// Creates the database (if missing) and all tables, then seeds the default settings. Safe to run again.
// Run: npm run db:setup
const mysql = require('mysql2/promise');
const cfg = require('../config');

(async () => {
  const c = await mysql.createConnection({ host: cfg.db.host, port: cfg.db.port, user: cfg.db.user, password: cfg.db.pass, connectTimeout: 10000 });
  await c.query(`CREATE DATABASE IF NOT EXISTS \`${cfg.db.name}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci`);
  await c.end();
  const { sequelize } = require('../models');
  const { seedSettings } = require('../services/settings');
  await sequelize.sync();
  // columns added after the first setup
  const qi = sequelize.getQueryInterface();
  const sess = await qi.describeTable('sessions');
  if (!sess.revokeReason) await qi.addColumn('sessions', 'revokeReason', { type: require('sequelize').DataTypes.STRING(16) });
  const tx = await qi.describeTable('transactions');
  if (!tx.wallet) await qi.addColumn('transactions', 'wallet', { type: require('sequelize').DataTypes.ENUM('coins', 'earnings'), allowNull: false, defaultValue: 'coins' });
  const hp = await qi.describeTable('host_profiles');
  if (!hp.isOnline) await qi.addColumn('host_profiles', 'isOnline', { type: require('sequelize').DataTypes.BOOLEAN, allowNull: false, defaultValue: false });
  if (!hp.onlineAt) await qi.addColumn('host_profiles', 'onlineAt', { type: require('sequelize').DataTypes.DATE });
  const cv = await qi.describeTable('conversations');
  if (!cv.startedBy) await qi.addColumn('conversations', 'startedBy', { type: require('sequelize').DataTypes.BIGINT.UNSIGNED });
  const ms = await qi.describeTable('messages');
  if (!ms.quantity) await qi.addColumn('messages', 'quantity', { type: require('sequelize').DataTypes.INTEGER.UNSIGNED, allowNull: false, defaultValue: 1 });
  if (!hp.ratingSum) await qi.addColumn('host_profiles', 'ratingSum', { type: require('sequelize').DataTypes.INTEGER.UNSIGNED, allowNull: false, defaultValue: 0 });
  if (!hp.ratingCount) await qi.addColumn('host_profiles', 'ratingCount', { type: require('sequelize').DataTypes.INTEGER.UNSIGNED, allowNull: false, defaultValue: 0 });
  if (!String(ms.kind.type).includes("'call'")) await qi.changeColumn('messages', 'kind', { type: require('sequelize').DataTypes.ENUM('text', 'gift', 'call'), allowNull: false, defaultValue: 'text' });
  const us = await qi.describeTable('users');
  const D = require('sequelize').DataTypes;
  for (const [col, def] of [
    ['xp', { type: D.INTEGER.UNSIGNED, allowNull: false, defaultValue: 0 }], ['level', { type: D.INTEGER.UNSIGNED, allowNull: false, defaultValue: 1 }],
    ['checkinStreak', { type: D.INTEGER.UNSIGNED, allowNull: false, defaultValue: 0 }], ['checkinLast', { type: D.DATEONLY }],
    ['referralCode', { type: D.STRING(12), unique: true }], ['referredBy', { type: D.BIGINT.UNSIGNED }],
    ['referralRewarded', { type: D.BOOLEAN, allowNull: false, defaultValue: false }],
  ]) if (!us[col]) await qi.addColumn('users', col, def);
  // starting levels: XP needed and coins given on reaching each one (the admin changes them later)
  const { LevelRule } = require('../models');
  if (!(await LevelRule.count())) {
    const xp = [0, 300, 700, 1200, 2000, 3000, 4200, 5600, 7200, 9000, 11000, 13500, 16500, 20000, 24000, 28500, 33500, 39000, 45000, 50000];
    await LevelRule.bulkCreate(xp.map((x, i) => ({ level: i + 1, xp: x, rewardCoins: i === 0 ? 0 : (i + 1) * 10 })));
  }
  if (!us.vipUntil) await qi.addColumn('users', 'vipUntil', { type: require('sequelize').DataTypes.DATE });
  const pm = await qi.describeTable('payments');
  if (!pm.vipPlanId) await qi.addColumn('payments', 'vipPlanId', { type: require('sequelize').DataTypes.INTEGER.UNSIGNED });
  if (!pm.vipDays) await qi.addColumn('payments', 'vipDays', { type: require('sequelize').DataTypes.INTEGER.UNSIGNED });
  // starting VIP plans (the admin changes them later); only when there are none yet
  const { VipPlan } = require('../models');
  if (!(await VipPlan.count())) {
    await VipPlan.bulkCreate([
      { name: '1 Month', days: 30, pricePaise: 29900, bonusCoins: 3000, sort: 1 },
      { name: '3 Months', days: 90, pricePaise: 69900, bonusCoins: 10000, tag: 'Popular', sort: 2 },
      { name: '12 Months', days: 365, pricePaise: 199900, bonusCoins: 45000, tag: 'Best value', sort: 3 },
    ]);
  }
  const hp2 = await qi.describeTable('host_profiles');
  if (!hp2.payoutUpiEnc) await qi.addColumn('host_profiles', 'payoutUpiEnc', { type: require('sequelize').DataTypes.STRING(300) });
  if (!hp2.payoutName) await qi.addColumn('host_profiles', 'payoutName', { type: require('sequelize').DataTypes.STRING(80) });
  const cl = await qi.describeTable('calls');
  if (!cl.byHost) await qi.addColumn('calls', 'byHost', { type: require('sequelize').DataTypes.BOOLEAN, allowNull: false, defaultValue: false });
  const ha = await qi.describeTable('host_applications');
  if (!ha.idNumberEnc) await qi.addColumn('host_applications', 'idNumberEnc', { type: require('sequelize').DataTypes.STRING(200) });
  // starting coin packs (the admin changes them later); only when there are none yet
  const { CoinPack } = require('../models');
  if (!(await CoinPack.count())) {
    await CoinPack.bulkCreate([
      { coins: 600, pricePaise: 4900, sort: 1 },
      { coins: 1500, bonusCoins: 100, pricePaise: 9900, tag: 'Popular', sort: 2 },
      { coins: 3500, bonusCoins: 300, pricePaise: 19900, sort: 3 },
      { coins: 8000, bonusCoins: 1000, pricePaise: 39900, tag: 'Best value', sort: 4 },
      { coins: 20000, bonusCoins: 3000, pricePaise: 89900, sort: 5 },
    ]);
  }
  // starting gifts (the admin uploads pictures and animations later and changes prices)
  const { Gift } = require('../models');
  if (!(await Gift.count())) {
    await Gift.bulkCreate([
      ['Rose', '🌹', 20, 'Popular'], ['Chai', '☕', 50, 'Popular'], ['Choco', '🍫', 99, 'Popular'], ['Teddy', '🧸', 199, 'Popular'],
      ['Heart', '💖', 25, 'Love'], ['Kiss', '💋', 60, 'Love'], ['Bouquet', '💐', 299, 'Love'], ['Ring', '💍', 999, 'Love'],
      ['Crown', '👑', 1999, 'Luxury'], ['Car', '🏎️', 2999, 'Luxury'], ['Yacht', '🛥️', 4999, 'Luxury'], ['Castle', '🏰', 9999, 'Luxury'],
    ].map(([name, emoji, price, category], i) => ({ name, emoji, price, category, sort: i })));
  }
  await seedSettings();
  const [tables] = await sequelize.query('SHOW TABLES');
  console.log(`Database ${cfg.db.name} ready:`, tables.map((t) => Object.values(t)[0]).join(', '));
  await sequelize.close();
})().catch((e) => { console.error('DB setup failed:', e.message); process.exit(1); });
