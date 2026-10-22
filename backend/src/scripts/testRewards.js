// Daily check-in, levels (XP -> level + coins), invites (paid after the friend's first purchase), VIP-only visitors.
// Run: npm run test:rewards
const assert = require('assert');
const kit = require('./testKit');
const { call, login, M } = kit;
const A = '6000000151'; const B = '6000000152'; const H = '6000000153';
let passed = 0; let failed = 0;
async function check(name, fn) { try { await fn(); passed++; console.log('PASS', name); } catch (e) { failed++; console.log('FAIL', name, '->', e.message); } }
const coinsOf = async (t) => Number((await M.Wallet.findByPk((await M.User.findOne({ where: { publicId: t.user.id } })).id)).coins);
const userOf = (t) => M.User.findOne({ where: { publicId: t.user.id } });
const dayIST = (d) => new Date(d.getTime() + 330 * 60_000).toISOString().slice(0, 10);

(async () => {
  await kit.start();
  await kit.cleanup([A, B, H]);
  let a; let b; let h;
  await check('setup', async () => {
    a = await login(A); b = await login(B); h = await login(H);
    await M.User.update({ name: 'Rew A' }, { where: { publicId: a.user.id } });
    await M.User.update({ name: 'Rew B' }, { where: { publicId: b.user.id } });
    await M.User.update({ name: 'Rew Host', role: 'host', hostStatus: 'approved' }, { where: { publicId: h.user.id } });
    h = await login(H);
  });

  await check('check-in: day 1 pays, twice a day refused, next day goes on, a missed day restarts, day 7 then a new round', async () => {
    const s = (await call('GET', '/checkin', { token: a.accessToken })).json;
    assert.deepEqual(s.rewards.length, 7); assert.equal(s.done, 0); assert.equal(s.collectedToday, false);
    const c0 = await coinsOf(a);
    const r1 = (await call('POST', '/checkin', { token: a.accessToken })).json;
    assert.equal(r1.day, 1); assert.equal(await coinsOf(a), c0 + s.rewards[0]);
    assert.equal((await call('POST', '/checkin', { token: a.accessToken })).status, 409);
    const u = await userOf(a);
    await u.update({ checkinLast: dayIST(new Date(Date.now() - 86_400_000)) });            // collected yesterday
    assert.equal((await call('POST', '/checkin', { token: a.accessToken })).json.day, 2);
    await u.reload(); await u.update({ checkinLast: dayIST(new Date(Date.now() - 3 * 86_400_000)) }); // missed days
    assert.equal((await call('POST', '/checkin', { token: a.accessToken })).json.day, 1, 'restarts');
    await u.reload(); await u.update({ checkinStreak: 6, checkinLast: dayIST(new Date(Date.now() - 86_400_000)) });
    const r7 = (await call('POST', '/checkin', { token: a.accessToken })).json;
    assert.equal(r7.day, 7); assert.equal(r7.coins, s.rewards[6]);
    await u.reload(); await u.update({ checkinLast: dayIST(new Date(Date.now() - 86_400_000)) });
    assert.equal((await call('POST', '/checkin', { token: a.accessToken })).json.day, 1, 'after day 7 a new round');
  });

  await check('levels: XP from check-in; crossing a level gives its coins once and a notice', async () => {
    const lv = (await call('GET', '/levels', { token: a.accessToken })).json;
    assert.ok(lv.rules.length >= 2 && lv.xp > 0, 'check-ins gave XP');
    const next = lv.rules.find((r) => r.level === lv.level + 1);
    const c0 = await coinsOf(a);
    await require('../services/levels').addXp((await userOf(a)).id, next.xp - lv.xp);
    const after = (await call('GET', '/levels', { token: a.accessToken })).json;
    assert.equal(after.level, next.level);
    assert.equal(await coinsOf(a), c0 + next.rewardCoins);
    const n = (await call('GET', '/notifications', { token: a.accessToken })).json.items;
    assert.ok(n.some((x) => x.kind === 'level' && x.title.includes(String(next.level))));
    assert.equal((await call('GET', `/profile/${a.user.id}`, { token: h.accessToken })).json.person.level, next.level);
  });

  await check('invite: code once, not own, before the first purchase; both paid on the friend\'s first purchase only', async () => {
    const inv = (await call('GET', '/invite', { token: a.accessToken })).json;
    assert.ok(/^[A-Z0-9]{6}$/.test(inv.code)); assert.equal(inv.rewardCoins, 100);
    assert.equal((await call('POST', '/invite/apply', { token: a.accessToken, body: { code: inv.code } })).json.error.code, 'own_code');
    assert.equal((await call('POST', '/invite/apply', { token: b.accessToken, body: { code: 'NOPE00' } })).status, 404);
    assert.equal((await call('POST', '/invite/apply', { token: b.accessToken, body: { code: inv.code } })).status, 200);
    assert.equal((await call('POST', '/invite/apply', { token: b.accessToken, body: { code: inv.code } })).json.error.code, 'already_used');
    const a0 = await coinsOf(a); const b0 = await coinsOf(b);
    const buy = async () => {
      const pack = (await call('GET', '/wallet/packs', { token: b.accessToken })).json.packs[0];
      const o = (await call('POST', '/wallet/orders', { token: b.accessToken, body: { packId: pack.id } })).json;
      const pay = (await call('POST', `/wallet/dev/pay/${o.orderId}`, { token: b.accessToken })).json;
      await call('POST', '/wallet/verify', { token: b.accessToken, body: { orderId: o.orderId, paymentId: pay.paymentId, signature: pay.signature } });
      return o.coins;
    };
    const got = await buy();
    assert.equal(await coinsOf(b), b0 + got + 100); assert.equal(await coinsOf(a), a0 + 100);
    const got2 = await buy();
    assert.equal(await coinsOf(a), a0 + 100, 'only the first purchase'); assert.equal(await coinsOf(b), b0 + got + got2 + 100);
    assert.equal((await call('GET', '/invite', { token: a.accessToken })).json.rewarded, 1);
  });

  await check('invite link: /invite gives <base>/i/<code>; the page opens the app with the code; a bad code is 404', async () => {
    const inv = (await call('GET', '/invite', { token: a.accessToken })).json;
    assert.ok(inv.link.endsWith(`/i/${inv.code}`), inv.link);
    const page = await call('GET', `/i/${inv.code.toLowerCase()}`);
    assert.equal(page.status, 200); assert.ok(page.type.includes('html'));
    const html = page.buffer.toString();
    assert.ok(html.includes(`intent://invite/${inv.code}#Intent;scheme=vibecallme;package=com.vibe.callme`));
    assert.ok(html.includes(encodeURIComponent(`invite=${inv.code}`)));
    assert.equal((await call('GET', '/i/<script>')).status, 404);
  });

  await check('visitors: users see only the count unless VIP; hosts always see who', async () => {
    await call('GET', `/profile/${h.user.id}`, { token: a.accessToken });      // a visits the host
    await call('GET', `/profile/${a.user.id}`, { token: h.accessToken });      // the host visits a
    const locked = (await call('GET', '/me/visitors', { token: a.accessToken })).json;
    assert.equal(locked.locked, true); assert.ok(locked.total >= 1); assert.equal(locked.visitors.length, 0);
    const hv = (await call('GET', '/me/visitors', { token: h.accessToken })).json;
    assert.equal(hv.locked, false); assert.ok(hv.visitors.length >= 1);
    await M.User.update({ vipUntil: new Date(Date.now() + 86_400_000) }, { where: { publicId: a.user.id } });
    const vip = (await call('GET', '/me/visitors', { token: a.accessToken })).json;
    assert.equal(vip.locked, false); assert.ok(vip.visitors.length >= 1);
  });

  await kit.cleanup([A, B, H]);
  await kit.stop();
  console.log(`\n${passed} passed, ${failed} failed`);
  process.exit(failed ? 1 : 0);
})().catch((e) => { console.error('crashed:', e); process.exit(1); });
