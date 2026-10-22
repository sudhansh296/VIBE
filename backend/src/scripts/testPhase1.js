// End-to-end check of Phase 1 against the real local database. Starts the API on a free port, runs every flow,
// prints PASS / FAIL for each check and removes the test accounts at the end. Run: npm run test:p1
process.env.LOGIN_RATE_LIMIT = '1000'; // the run logs in many times; the real limit is checked separately below
const assert = require('assert');
const { sequelize, User, Wallet, Transaction, Session, Block, Report, UserSetting, HostProfile } = require('../models');
const { seedSettings } = require('../services/settings');
const { canInteract } = require('../services/rules');
const app = require('../app');

// numbers that are never used as Firebase test numbers or real logins (the cleanup deletes these accounts)
const PHONE_A = '6000000091';
const PHONE_B = '6000000092';
let base;
const testDeletedIds = []; // accounts this run deleted (their phone is gone, so remember them by id)
let passed = 0;
let failed = 0;

async function call(method, path, { token, body, form } = {}) {
  const headers = {};
  if (token) headers.Authorization = `Bearer ${token}`;
  let payload;
  if (form) payload = form;
  else if (body !== undefined) { headers['Content-Type'] = 'application/json'; payload = JSON.stringify(body); }
  const res = await fetch(base + path, { method, headers, body: payload });
  let json = null;
  try { json = await res.json(); } catch { /* empty */ }
  return { status: res.status, json };
}

async function check(name, fn) {
  try { await fn(); passed++; console.log('PASS', name); } catch (e) { failed++; console.log('FAIL', name, '->', e.message); }
}

async function login(phone) {
  await call('POST', '/auth/dev/request-otp', { body: { phone } });
  return call('POST', '/auth/dev/verify-otp', { body: { phone, code: '123456' } });
}

async function cleanup() {
  const users = await User.findAll({ where: { phone: ['+91' + PHONE_A, '+91' + PHONE_B] } });
  const ids = users.map((u) => u.id);
  ids.push(...testDeletedIds);
  if (!ids.length) return;
  await Promise.all([
    Wallet.destroy({ where: { userId: ids } }), Transaction.destroy({ where: { userId: ids } }), Session.destroy({ where: { userId: ids } }),
    Block.destroy({ where: { blockerId: ids } }), Block.destroy({ where: { blockedId: ids } }),
    Report.destroy({ where: { reporterId: ids } }), Report.destroy({ where: { targetId: ids } }),
    UserSetting.destroy({ where: { userId: ids } }), HostProfile.destroy({ where: { userId: ids } }),
  ]);
  // remove the test users' uploaded photos too
  const fs = require('fs'); const path = require('path'); const dir = path.join(__dirname, '..', '..', 'uploads', 'dp');
  if (fs.existsSync(dir)) for (const f of fs.readdirSync(dir)) if (ids.some((id) => f.startsWith(`${id}_`))) fs.rmSync(path.join(dir, f), { force: true });
  await User.destroy({ where: { id: ids } });
}

