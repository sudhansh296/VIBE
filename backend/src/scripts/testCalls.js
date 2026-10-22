// Phase 5: per-minute billing (charged at the start of each minute), who keeps the running minute, WebRTC relay,
// reconnect grace, Random match, rating, history, ICE servers. A "minute" is 1.5 s here. Run: npm run test:calls
process.env.CALL_MINUTE_MS = '1500';
const assert = require('assert');
const { io } = require('socket.io-client');
const kit = require('./testKit');
const settings = require('../services/settings');
const { call, login, M } = kit;

const U = '6000000131'; const U2 = '6000000132'; const H = '6000000133'; const H2 = '6000000134';
let passed = 0; let failed = 0;
async function check(name, fn) { try { await fn(); passed++; console.log('PASS', name); } catch (e) { failed++; console.log('FAIL', name, '->', e.message); } }
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const minuteN = (s, n, ms = 5000) => new Promise((res) => {
  const t = setTimeout(() => { s.off('call_billing', f); res(null); }, ms);
  const f = (b) => { if (b.minute === n) { clearTimeout(t); s.off('call_billing', f); res(b); } };
  s.on('call_billing', f);
});
const once = (s, ev, ms = 4000) => new Promise((res) => { const t = setTimeout(() => res(null), ms); s.once(ev, (d) => { clearTimeout(t); res(d); }); });

