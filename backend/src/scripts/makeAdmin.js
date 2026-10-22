// Makes an existing account an admin. The person must have logged in once. Run: npm run make:admin -- 9876543210
const { User } = require('../models');
const { normalizePhone } = require('../utils/phone');
(async () => {
  const phone = normalizePhone(process.argv[2] || '');
  if (!phone) { console.error('Usage: npm run make:admin -- <10-digit phone>'); process.exit(1); }
  const u = await User.findOne({ where: { phone } });
  if (!u) { console.error('No account with that phone. Log in with it once first.'); process.exit(1); }
  await u.update({ role: 'admin' });
  console.log(`${phone} (${u.name || 'no name'}) is now an admin`);
  await require('../models').sequelize.close();
})().catch((e) => { console.error(e.message); process.exit(1); });