(async () => {
  await sequelize.sync();
  await seedSettings();
  await cleanup();
  const server = app.listen(0);
  base = `http://127.0.0.1:${server.address().port}`;

  let A; let B;

  await check('health + public config', async () => {
    assert.equal((await call('GET', '/health')).status, 200);
    const c = await call('GET', '/config');
    assert.strictEqual(c.json.config.min_age, 18);
    assert.ok(Array.isArray(c.json.config.allowed_languages), 'settings must come back as real JSON');
    assert.ok(!('signup_bonus_coins' in c.json.config), 'private setting must not leak');
  });

  await check('protected routes need a token', async () => {
    assert.equal((await call('GET', '/me')).status, 401);
    assert.equal((await call('GET', '/me', { token: 'garbage' })).status, 401);
  });

  await check('bad phone is rejected', async () => {
    const r = await call('POST', '/auth/dev/request-otp', { body: { phone: '12345' } });
    assert.equal(r.status, 400);
  });

  await check('wrong OTP is rejected', async () => {
    await call('POST', '/auth/dev/request-otp', { body: { phone: PHONE_A } });
    const r = await call('POST', '/auth/dev/verify-otp', { body: { phone: PHONE_A, code: '000000' } });
    assert.equal(r.status, 400);
    assert.equal(r.json.error.code, 'otp_wrong');
  });

  await check('new user: login, wallet with signup bonus from settings', async () => {
    const r = await login(PHONE_A);
    assert.equal(r.status, 200, JSON.stringify(r.json));
    assert.equal(r.json.isNewUser, true);
    assert.equal(r.json.user.coins, 100);
    assert.equal(String(r.json.user.id).length, 8);
    assert.equal(r.json.user.profileComplete, false);
    A = r.json;
    const tx = await Transaction.findAll({ where: { kind: 'signup_bonus' }, include: [] });
    assert.ok(tx.length >= 1);
  });

  await check('same phone again = same account, no second bonus', async () => {
    const r = await login(PHONE_A);
    assert.equal(r.json.isNewUser, false);
    assert.equal(r.json.user.id, A.user.id);
    assert.equal(r.json.user.coins, 100);
    A.accessToken = r.json.accessToken; A.refreshToken = r.json.refreshToken;
  });

  await check('profile: under 18 is refused', async () => {
    const y = new Date().getUTCFullYear() - 16;
    const r = await call('PUT', '/me/profile', { token: A.accessToken, body: { dob: `${y}-01-01` } });
    assert.equal(r.status, 403);
    assert.equal(r.json.error.code, 'under_age');
  });

  await check('profile: unknown field / bad language refused', async () => {
    assert.equal((await call('PUT', '/me/profile', { token: A.accessToken, body: { coins: 999999 } })).status, 400);
    assert.equal((await call('PUT', '/me/profile', { token: A.accessToken, body: { languages: ['Klingon'] } })).status, 400);
    assert.equal((await call('PUT', '/me/profile', { token: A.accessToken, body: { languages: ['Hin'] } })).status, 400, 'part of a name must not pass');
  });

  await check('profile: valid save', async () => {
    const r = await call('PUT', '/me/profile', { token: A.accessToken, body: { name: 'Test A', gender: 'male', dob: '2000-03-14', city: 'Delhi', bio: 'Chai and cricket', languages: ['Hindi', 'English'] } });
    assert.equal(r.status, 200, JSON.stringify(r.json));
    assert.equal(r.json.user.profileComplete, true);
    assert.deepStrictEqual(r.json.user.languages, ['Hindi', 'English']);
    assert.equal(r.json.user.age >= 26, true);
  });

  await check('profile: gender and dob are locked once set', async () => {
    assert.equal((await call('PUT', '/me/profile', { token: A.accessToken, body: { gender: 'female' } })).json.error.code, 'gender_locked');
    assert.equal((await call('PUT', '/me/profile', { token: A.accessToken, body: { dob: '1999-01-01' } })).json.error.code, 'dob_locked');
  });

  await check('DP: real PNG accepted, fake image refused', async () => {
    const png = Buffer.from('89504E470D0A1A0A0000000D49484452000000010000000108060000001F15C4890000000D4944415478DA63F8CFC0F01F0005000201E2216BC80000000049454E44AE426082', 'hex');
    const f1 = new FormData(); f1.append('photo', new Blob([png], { type: 'image/png' }), 'a.png');
    const ok = await call('POST', '/me/dp', { token: A.accessToken, form: f1 });
    assert.equal(ok.status, 200, JSON.stringify(ok.json));
    assert.ok(ok.json.user.dp.includes('/uploads/dp/'));
    const img = await fetch(ok.json.user.dp.replace(/^https?:\/\/[^/]+/, base));
    assert.equal(img.status, 200);
    const f2 = new FormData(); f2.append('photo', new Blob([Buffer.from('<script>alert(1)</script>')], { type: 'image/png' }), 'x.png');
    const no = await call('POST', '/me/dp', { token: A.accessToken, form: f2 });
    assert.equal(no.status, 400);
    assert.equal(no.json.error.code, 'not_an_image');
  });

  await check('block / unblock another user', async () => {
    B = (await login(PHONE_B)).json;
    assert.equal((await call('POST', `/blocks/${B.user.id}`, { token: A.accessToken })).status, 200);
    const list = await call('GET', '/blocks', { token: A.accessToken });
    assert.equal(list.json.blocks.length, 1);
    assert.equal(list.json.blocks[0].id, B.user.id);
    assert.equal(list.json.blocks[0].phone, undefined, 'other user phone must not leak');
    assert.equal((await call('POST', `/blocks/${A.user.id}`, { token: A.accessToken })).status, 400);
    const ua = await User.findOne({ where: { publicId: A.user.id } });
    const ub = await User.findOne({ where: { publicId: B.user.id } });
    assert.equal((await canInteract(ua, ub)).reason, 'blocked');
    assert.equal((await call('DELETE', `/blocks/${B.user.id}`, { token: A.accessToken })).status, 200);
    assert.equal((await canInteract(ua, ub)).ok, true);
  });

  await check('rules: host cannot interact with host, user <-> host is fine', async () => {
    const ua = await User.findOne({ where: { publicId: A.user.id } });
    const ub = await User.findOne({ where: { publicId: B.user.id } });
    await ub.update({ role: 'host' });
    assert.equal((await canInteract(ua, ub)).ok, true);
    await ua.update({ role: 'host' });
    assert.equal((await canInteract(ua, ub)).reason, 'host_to_host');
    await ua.update({ role: 'user' }); await ub.update({ role: 'user' });
  });

  await check('report: bad reason refused, good one saved once', async () => {
    assert.equal((await call('POST', '/reports', { token: A.accessToken, body: { targetId: B.user.id, reason: 'boring' } })).status, 400);
    assert.equal((await call('POST', '/reports', { token: A.accessToken, body: { targetId: B.user.id, reason: 'spam', detail: 'test' } })).status, 200);
    await call('POST', '/reports', { token: A.accessToken, body: { targetId: B.user.id, reason: 'spam' } });
    const ua = await User.findOne({ where: { publicId: A.user.id } });
    assert.equal(await Report.count({ where: { reporterId: ua.id } }), 1);
  });

  await check('settings: notifications default on, can be switched, nothing else accepted', async () => {
    const g = await call('GET', '/me/settings', { token: A.accessToken });
    assert.deepStrictEqual(g.json.settings, { notifyMessages: true, notifyMissedCalls: true, notifyOffers: true });
    const p = await call('PUT', '/me/settings', { token: A.accessToken, body: { notifyOffers: false } });
    assert.equal(p.json.settings.notifyOffers, false);
    assert.equal((await call('GET', '/me/settings', { token: A.accessToken })).json.settings.notifyOffers, false);
    assert.equal((await call('PUT', '/me/settings', { token: A.accessToken, body: { showOnline: false } })).status, 400, 'privacy switches do not exist');
    assert.equal((await call('PUT', '/me/settings', { token: A.accessToken, body: { notifyMessages: 'no' } })).status, 400);
  });

  await check('host rates: users cannot, hosts can only inside the admin range', async () => {
    assert.equal((await call('GET', '/host/rates', { token: A.accessToken })).status, 403, 'a user is not a host');
    const ua = await User.findOne({ where: { publicId: A.user.id } });
    await ua.update({ role: 'host' });
    const g = await call('GET', '/host/rates', { token: A.accessToken });
    assert.equal(g.status, 200);
    assert.equal(g.json.callRate, 60, 'new host gets the default from settings');
    assert.equal(g.json.limits.callRateMin, 40);
    const ok = await call('PUT', '/host/rates', { token: A.accessToken, body: { callRate: 75, messagePrice: 150 } });
    assert.equal(ok.status, 200);
    assert.equal(ok.json.callRate, 75, 'any amount inside the range, not only the preset chips');
    assert.equal((await call('PUT', '/host/rates', { token: A.accessToken, body: { callRate: 39 } })).json.error.code, 'rate_out_of_range');
    assert.equal((await call('PUT', '/host/rates', { token: A.accessToken, body: { callRate: 401 } })).json.error.code, 'rate_out_of_range');
    assert.equal((await call('PUT', '/host/rates', { token: A.accessToken, body: { messagePrice: 20 } })).json.error.code, 'price_out_of_range');
    assert.equal((await call('PUT', '/host/rates', { token: A.accessToken, body: { callRate: 75.5 } })).status, 400, 'whole coins only');
    await ua.update({ role: 'user' });
  });

  await check('a refresh retried right away (its answer was lost) gets a fresh pair; the lost pair stops working', async () => {
    const fresh = (await login(PHONE_A)).json;
    const first = await call('POST', '/auth/refresh', { body: { refreshToken: fresh.refreshToken } });   // "lost" answer
    assert.equal(first.status, 200);
    const retry = await call('POST', '/auth/refresh', { body: { refreshToken: fresh.refreshToken } });
    assert.equal(retry.status, 200, 'a quick retry is not theft');
    assert.equal((await call('POST', '/auth/refresh', { body: { refreshToken: first.json.refreshToken } })).status, 401, 'the lost pair is cancelled');
    const next = await call('POST', '/auth/refresh', { body: { refreshToken: retry.json.refreshToken } });
    assert.equal(next.status, 200);
    A = { ...A, accessToken: next.json.accessToken, refreshToken: next.json.refreshToken };
  });

  await check('refresh token rotates; reuse of an old one logs everything out', async () => {
    process.env.REFRESH_REUSE_GRACE_MS = '0';   // a late reuse (after the retry window) is theft
    const r1 = await call('POST', '/auth/refresh', { body: { refreshToken: A.refreshToken } });
    assert.equal(r1.status, 200);
    const reuse = await call('POST', '/auth/refresh', { body: { refreshToken: A.refreshToken } });
    assert.equal(reuse.status, 401);
    assert.equal(reuse.json.error.code, 'refresh_reused');
    const r2 = await call('POST', '/auth/refresh', { body: { refreshToken: r1.json.refreshToken } });
    assert.equal(r2.status, 401, 'after reuse every session must be revoked');
    delete process.env.REFRESH_REUSE_GRACE_MS;
  });

  await check('log out of other devices: the other phone is out at once, this one stays', async () => {
    const one = (await login(PHONE_B)).json;
    const two = (await login(PHONE_B)).json;
    assert.equal((await call('GET', '/me', { token: two.accessToken })).status, 200);
    const r = await call('POST', '/auth/logout-others', { token: one.accessToken });
    assert.equal(r.status, 200);
    assert.ok(r.json.loggedOut >= 1);
    assert.equal((await call('GET', '/me', { token: two.accessToken })).status, 401, 'other phone access token stops at once');
    const rr = await call('POST', '/auth/refresh', { body: { refreshToken: two.refreshToken } });
    assert.equal(rr.status, 401, 'other phone cannot refresh');
    assert.equal(rr.json.error.code, 'session_ended', 'a normal logout is not treated as theft');
    const still = await call('GET', '/me', { token: one.accessToken });
    assert.equal(still.status, 200, 'this phone stays logged in: ' + JSON.stringify(still.json));
    assert.equal((await call('POST', '/auth/logout-others', { token: two.accessToken })).status, 401, 'the logged-out phone cannot log others out');
  });

  await check('sign out ends the access token too, not only the refresh token', async () => {
    const r = (await login(PHONE_B)).json;
    await call('POST', '/auth/logout', { body: { refreshToken: r.refreshToken } });
    assert.equal((await call('GET', '/me', { token: r.accessToken })).status, 401);
  });

  await check('logout revokes the refresh token', async () => {
    const r = await login(PHONE_B);
    await call('POST', '/auth/logout', { body: { refreshToken: r.json.refreshToken } });
    assert.equal((await call('POST', '/auth/refresh', { body: { refreshToken: r.json.refreshToken } })).status, 401);
  });

  await check('delete account: token stops working, phone can sign up fresh', async () => {
    const r = await login(PHONE_A);
    const before = await User.findOne({ where: { publicId: r.json.user.id } });
    const dpFile = before.dpPath;
    assert.equal((await call('DELETE', '/me', { token: r.json.accessToken })).status, 200);
    testDeletedIds.push(before.id);
    const after = await User.findByPk(before.id);
    assert.equal(after.status, 'deleted');
    assert.equal(after.name, null, 'name must be removed');
    assert.equal(after.dpPath, null, 'photo must be removed');
    if (dpFile) { await new Promise((ok) => setTimeout(ok, 200)); assert.ok(!require('fs').existsSync(require('path').join(__dirname, '..', '..', 'uploads', dpFile)), 'photo file must be deleted'); }
    assert.equal((await call('GET', '/me', { token: r.json.accessToken })).status, 401);
    const again = await login(PHONE_A);
    assert.equal(again.json.isNewUser, true);
  });

  await check('blocked account is locked out', async () => {
    const r = await login(PHONE_B);
    await User.update({ status: 'blocked' }, { where: { publicId: r.json.user.id } });
    const me = await call('GET', '/me', { token: r.json.accessToken });
    assert.equal(me.status, 403);
    assert.equal(me.json.error.code, 'account_blocked');
  });

  await check('unknown route = 404 json', async () => {
    const r = await call('GET', '/nope');
    assert.equal(r.status, 404);
    assert.equal(r.json.error.code, 'not_found');
  });

  await cleanup();
  server.close();
  await sequelize.close();
  console.log(`\n${passed} passed, ${failed} failed`);
  process.exit(failed ? 1 : 0);
})().catch(async (e) => { console.error('Test run crashed:', e); process.exit(1); });
