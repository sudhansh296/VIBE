// Removes the 8 photo-less demo hosts (public ids 40362381-40362388) and everything that points at them.
// They can be put back any time with: npm run seed:demo
require('dotenv').config();
process.env.LOGIN_RATE_LIMIT = process.env.LOGIN_RATE_LIMIT || '2000';
const kit = require('./testKit');

(async () => {
  const ids = Array.from({ length: 8 }, (_, i) => 40362381 + i);
  const users = await kit.M.User.findAll({ where: { publicId: ids } });
  await kit.cleanup([], users.map((u) => u.id));
  console.log(`removed ${users.length} demo hosts: ${users.map((u) => u.name).join(', ')}`);
  await kit.M.sequelize.close();
})().catch((e) => { console.error(e.message); process.exit(1); });
