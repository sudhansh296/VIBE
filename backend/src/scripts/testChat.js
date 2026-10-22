// Phase 4: chat (free then paid messages, host may start chats), read receipts, typing, gifts (+ live events), admin gifts. Run: npm run test:chat
const assert = require('assert');
const { io } = require('socket.io-client');
const kit = require('./testKit');
const settings = require('../services/settings');
const { call, login, png, M } = kit;

const U = '6000000121'; const U2 = '6000000122'; const H = '6000000123'; const H2 = '6000000124'; const AD = '6000000125';
let passed = 0; let failed = 0;
async function check(name, fn) { try { await fn(); passed++; console.log('PASS', name); } catch (e) { failed++; console.log('FAIL', name, '->', e.message); } }
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const once = (s, ev, ms = 3000) => new Promise((res) => { const t = setTimeout(() => res(null), ms); s.once(ev, (d) => { clearTimeout(t); res(d); }); });
let n = 0; const cid = () => `test_${Date.now()}_${n++}`;

(async () => {
  await kit.start();
  await kit.cleanup([U, U2, H, H2, AD]);
  const saved = { free: await settings.get('free_messages_per_chat'), daily: await settings.get('host_new_chats_per_day') };
  const connect = (token) => new Promise((resolve) => {
    const s = io(`http://127.0.0.1:${require('../realtime').port()}`, { auth: { token }, transports: ['websocket'], reconnection: false });
    s.on('connect', () => resolve(s)); s.on('connect_error', () => resolve(null));
  });
  let u; let u2; let h; let h2; let ad; let hu; let uu; let gifts; let us; let hs;
  const coins = async (t) => (await call('GET', '/wallet', { token: t.accessToken })).json.coins;
  const earnings = async () => Number((await M.Wallet.findByPk(hu.id)).earnings);
  const send = (from, to, text, clientId = cid()) => call('POST', `/chats/${to.user.id}/messages`, { token: from.accessToken, body: { text, clientId } });

  await check('setup', async () => {
    await settings.update('free_messages_per_chat', 2, null);
    u = await login(U); u2 = await login(U2); h = await login(H); h2 = await login(H2); ad = await login(AD);
    for (const [t, name] of [[u, 'Chat User'], [u2, 'Chat User 2']]) await M.User.update({ name }, { where: { publicId: t.user.id } });
    for (const [t, name] of [[h, 'Chat Host'], [h2, 'Chat Host 2']]) {
      await M.User.update({ name, role: 'host', hostStatus: 'approved' }, { where: { publicId: t.user.id } });
      const x = await M.User.findOne({ where: { publicId: t.user.id } });
      await M.HostProfile.findOrCreate({ where: { userId: x.id }, defaults: { callRate: 60, messagePrice: 30 } });
    }
    await M.User.update({ role: 'admin' }, { where: { publicId: ad.user.id } });
    h = await login(H); h2 = await login(H2); ad = await login(AD);
    hu = await M.User.findOne({ where: { publicId: h.user.id } }); uu = await M.User.findOne({ where: { publicId: u.user.id } });
    await M.Wallet.update({ coins: 1000 }, { where: { userId: uu.id } });
    us = await connect(u.accessToken); hs = await connect(h.accessToken);
    assert.ok(us && hs);
  });

  await check("first messages free, then the host's price; host earns its share", async () => {
    const live = once(hs, 'chat_message');
    const a = await send(u, h, 'Hi there');
    assert.equal(a.status, 200, JSON.stringify(a.json));
    assert.equal(a.json.message.coins, 0); assert.equal(a.json.freeLeft, 1);
    const ev = await live;
    assert.ok(ev && ev.message.text === 'Hi there' && ev.message.mine === false && ev.peerId === u.user.id, 'the host gets it live');
    assert.equal((await send(u, h, 'second')).json.freeLeft, 0);
    const e0 = await earnings();
    const c = await send(u, h, 'third is paid');
    assert.equal(c.json.message.coins, 30);
    assert.equal(c.json.coins, 970);
    assert.equal(await earnings(), e0 + Math.floor(30 * Number(await settings.get('host_share_pct')) / 100));
  });

  await check('the same message sent twice (bad network) is saved and charged once', async () => {
    const id = cid();
    const a = await send(u, h, 'again', id); const b = await send(u, h, 'again', id);
    assert.equal(a.json.message.id, b.json.message.id);
    assert.equal(b.json.coins, a.json.coins);
  });

  await check('not enough coins: refused, nothing saved', async () => {
    await M.Wallet.update({ coins: 5 }, { where: { userId: uu.id } });
    const before = await M.Message.count();
    const r = await send(u, h, 'cannot pay');
    assert.equal(r.json.error.code, 'not_enough_coins');
    assert.equal(await M.Message.count(), before);
    await M.Wallet.update({ coins: 1000 }, { where: { userId: uu.id } });
  });

  await check('host replies are free; a host may start a chat with a user, up to the daily limit', async () => {
    const r = await send(h, u, 'Hello from host');
    assert.equal(r.status, 200); assert.equal(r.json.message.coins, 0); assert.equal(r.json.freeLeft, null);
    await settings.update('host_new_chats_per_day', 1, null);
    assert.equal((await send(h, u2, 'Hi new user')).status, 200, 'first new chat today');
    const u3 = await login('6000000126');
    await M.User.update({ name: 'U3' }, { where: { publicId: u3.user.id } });
    assert.equal((await send(h, u3, 'one more')).json.error.code, 'daily_chat_limit');
    assert.equal((await send(h, u, 'replies are never limited')).status, 200);
    await kit.cleanup(['6000000126']);
  });

  await check('never host-to-host, never user-to-user, never when blocked', async () => {
    assert.equal((await send(h, h2, 'host to host')).status, 404);
    assert.equal((await send(u, u2, 'user to user')).status, 404);
    await call('POST', `/blocks/${u.user.id}`, { token: h2.accessToken });
    assert.equal((await send(u, h2, 'blocked')).status, 404);
    await call('DELETE', `/blocks/${u.user.id}`, { token: h2.accessToken });
  });

  await check('chat list with last message and unread; reading clears it and tells the sender', async () => {
    const l = await call('GET', '/chats', { token: h.accessToken });
    const row = l.json.chats.find((c) => c.peer.id === u.user.id);
    assert.ok(row && row.unread >= 4, JSON.stringify(row && row.unread));
    assert.equal((await call('GET', '/chats/unread', { token: h.accessToken })).json.unread, l.json.unreadTotal);
    const readEv = once(us, 'chat_read');
    await call('POST', `/chats/${u.user.id}/read`, { token: h.accessToken });
    assert.ok(await readEv, 'the user is told their messages were seen');
    const m = await call('GET', `/chats/${h.user.id}/messages`, { token: u.accessToken });
    assert.ok(m.json.messages.filter((x) => x.mine).every((x) => x.read));
    assert.equal(m.json.freeLeft, 0); assert.equal(m.json.messagePrice, 30);
    assert.equal((await call('GET', '/chats', { token: h.accessToken })).json.chats.find((c) => c.peer.id === u.user.id).unread, 0);
  });

  await check('typing is passed on only between people who may talk', async () => {
    const t = once(hs, 'chat_typing');
    us.emit('chat:typing', { peerId: h.user.id });
    assert.equal((await t).peerId, u.user.id);
  });

  await check('gifts: catalogue from the server; sending charges, earns, lands in the chat and plays on both apps', async () => {
    gifts = (await call('GET', '/gifts', { token: u.accessToken })).json.gifts;
    assert.ok(gifts.length >= 1 && gifts[0].price > 0);
    const g = gifts[0];
    const c0 = await coins(u); const e0 = await earnings();
    const hostSees = once(hs, 'gift'); const userSees = once(us, 'gift');
    const r = await call('POST', '/gifts/send', { token: u.accessToken, body: { toId: h.user.id, giftId: g.id, context: 'profile', clientId: cid() } });
    assert.equal(r.status, 200, JSON.stringify(r.json));
    assert.equal(r.json.coins, c0 - g.price);
    assert.equal(await earnings(), e0 + Math.floor(g.price * Number(await settings.get('host_share_pct')) / 100));
    const [a, b] = [await hostSees, await userSees];
    assert.ok(a && b && a.id === b.id && a.gift.id === g.id && a.from.id === u.user.id);
    const last = (await call('GET', `/chats/${u.user.id}/messages`, { token: h.accessToken })).json.messages.pop();
    assert.equal(last.kind, 'gift'); assert.equal(last.gift.id, g.id);
    const hist = (await call('GET', '/wallet/history', { token: u.accessToken })).json.items[0];
    assert.equal(hist.kind, 'gift_spend'); assert.ok(hist.title.includes(g.name) && hist.title.includes('Chat Host'), hist.title);
  });

  await check('several of one gift at once: one payment, one animation', async () => {
    const g = gifts[0]; const c0 = await coins(u);
    const r = await call('POST', '/gifts/send', { token: u.accessToken, body: { toId: h.user.id, giftId: g.id, quantity: 5, context: 'call', clientId: cid() } });
    assert.equal(r.json.coins, c0 - g.price * 5); assert.equal(r.json.gift.quantity, 5); assert.equal(r.json.message.quantity, 5);
    assert.equal((await call('POST', '/gifts/send', { token: u.accessToken, body: { toId: h.user.id, giftId: g.id, quantity: 500, context: 'call', clientId: cid() } })).status, 400);
  });

  await check('hosts cannot send gifts; a gift sent twice is charged once', async () => {
    assert.equal((await call('POST', '/gifts/send', { token: h.accessToken, body: { toId: u.user.id, giftId: gifts[0].id, context: 'chat', clientId: cid() } })).json.error.code, 'hosts_cannot_send_gifts');
    const id = cid();
    const a = await call('POST', '/gifts/send', { token: u.accessToken, body: { toId: h.user.id, giftId: gifts[0].id, context: 'chat', clientId: id } });
    const b = await call('POST', '/gifts/send', { token: u.accessToken, body: { toId: h.user.id, giftId: gifts[0].id, context: 'chat', clientId: id } });
    assert.equal(b.json.alreadySent, true); assert.equal(b.json.coins, a.json.coins);
  });

  let newGift;
  await check('admin gifts: picture + animation of any kind, checked by content', async () => {
    const lottie = Buffer.from(JSON.stringify({ v: '5.7.0', fr: 30, ip: 0, op: 60, w: 100, h: 100, layers: [] }));
    const f = new FormData();
    f.append('name', 'Test Star'); f.append('price', '77'); f.append('category', 'Test');
    f.append('thumb', new Blob([png(9)], { type: 'image/png' }), 't.png');
    f.append('anim', new Blob([lottie], { type: 'application/json' }), 'a.json');
    assert.equal((await call('POST', '/admin/gifts', { token: u.accessToken, form: f })).status, 403);
    const r = await call('POST', '/admin/gifts', { token: ad.accessToken, form: f });
    assert.equal(r.status, 200, JSON.stringify(r.json));
    newGift = r.json.gift;
    assert.equal(newGift.animType, 'lottie'); assert.ok(newGift.thumbUrl && newGift.animUrl);
    const file = await fetch(newGift.animUrl.replace(/^https?:\/\/[^/]+/, `http://127.0.0.1:${require('../realtime').port()}`));
    assert.equal(file.status, 200, 'the animation is served');
    const bad = new FormData(); bad.append('anim', new Blob([Buffer.from('not an animation at all')]), 'x.mp4');
    assert.equal((await call('PUT', `/admin/gifts/${newGift.id}`, { token: ad.accessToken, form: bad })).json.error.code, 'bad_animation');
    const up = new FormData(); up.append('price', '88'); up.append('active', 'false');
    assert.equal((await call('PUT', `/admin/gifts/${newGift.id}`, { token: ad.accessToken, form: up })).json.gift.price, 88);
    assert.ok(!(await call('GET', '/gifts', { token: u.accessToken })).json.gifts.some((x) => x.id === newGift.id), 'inactive gift hidden');
    assert.equal((await call('POST', '/gifts/send', { token: u.accessToken, body: { toId: h.user.id, giftId: newGift.id, context: 'chat', clientId: cid() } })).status, 404);
  });

  us.close(); hs.close();
  if (newGift) {
    const g = await M.Gift.findByPk(newGift.id);
    require('../utils/files').remove(g.thumbPath); require('../utils/files').remove(g.animPath);
    await g.destroy();
  }
  const ids = (await M.User.findAll({ where: { phone: [U, U2, H, H2, AD].map((p) => '+91' + p) } })).map((x) => x.id);
  const convs = await M.Conversation.findAll({ where: { [require('sequelize').Op.or]: [{ userId: ids }, { hostId: ids }] } });
  await M.Message.destroy({ where: { conversationId: convs.map((c) => c.id) } });
  await M.Conversation.destroy({ where: { id: convs.map((c) => c.id) } });
  await settings.update('free_messages_per_chat', saved.free, null);
  await settings.update('host_new_chats_per_day', saved.daily, null);
  await kit.cleanup([U, U2, H, H2, AD]);
  await kit.stop();
  console.log(`\n${passed} passed, ${failed} failed`);
  process.exit(failed ? 1 : 0);
})().catch((e) => { console.error('crashed:', e); process.exit(1); });
