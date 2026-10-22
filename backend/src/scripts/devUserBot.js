// DEV ONLY: a pretend user for testing the host side of the app on a phone. Logs in "Test User" (dev login), gives it coins,
// opens a host's profile over Socket.IO and stays there (so the host gets the "looking at your profile" banner and then the auto call).
// Usage: node src/scripts/devUserBot.js <hostPublicId> [stay|call] [seconds to stay before leaving]
require('dotenv').config();
const { io } = require('socket.io-client');
const M = require('../models');

const BASE = `http://127.0.0.1:${process.env.PORT || 4000}`;
const PHONE = '6000000202';
const [hostId, mode = 'stay', staySec = '40'] = process.argv.slice(2);
const log = (...a) => console.log(new Date().toISOString().slice(11, 19), ...a);
const post = async (p, body) => (await fetch(BASE + p, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) })).json();

(async () => {
  await post('/auth/dev/request-otp', { phone: PHONE });
  const t = await post('/auth/dev/verify-otp', { phone: PHONE, code: '123456' });
  if (!t.user) throw new Error(JSON.stringify(t));
  const u = await M.User.findOne({ where: { publicId: t.user.id } });
  await u.update({ name: 'Test User', gender: 'male', dob: '1998-03-12', city: 'Jaipur' });
  await M.Wallet.update({ coins: 1000 }, { where: { userId: u.id } });
  log(`user ready: id ${t.user.id}, looking at host ${hostId}, mode ${mode}`);

  const s = io(BASE, { auth: { token: t.accessToken }, transports: ['websocket'] });
  s.on('connect', () => {
    log('connected');
    if (mode === 'call') s.emit('call:start', { hostId: Number(hostId) }, (r) => log('call:start ->', JSON.stringify(r)));
    else s.emit('profile:open', { hostId: Number(hostId) });
    setTimeout(() => { log('leaving'); s.emit('profile:close'); s.close(); M.sequelize.close(); }, Number(staySec) * 1000);
  });
  s.on('connect_error', (e) => log('connect error', e.message));
  s.on('call_ringing', (c) => log('RINGING at the host', c.auto ? '(auto)' : '(button)'));
  s.on('call_accepted', () => log('host ACCEPTED'));
  s.on('call_ended', (c) => log('call ended:', c.reason));
})().catch((e) => { console.error(e.message); process.exit(1); });