(async () => {
  await kit.start();
  await kit.cleanup([U, U2, H, H2]);
  const saved = { share: await settings.get('host_share_pct'), grace: await settings.get('call_reconnect_seconds'), random: await settings.get('random_match_rate') };
  await settings.update('host_share_pct', 60, null);
  await settings.update('call_reconnect_seconds', 5, null);
  await settings.update('random_match_rate', 100, null);
  const port = () => require('../realtime').port();
  const connect = (token) => new Promise((resolve) => {
    const s = io(`http://127.0.0.1:${port()}`, { auth: { token }, transports: ['websocket'], reconnection: false });
    s.on('connect', () => resolve(s)); s.on('connect_error', () => resolve(null));
  });
  let u; let u2; let h; let h2; let uu; let hu; let us; let hs;
  const coins = async () => Number((await M.Wallet.findByPk(uu.id)).coins);
  const earnings = async () => Number((await M.Wallet.findByPk(hu.id)).earnings);
  const startAndAccept = async (userSock = us) => {
    const ack = await new Promise((r) => userSock.emit('call:start', { hostId: h.user.id }, r));
    assert.equal(ack.ok, true, JSON.stringify(ack));
    const acc = once(userSock, 'call_accepted');
    hs.emit('call:accept', { callId: ack.callId });
    assert.ok(await acc, 'accepted');
    return ack.callId;
  };

  await check('setup', async () => {
    u = await login(U); u2 = await login(U2); h = await login(H); h2 = await login(H2);
    for (const [t, name] of [[u, 'Call User'], [u2, 'Call User 2']]) await M.User.update({ name }, { where: { publicId: t.user.id } });
    for (const [t, name, rate] of [[h, 'Call Host', 50], [h2, 'Call Host 2', 70]]) {
      await M.User.update({ name, role: 'host', hostStatus: 'approved', gender: 'female' }, { where: { publicId: t.user.id } });
      const x = await M.User.findOne({ where: { publicId: t.user.id } });
      await M.HostProfile.findOrCreate({ where: { userId: x.id }, defaults: { callRate: rate, messagePrice: 30 } });
    }
    h = await login(H); h2 = await login(H2);
    uu = await M.User.findOne({ where: { publicId: u.user.id } }); hu = await M.User.findOne({ where: { publicId: h.user.id } });
    await M.Wallet.update({ coins: 1000 }, { where: { userId: uu.id } });
    us = await connect(u.accessToken); hs = await connect(h.accessToken);
    assert.ok(us && hs); await sleep(400);
  });

  await check('the first minute is charged at pick-up, the next at the start of each minute; the host earns its share', async () => {
    const c0 = await coins(); const e0 = await earnings();
    const bill = once(us, 'call_billing');
    const id = await startAndAccept();
    const b = await bill;
    assert.ok(b && b.minute === 1 && b.coinsLeft === c0 - 50, JSON.stringify(b));
    assert.equal(await earnings(), e0 + 30);
    const b2 = await once(us, 'call_billing', 3000);
    assert.equal(b2.minute, 2); assert.equal(await coins(), c0 - 100);
    await sleep(500);
    // the USER hangs up in the middle of minute 2: the host keeps it
    const end = once(hs, 'call_ended');
    us.emit('call:end', { callId: id });
    const e = await end;
    assert.equal(e.endedBy, 'user'); assert.equal(e.coins, 100); assert.equal(e.earned, 60);
    assert.equal(await coins(), c0 - 100); assert.equal(await earnings(), e0 + 60);
    const row = await M.Call.findByPk(id);
    assert.equal(row.status, 'ended'); assert.equal(row.coinsCharged, 100); assert.equal(row.hostEarned, 60); assert.ok(row.seconds >= 1);
  });

  await check('the HOST hangs up during a minute: the user gets that minute back, the host loses its share of it', async () => {
    const c0 = await coins(); const e0 = await earnings();
    const m2 = minuteN(us, 2);
    const id = await startAndAccept();
    assert.ok(await m2, 'minute 2 starts');
    await sleep(300);
    const end = once(us, 'call_ended');
    hs.emit('call:end', { callId: id });
    const e = await end;
    assert.equal(e.endedBy, 'host');
    assert.equal(e.coins, 50, 'only minute 1 is paid');
    assert.equal(await coins(), c0 - 50); assert.equal(await earnings(), e0 + 30);
    const hist = (await call('GET', '/wallet/history', { token: u.accessToken })).json.items;
    assert.equal(hist[0].kind, 'call_refund');
  });

  await check('when the coins run out the call ends at the start of the minute that cannot be paid', async () => {
    await M.Wallet.update({ coins: 120 }, { where: { userId: uu.id } });
    const id = await startAndAccept();
    const end = await once(us, 'call_ended', 6000);
    assert.equal(end.reason, 'no_coins'); assert.equal(end.minutes, 2);
    assert.equal(await coins(), 20);
    assert.ok(id);
    await M.Wallet.update({ coins: 1000 }, { where: { userId: uu.id } });
  });

  await check('WebRTC offer / answer / ICE go only to the other phone of the call', async () => {
    const id = await startAndAccept();
    const stranger = await connect(u2.accessToken);
    let leaked = false; stranger.on('rtc_signal', () => { leaked = true; });
    const got = once(hs, 'rtc_signal');
    us.emit('rtc:signal', { callId: id, data: { type: 'offer', sdp: 'v=0 test' } });
    const sig = await got;
    assert.equal(sig.data.type, 'offer'); assert.equal(sig.callId, id);
    let hostGotFake = false; hs.once('rtc_signal', () => { hostGotFake = true; });
    stranger.emit('rtc:signal', { callId: id, data: { type: 'offer', sdp: 'evil' } });
    const back = once(us, 'rtc_signal');
    hs.emit('rtc:signal', { callId: id, data: { type: 'answer', sdp: 'v=0 answer' } });
    assert.equal((await back).data.type, 'answer');
    const chat = once(hs, 'call_chat');
    us.emit('call:chat', { callId: id, text: 'hello in the call' });
    assert.equal((await chat).text, 'hello in the call');
    await sleep(300);
    assert.equal(leaked, false); assert.equal(hostGotFake, false);
    stranger.close();
    const end = once(hs, 'call_ended'); us.emit('call:end', { callId: id }); await end;
  });

  await check('a dropped connection gets time to come back; the call goes on', async () => {
    const id = await startAndAccept();
    const lost = once(us, 'call_peer_lost');
    hs.close();
    assert.ok(await lost, 'the user is told the host lost the connection');
    hs = await connect(h.accessToken);
    const resume = await once(hs, 'call_resume', 2000).catch(() => null);
    await sleep(300);
    assert.ok(require('../services/ringing').isBusy(hu.id), 'still in the call');
    const end = once(us, 'call_ended'); hs.emit('call:end', { callId: id }); await end;
    assert.ok(resume === null || resume.callId === id);
  });

  await check('a dropped connection that never comes back ends the call; a dropped host refunds the running minute', async () => {
    const c0 = await coins();
    const id = await startAndAccept();
    const end = once(us, 'call_ended', 9000);
    hs.close();
    const e = await end;
    assert.equal(e.reason, 'disconnected'); assert.equal(e.endedBy, 'host');
    const row = await M.Call.findByPk(id);
    assert.equal(await coins(), c0 - row.coinsCharged);
    hs = await connect(h.accessToken); await sleep(300);
  });

  await check('a host calls a user: the user is rung (role user), picks up, and still pays the host rate; the call line is from the host', async () => {
    const ring = once(us, 'incoming_call'); const back = once(hs, 'call_ringing');
    const ack = await new Promise((r) => hs.emit('call:start', { userId: u.user.id }, r));
    assert.equal(ack.ok, true, JSON.stringify(ack));
    const inc = await ring; const rg = await back;
    assert.equal(inc.role, 'user'); assert.equal(inc.from.id, h.user.id); assert.equal(inc.ratePerMinute, 50);
    assert.equal(rg.role, 'host'); assert.equal(rg.host.id, u.user.id);
    hs.emit('call:accept', { callId: ack.callId }); await sleep(400);            // the caller cannot pick up his own call
    assert.equal((await M.Call.findByPk(ack.callId)).status, 'ringing');
    const before = await coins(); const e0 = await earnings();
    const m1 = minuteN(us, 1); us.emit('call:accept', { callId: ack.callId });
    assert.ok(await m1, 'first minute billed');
    assert.equal(before - (await coins()), 50); assert.equal((await earnings()) - e0, 30);
    const end = once(us, 'call_ended'); hs.emit('call:end', { callId: ack.callId }); await end;   // host hangs up: minute back
    assert.equal(before - (await coins()), 0);
    await sleep(400);   // the chat line is written just after call_ended
    const conv = await M.Conversation.findOne({ where: { userId: uu.id, hostId: hu.id } });
    const line = await M.Message.findOne({ where: { conversationId: conv.id, kind: 'call' }, order: [['id', 'DESC']] });
    assert.equal(String(line.senderId), String(hu.id));
  });

  await check('a host calls a user who declines / cannot pay / is a host', async () => {
    const ring = once(us, 'incoming_call');
    const ack = await new Promise((r) => hs.emit('call:start', { userId: u.user.id }, r));
    await ring; const end = once(hs, 'call_ended'); us.emit('call:decline', { callId: ack.callId });
    assert.equal((await end).reason, 'declined');
    // a user without coins still gets the call; picking up asks for a recharge, recharging rings longer, then it works
    const keep = await coins(); await M.Wallet.update({ coins: 10 }, { where: { userId: uu.id } });
    const ring2 = once(us, 'incoming_call');
    const poor = await new Promise((r) => hs.emit('call:start', { userId: u.user.id }, r));
    assert.equal(poor.ok, true, 'it rings even without coins'); await ring2;
    const need = once(us, 'call_need_coins'); us.emit('call:accept', { callId: poor.callId });
    assert.equal((await need).need, 50);
    assert.equal((await M.Call.findByPk(poor.callId)).status, 'ringing', 'not picked up without coins');
    const told = once(hs, 'call_topping'); us.emit('call:topping', { callId: poor.callId });
    assert.ok((await told).seconds > 0, 'the host is told the user is recharging');
    await M.Wallet.update({ coins: keep }, { where: { userId: uu.id } });   // the recharge went through
    const acc = once(us, 'call_accepted'); us.emit('call:accept', { callId: poor.callId });
    assert.ok(await acc, 'picked up after recharging');
    const fin = once(hs, 'call_ended'); us.emit('call:end', { callId: poor.callId }); await fin;
    const h2s = await connect(h2.accessToken); await sleep(300);
    const other = await new Promise((r) => hs.emit('call:start', { userId: h2.user.id }, r));
    assert.equal(other.ok, false); h2s.close();
  });

  await check('a gift during a call: the host sees it at once and the summary counts it (minutes + gift share)', async () => {
    const callId = await startAndAccept();
    await minuteN(us, 1);
    const gift = await M.Gift.findOne({ where: { active: true }, order: [['price', 'ASC']] });
    const got = once(hs, 'call_earned');
    const r = await call('POST', '/gifts/send', { token: u.accessToken, body: { toId: h.user.id, giftId: gift.id, quantity: 2, context: 'call', clientId: 'callgift-' + Date.now() } });
    assert.equal(r.status, 200, JSON.stringify(r.json));
    const share = Math.floor((gift.price * 2 * 60) / 100);
    assert.equal((await got).earned, 30 + share);
    const end = once(hs, 'call_ended'); us.emit('call:end', { callId }); const sum = await end;
    assert.equal(sum.giftEarned, share); assert.equal(sum.gifts, 2); assert.equal(sum.earned, sum.callEarned + share);
  });

  await check('Random match: fixed rate for every host, gender filter, none online', async () => {
    const c0 = await coins();
    const inc = once(hs, 'incoming_call');
    const ack = await new Promise((r) => us.emit('random:start', { gender: 'female' }, r));
    assert.equal(ack.ok, true, JSON.stringify(ack)); assert.equal(ack.rate, 100);
    const i = await inc; assert.equal(i.kind, 'random'); assert.equal(i.ratePerMinute, 100, 'the random rate, not the host rate (50)');
    const bill = once(us, 'call_billing'); hs.emit('call:accept', { callId: ack.callId });
    assert.equal((await bill).rate, 100);
    assert.equal(await coins(), c0 - 100);
    const end = once(hs, 'call_ended'); us.emit('call:end', { callId: ack.callId }); await end;
    const none = await new Promise((r) => us.emit('random:start', { gender: 'male' }, r));
    assert.equal(none.reason, 'no_hosts');
    const skip = await new Promise((r) => us.emit('random:start', { skip: [h.user.id] }, r));
    assert.equal(skip.reason, 'no_hosts', 'a skipped host is not picked again');
  });

  await check('rating once per finished call; it shows on the host card', async () => {
    const list = (await call('GET', '/calls', { token: u.accessToken })).json.items;
    const answered = list.find((x) => x.result === 'answered');
    assert.ok(answered && answered.peer && answered.peer.id === h.user.id && answered.coins > 0);
    assert.equal((await call('POST', `/calls/${answered.id}/rate`, { token: h.accessToken, body: { stars: 5 } })).status, 404, 'the host cannot rate');
    assert.equal((await call('POST', `/calls/${answered.id}/rate`, { token: u.accessToken, body: { stars: 4, tags: ['Friendly'] } })).status, 200);
    assert.equal((await call('POST', `/calls/${answered.id}/rate`, { token: u.accessToken, body: { stars: 5 } })).status, 409);
    const p = (await call('GET', `/profile/${h.user.id}`, { token: u.accessToken })).json.person;
    assert.equal(p.rating, 4); assert.equal(p.ratingCount, 1);
    const hostList = (await call('GET', '/calls', { token: h.accessToken })).json.items;
    assert.ok(hostList[0].earned !== undefined && hostList[0].coins === undefined, "a host sees earnings, not the user's coins");
  });

  await check('a host profile shows real calls, answer rate and what people say; a user profile has no stats', async () => {
    const r = (await call('GET', `/profile/${h.user.id}`, { token: u.accessToken })).json;
    assert.ok(r.stats.calls > 0, 'calls'); assert.ok(r.stats.answerRate > 0 && r.stats.answerRate <= 100, 'answer rate');
    assert.deepEqual(r.stats.tags, [{ tag: 'Friendly', count: 1 }]);
    assert.ok(Array.isArray(r.stats.gifts) && r.stats.giftsTotal === r.stats.gifts.reduce((s, g) => s + g.count, 0));
    const asked = await M.Call.count({ where: { hostId: hu.id, byHost: true } });
    assert.ok(asked >= 2, 'host-made calls are marked and left out of the answer rate');
    const up = (await call('GET', `/profile/${u.user.id}`, { token: h.accessToken })).json;
    assert.equal(up.stats, undefined);
  });

  await check('a call leaves a line in their chat; history shows one line per call', async () => {
    const msgs = (await call('GET', `/chats/${h.user.id}/messages`, { token: u.accessToken })).json.messages.filter((m) => m.kind === 'call');
    assert.ok(msgs.length >= 3 && msgs.some((m) => Number(m.text) > 0));
    const hist = (await call('GET', '/wallet/history', { token: u.accessToken })).json.items.filter((x) => x.kind === 'call_spend');
    assert.ok(hist.some((x) => / · \d+ min$/.test(x.title)), JSON.stringify(hist.map((x) => x.title)));
  });

  await check('ICE servers for the phones (STUN; TURN when configured, with short-lived passwords)', async () => {
    const r = await call('GET', '/calls/ice', { token: u.accessToken });
    assert.ok(r.json.iceServers[0].urls.length >= 1);
    process.env.TURN_URLS = 'turn:turn.example.com:3478'; process.env.TURN_SECRET = 'test-secret';
    const t = await call('GET', '/calls/ice', { token: u.accessToken });
    const turn = t.json.iceServers.find((s) => s.username);
    assert.ok(turn && turn.credential && turn.username.endsWith(`:${u.user.id}`));
    delete process.env.TURN_URLS; delete process.env.TURN_SECRET;
  });

  us.close(); hs.close();
  const ids = (await M.User.findAll({ where: { phone: [U, U2, H, H2].map((p) => '+91' + p) } })).map((x) => x.id);
  await M.Call.destroy({ where: { userId: ids } });
  await settings.update('host_share_pct', saved.share, null);
  await settings.update('call_reconnect_seconds', saved.grace, null);
  await settings.update('random_match_rate', saved.random, null);
  await kit.cleanup([U, U2, H, H2]);
  await kit.stop();
  console.log(`\n${passed} passed, ${failed} failed`);
  process.exit(failed ? 1 : 0);
})().catch((e) => { console.error('crashed:', e); process.exit(1); });
