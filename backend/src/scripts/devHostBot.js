// DEV ONLY: a pretend host for testing the app on a phone. Logs in a test host (dev login), gives it photos, keeps it online
// over Socket.IO and prints everything the server sends. Usage: node src/scripts/devHostBot.js [ignore|accept|accept-end|decline] [reply]
require('dotenv').config();
const fs = require('fs');
const path = require('path');
const { io } = require('socket.io-client');
const M = require('../models');

const BASE = `http://127.0.0.1:${process.env.PORT || 4000}`;
// BOT_PHONE / BOT_NAME / BOT_PHOTOS make a second pretend host (e.g. 6000000203, "Test Host 2", 2)
const PHONE = process.env.BOT_PHONE || '6000000201';
const NAME = process.env.BOT_NAME || 'Test Host';
const PHOTO_SET = process.env.BOT_PHOTOS || '1';
const MODE = process.argv[2] || 'ignore';
const log = (...a) => console.log(new Date().toISOString().slice(11, 19), ...a);

async function call(method, p, token, body, form) {
  const headers = token ? { Authorization: `Bearer ${token}` } : {};
  if (body) headers['Content-Type'] = 'application/json';
  const r = await fetch(BASE + p, { method, headers, body: form || (body ? JSON.stringify(body) : undefined) });
  return r.json();
}
async function login() {
  await call('POST', '/auth/dev/request-otp', null, { phone: PHONE });
  return call('POST', '/auth/dev/verify-otp', null, { phone: PHONE, code: '123456' });
}

(async () => {
  let t = await login();
  const u = await M.User.findOne({ where: { publicId: t.user.id } });
  await u.update({ name: NAME, gender: 'female', dob: '2000-05-10', city: 'Delhi', bio: 'Test host for the phone check', languages: ['Hindi', 'English'] });
  if (!(await M.Media.count({ where: { userId: u.id, kind: 'photo' } }))) {
    const res = 'C:/VIBE/call me/app/app/src/main/res/drawable-nodpi';
    for (const n of [0, 1, 2]) {
      const f = new FormData();
      f.append('file', new Blob([fs.readFileSync(path.join(res, `host_${PHOTO_SET}_${n}.webp`))], { type: 'image/webp' }), `p${n}.webp`);
      log('photo upload', (await call('POST', '/me/media', t.accessToken, null, f)).added || 'failed');
    }
  }
  await u.update({ role: 'host', hostStatus: 'approved' });
  await M.HostProfile.findOrCreate({ where: { userId: u.id }, defaults: { callRate: 60, messagePrice: 200 } });
  const main = (await M.Media.findAll({ where: { userId: u.id, kind: 'photo' } }))[0];
  if (main && !u.dpPath) await call('POST', `/me/media/${main.id}/main`, t.accessToken);
  t = await login();
  log(`host ready: id ${t.user.id}, mode ${MODE}`);

  const s = io(BASE, { auth: { token: t.accessToken }, transports: ['websocket'] });
  s.on('connect', () => log('connected (online)'));
  s.on('connect_error', (e) => log('connect error', e.message));
  s.on('presence', (p) => log('presence', JSON.stringify(p)));
  s.on('profile_watch', (p) => log('WATCH (' + (p.where || 'profile') + '):', p.user.name, p.user.id));
  s.on('incoming_call', (c) => {
    log('INCOMING CALL', c.auto ? '(auto)' : '(button)', 'from', c.from.name, 'rate', c.ratePerMinute);
    if (MODE === 'accept' || MODE === 'accept-end') setTimeout(() => s.emit('call:accept', { callId: c.callId }), 3000);
    if (MODE === 'accept-end') setTimeout(() => { log('host hangs up'); s.emit('call:end', { callId: c.callId }); }, 9000);
    if (MODE === 'decline') setTimeout(() => s.emit('call:decline', { callId: c.callId }), 3000);
  });
  s.on('call_accepted', (c) => log('call accepted', c.callId));
  s.on('call_ended', (c) => log('call ended:', c.reason));
  s.on('gift', (g) => log('GIFT:', g.from && g.from.name, 'sent', g.quantity > 1 ? `${g.quantity}x` : '', g.gift.name, `(${g.gift.animType || 'no animation'})`));
  s.on('chat_typing', (t) => log('typing from', t.peerId));
  s.on('chat_message', async (e) => {
    if (e.message.mine) return;
    log('MESSAGE from', e.peerId, ':', e.message.kind === 'gift' ? `[gift ${e.message.gift && e.message.gift.name}]` : e.message.text);
    if (!process.argv.includes('reply')) return;
    // read it, show "typingâ€¦", then answer
    await fetch(`${BASE}/chats/${e.peerId}/read`, { method: 'POST', headers: { Authorization: `Bearer ${t.accessToken}` } });
    s.emit('chat:typing', { peerId: e.peerId });
    setTimeout(async () => {
      const text = e.message.kind === 'gift' ? 'Thank you for the gift! ðŸ˜Š' : `Hi! You said: "${(e.message.text || '').slice(0, 40)}"`;
      const r = await fetch(`${BASE}/chats/${e.peerId}/messages`, { method: 'POST', headers: { Authorization: `Bearer ${t.accessToken}`, 'Content-Type': 'application/json' }, body: JSON.stringify({ text, clientId: `bot_${Date.now()}` }) });
      log('replied', r.status);
    }, 2000);
  });
})().catch((e) => { console.error(e); process.exit(1); });

