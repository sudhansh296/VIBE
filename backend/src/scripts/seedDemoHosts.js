// DEV ONLY: puts the app's 8 test hosts into the database (same names, ages and the IDs the app shows: 40362381 ...),
// so block / report from the app work against the real server until Phase 2 brings real host sign-up. Safe to run again.
// Run: npm run seed:demo
const cfg = require('../config');
const { sequelize, User, Wallet, HostProfile } = require('../models');

if (cfg.isProd) { console.error('Refusing to seed demo hosts in production'); process.exit(1); }

const HOSTS = [
  // id, name, age, gender, city, languages, video rate (coins/min) as the app shows it
  [1, 'Priya', 24, 'female', 'Mumbai', ['Hindi', 'English'], 60],
  [2, 'Arjun', 26, 'male', 'Delhi', ['Hindi', 'Punjabi'], 50],
  [3, 'Ananya', 22, 'female', 'Bangalore', ['English', 'Kannada'], 80],
  [4, 'Riya', 27, 'female', 'Pune', ['Marathi', 'Hindi'], 40],
  [5, 'Diya', 23, 'female', 'Chennai', ['Tamil', 'English'], 60],
  [6, 'Kavya', 25, 'female', 'Jaipur', ['Hindi', 'English'], 70],
  [7, 'Meera', 28, 'female', 'Kolkata', ['Bengali', 'Hindi'], 100],
  [8, 'Vidya', 29, 'female', 'Hyderabad', ['Telugu', 'Hindi'], 60],
];

(async () => {
  await sequelize.sync();
  const year = new Date().getUTCFullYear();
  for (const [n, name, age, gender, city, languages, rate] of HOSTS) {
    const publicId = 40362380 + n;
    const [u, created] = await User.findOrCreate({
      where: { publicId },
      defaults: { name, gender, dob: `${year - age - 1}-12-31`, city, languages, role: 'host', hostStatus: 'approved', status: 'active' },
    });
    if (created) await Wallet.create({ userId: u.id });
    await HostProfile.findOrCreate({ where: { userId: u.id }, defaults: { callRate: rate, messagePrice: 200 } });
    console.log(`${created ? 'created' : 'exists '} ${publicId} ${name}`);
  }
  await sequelize.close();
})().catch((e) => { console.error(e.message); process.exit(1); });
