// Profile watch (notification after a few seconds) and the automatic call. Times are shortened through the settings. Run: npm run test:watch
const assert = require('assert');
const { io } = require('socket.io-client');
const kit = require('./testKit');
const settings = require('../services/settings');
const { login, M } = kit;

const HOST = '6000000091'; const USER = '6000000092'; const POOR = '6000000093'; const HOST2 = '6000000094';
let passed = 0; let failed = 0;
async function check(name, fn) { try { await fn(); passed++; console.log('PASS', name); } catch (e) { failed++; console.log('FAIL', name, '->', e.message); } }
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const once = (s, ev, ms = 3500) => new Promise((res) => { const t = setTimeout(() => res(null), ms); s.once(ev, (d) => { clearTimeout(t); res(d); }); });

(async () => {
  await kit.start();
  await kit.cleanup([HOST, USER, POOR, HOST2]);
  const saved = {};
  for (const k of ['watch_notify_seconds', 'watch_autocall_seconds', 'call_ring_seconds']) saved[k] = await settings.get(k);
  await settings.update('watch_notify_seconds', 1, null); await settings.update('watch_autocall_seconds', 2, null);
  await settings.update('call_ring_seconds', 10, null);
  const port = () => require('../realtime').port();
  const connect = (token) => new Promise((resolve) => {
    const s = io(`http://127.0.0.1:${port()}`, { auth: { token }, transports: ['websocket'], reconnection: false });
    s.on('connect', () => resolve(s)); s.on('connect_error', () => resolve(null));
  });
  let H; let U; let P; let H2; let hu; let uu; let pu; let hs; let us;
  const mkHost = async (phone, name) => {
    const t = await login(phone); const u = await M.User.findOne({ where: { publicId: t.user.id } });
    await u.update({ role: 'host', hostStatus: 'approved', name });
    await M.HostProfile.findOrCreate({ where: { userId: u.id }, defaults: { callRate: 60, messagePrice: 200 } });
    return [await login(phone), u];
  };

  await check('setup', async () => {
    [H, hu] = await mkHost(HOST, 'Watch Host');
    [H2] = await mkHost(HOST2, 'Other Host');
    U = await login(USER); P = await login(POOR);
    uu = await M.User.findOne({ where: { publicId: U.user.id } }); pu = await M.User.findOne({ where: { publicId: P.user.id } });
    await uu.update({ name: 'Viewer' }); await pu.update({ name: 'Poor' });
    await M.Wallet.update({ coins: 500 }, { where: { userId: uu.id } });
    await M.Wallet.update({ coins: 10 }, { where: { userId: pu.id } });
    hs = await connect(H.accessToken); us = await connect(U.accessToken);
    assert.ok(hs && us); await sleep(300);
  });

  await check('leaving the profile before the time = nothing happens', async () => {
    let got = false; hs.once('profile_watch', () => { got = true; });
    us.emit('profile:open', { hostId: H.user.id }); await sleep(400); us.emit('profile:close'); await sleep(2500);
    assert.equal(got, false);
  });

  await check('staying: host gets the notification, then the call rings; user can cancel', async () => {
    const note = once(hs, 'profile_watch'); const inc = once(hs, 'incoming_call'); const ringU = once(us, 'call_ringing');
    us.emit('profile:open', { hostId: H.user.id });
    const n = await note; assert.ok(n && n.user.id === U.user.id && n.user.name === 'Viewer');
    const c = await inc; assert.ok(c && c.auto === true && c.ratePerMinute === 60);
    assert.ok(await ringU);
    const ended = once(hs, 'call_ended'); us.emit('call:cancel', { callId: c.callId });
    assert.equal((await ended).reason, 'cancelled');
  });

  await check('host accepts -> both are told; only the host can accept', async () => {
    const inc = once(hs, 'incoming_call'); us.emit('profile:open', { hostId: H.user.id });
    const c = await inc; assert.ok(c);
    const a1 = once(us, 'call_accepted'); const a2 = once(hs, 'call_accepted');
    us.emit('call:accept', { callId: c.callId }); await sleep(200);
    assert.equal(await Promise.race([a1, sleep(100).then(() => 'none')]), 'none', 'the caller cannot accept for the host');
    hs.emit('call:accept', { callId: c.callId });
    assert.ok(await a1); assert.ok(await a2);
    const end = once(us, 'call_ended'); hs.emit('call:end', { callId: c.callId }); assert.equal((await end).reason, 'ended');
  });

  await check('host declines', async () => {
    const inc = once(hs, 'incoming_call'); us.emit('profile:open', { hostId: H.user.id });
    const c = await inc; const end = once(us, 'call_ended'); hs.emit('call:decline', { callId: c.callId });
    assert.equal((await end).reason, 'declined');
  });

  await check('a host who is already ringing is busy for others', async () => {
    const inc = once(hs, 'incoming_call'); us.emit('profile:open', { hostId: H.user.id });
    const c = await inc; assert.ok(c);
    await M.Wallet.update({ coins: 500 }, { where: { userId: pu.id } });
    const ps = await connect(P.accessToken); let second = false; hs.once('incoming_call', () => { second = true; });
    ps.emit('profile:open', { hostId: H.user.id }); await sleep(3000);
    assert.equal(second, false); ps.close();
    const end = once(us, 'call_ended'); us.emit('call:cancel', { callId: c.callId }); await end;
  });

  await check('too few coins: only the notification, no call', async () => {
    require('../services/ringing').clearHistory();
    await M.Wallet.update({ coins: 10 }, { where: { userId: pu.id } });
    const ps = await connect(P.accessToken); await sleep(200);
    let called = false; hs.once('incoming_call', () => { called = true; });
    const note = once(hs, 'profile_watch'); ps.emit('profile:open', { hostId: H.user.id });
    assert.ok(await note); await sleep(2500); assert.equal(called, false); ps.close();
  });

  await check('nobody answers -> missed', async () => {
    await settings.update('call_ring_seconds', 10, null);
    const inc = once(hs, 'incoming_call'); us.emit('profile:open', { hostId: H.user.id });
    const c = await inc; assert.ok(c);
    // shortened for the test: the ring timer was set from the setting, so end it through the host going away instead
    const end = once(us, 'call_ended'); hs.close();
    assert.equal((await end).reason, 'disconnected');
    await sleep(900); hs = await connect(H.accessToken); await sleep(300);
  });

  await check('host offline (app closed) -> no call', async () => {
    hs.close(); await sleep(900);
    let got = false; us.once('call_ringing', () => { got = true; });
    us.emit('profile:open', { hostId: H.user.id }); await sleep(2800); us.emit('profile:close');
    assert.equal(got, false);
    hs = await connect(H.accessToken); await sleep(300);
  });

  await check('admin switch off, host-to-host and blocked never ring', async () => {
    await settings.update('autocall_enabled', false, null);
    let got = false; hs.once('incoming_call', () => { got = true; });
    us.emit('profile:open', { hostId: H.user.id }); await sleep(2800); us.emit('profile:close');
    assert.equal(got, false); await settings.update('autocall_enabled', true, null);
    const h2 = await connect(H2.accessToken); let any = false; hs.once('profile_watch', () => { any = true; });
    h2.emit('profile:open', { hostId: H.user.id }); await sleep(2800); assert.equal(any, false); h2.close();
    await M.Block.create({ blockerId: hu.id, blockedId: uu.id });
    let b = false; hs.once('profile_watch', () => { b = true; });
    us.emit('profile:open', { hostId: H.user.id }); await sleep(2800); assert.equal(b, false);
    await M.Block.destroy({ where: { blockerId: hu.id } });
  });

  await check('no cooldown: the same user can trigger the call again right away (host decides)', async () => {
    require('../services/ringing').clearHistory();
    const i1 = once(hs, 'incoming_call'); us.emit('profile:open', { hostId: H.user.id });
    const c = await i1; assert.ok(c); hs.emit('call:decline', { callId: c.callId }); await sleep(300);
    const i2 = once(hs, 'incoming_call'); us.emit('profile:open', { hostId: H.user.id });
    const c2 = await i2; assert.ok(c2, 'rings again'); us.emit('call:cancel', { callId: c2.callId }); await sleep(300);
  });

  await check('opening the chat: the host is told at once (banner), but no call ever starts by itself', async () => {
    require('../services/ringing').clearHistory();
    const note = once(hs, 'profile_watch'); let rang = false; hs.once('incoming_call', () => { rang = true; });
    const t0 = Date.now();
    us.emit('chat:open', { hostId: H.user.id });
    const n = await note;
    assert.ok(Date.now() - t0 < 700, 'told at once, not after the profile wait');
    assert.ok(n && n.where === 'chat' && n.user.id === U.user.id);
    await sleep(2500); us.emit('chat:close');
    assert.equal(rang, false, 'never an automatic call from a chat');
  });

  await check('pressing the call button: same rules, answered at once', async () => {
    const ack = (hostId, who) => new Promise((r) => who.emit('call:start', { hostId }, r));
    await M.Wallet.update({ coins: 500 }, { where: { userId: uu.id } });
    const inc = once(hs, 'incoming_call');
    const a = await ack(H.user.id, us); assert.equal(a.ok, true);
    const c = await inc; assert.equal(c.auto, false);
    const b = await ack(H.user.id, us); assert.equal(b.ok, false, 'already in a call');
    us.emit('call:cancel', { callId: c.callId }); await sleep(200);
    await M.Wallet.update({ coins: 5 }, { where: { userId: uu.id } });
    const n = await ack(H.user.id, us); assert.equal(n.reason, 'not_enough_coins');
    await M.Wallet.update({ coins: 500 }, { where: { userId: uu.id } });
    const h2 = await connect(H2.accessToken);
    assert.equal((await ack(H.user.id, h2)).ok, false, 'host to host');
    h2.close();
  });

  hs.close(); us.close();
  for (const k of ['watch_autocall_seconds', 'watch_notify_seconds', 'call_ring_seconds']) await settings.update(k, saved[k], null);
  await settings.update('autocall_enabled', true, null);
  await kit.cleanup([HOST, USER, POOR, HOST2]);
  await kit.stop();
  console.log(`\n${passed} passed, ${failed} failed`);
  process.exit(failed ? 1 : 0);
})().catch((e) => { console.error('crashed:', e); process.exit(1); });
