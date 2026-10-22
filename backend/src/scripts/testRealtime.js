// Socket.IO presence: online while the app is connected, offline shortly after it is closed. Run: npm run test:realtime
const assert = require('assert');
const { io } = require('socket.io-client');
const kit = require('./testKit');
const { call, login, M } = kit;

const HOST = '6000000081';
const USER = '6000000082';
let passed = 0; let failed = 0;
async function check(name, fn) { try { await fn(); passed++; console.log('PASS', name); } catch (e) { failed++; console.log('FAIL', name, '->', e.message); } }
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const online = async (u) => (await M.HostProfile.findOne({ where: { userId: u.id } }))?.isOnline === true;
async function until(fn, ms = 3000) { const t = Date.now(); while (Date.now() - t < ms) { if (await fn()) return true; await sleep(50); } return false; }

(async () => {
  await kit.start();
  await kit.cleanup([HOST, USER]);
  const url = () => `http://127.0.0.1:${require('../realtime').port()}`;
  const connect = (token) => new Promise((resolve) => {
    const s = io(url(), { auth: { token }, transports: ['websocket'], reconnection: false });
    s.on('connect', () => resolve({ s, ok: true }));
    s.on('connect_error', (e) => resolve({ s, ok: false, msg: e.message }));
  });
  let H; let U; let hu;

  await check('setup: an approved host and a normal user', async () => {
    H = await login(HOST); U = await login(USER);
    hu = await M.User.findOne({ where: { publicId: H.user.id } });
    await hu.update({ role: 'host', hostStatus: 'approved' });
    await M.HostProfile.findOrCreate({ where: { userId: hu.id }, defaults: { callRate: 60, messagePrice: 200 } });
    await M.HostProfile.update({ isOnline: false }, { where: { userId: hu.id } });
    H = await login(HOST);
  });

  await check('no token / bad token is refused', async () => {
    assert.equal((await connect(undefined)).ok, false);
    const r = await connect('garbage'); assert.equal(r.ok, false); r.s.close();
  });

  let hs;
  await check('host opens the app -> online, others are told', async () => {
    const watcher = await connect(U.accessToken); assert.ok(watcher.ok);
    const got = new Promise((res) => watcher.s.on('presence', (e) => { if (e.id === H.user.id) res(e); }));   // users' presence is broadcast too
    const c = await connect(H.accessToken); assert.ok(c.ok); hs = c.s;
    const ev = await Promise.race([got, sleep(3000).then(() => null)]);
    assert.deepStrictEqual(ev, { id: H.user.id, online: true });
    assert.ok(await online(hu));
    await hu.update({ name: 'Realtime Host' });
    const d = await call('GET', '/discover?q=Realtime', { token: U.accessToken });
    assert.equal(d.status, 200, JSON.stringify(d.json));
    const row = (d.json.people || d.json.items || d.json.results || [])[0];
    assert.ok(row && row.online === true, 'Home list shows the host online: ' + JSON.stringify(d.json).slice(0, 200));
    watcher.s.close();
  });

  await check('second connection keeps host online when the first closes', async () => {
    const c2 = await connect(H.accessToken); assert.ok(c2.ok);
    hs.close(); await sleep(900);
    assert.ok(await online(hu));
    c2.s.close(); hs = null;
  });

  await check('app closed -> offline after the grace time', async () => {
    assert.ok(await until(async () => !(await online(hu)), 3000));
  });

  await check('quick reconnect does not flicker offline', async () => {
    const a = await connect(H.accessToken); assert.ok(await until(() => online(hu)));
    a.s.close(); await sleep(100);
    const b = await connect(H.accessToken); await sleep(900);
    assert.ok(await online(hu)); b.s.close();
    assert.ok(await until(async () => !(await online(hu))));
  });

  await check('a normal user connecting never becomes an online host', async () => {
    const c = await connect(U.accessToken); assert.ok(c.ok); await sleep(300);
    const u = await M.User.findOne({ where: { publicId: U.user.id } });
    assert.equal(await M.HostProfile.count({ where: { userId: u.id } }), 0);
    c.s.close();
  });

  await check('logging out ends the live connection and sets offline', async () => {
    const H2 = await login(HOST);
    const c = await connect(H2.accessToken); assert.ok(c.ok);
    const closed = new Promise((r) => c.s.once('disconnect', r));
    await call('POST', '/auth/logout', { token: H2.accessToken, body: { refreshToken: H2.refreshToken } });
    assert.ok(await Promise.race([closed.then(() => true), sleep(3000).then(() => false)]));
    assert.ok(await until(async () => !(await online(hu))));
  });

  await check('a revoked session cannot connect', async () => {
    const H3 = await login(HOST);
    await call('POST', '/auth/logout', { token: H3.accessToken, body: { refreshToken: H3.refreshToken } });
    assert.equal((await connect(H3.accessToken)).ok, false);
  });

  await kit.cleanup([HOST, USER]);
  await kit.stop();
  console.log(`\n${passed} passed, ${failed} failed`);
  process.exit(failed ? 1 : 0);
})().catch((e) => { console.error('crashed:', e); process.exit(1); });
