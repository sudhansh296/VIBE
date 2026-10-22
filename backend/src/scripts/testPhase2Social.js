// Phase 2, part 3: home list with filters, profile, follow, visitors, and the "who sees whom" rules. Run: npm run test:p2social
const assert = require('assert');
const kit = require('./testKit');
const { call, login, M } = kit;

const U1 = '6000000061'; const U2 = '6000000062'; // two normal users
const H1 = '6000000063'; const H2 = '6000000064'; const H3 = '6000000065'; // three hosts
let passed = 0; let failed = 0;
async function check(name, fn) { try { await fn(); passed++; console.log('PASS', name); } catch (e) { failed++; console.log('FAIL', name, '->', e.message); } }
const ids = (r) => r.json.people.map((p) => p.id);

(async () => {
  await kit.start();
  await kit.cleanup([U1, U2, H1, H2, H3]);
  let u1; let u2; let h1; let h2; let h3; let rows = {};

  await check('setup: 2 users, 3 approved hosts with different details', async () => {
    u1 = await login(U1); u2 = await login(U2); h1 = await login(H1); h2 = await login(H2); h3 = await login(H3);
    const mk = async (login, data, rate, online) => {
      const row = await M.User.findOne({ where: { publicId: login.user.id } });
      await row.update({ role: 'host', hostStatus: 'approved', ...data });
      await M.HostProfile.create({ userId: row.id, callRate: rate, messagePrice: 200, isOnline: online, onlineAt: online ? new Date() : null });
      await M.HostApplication.create({ userId: row.id, reviewedAt: data.fresh ? new Date() : new Date(Date.now() - 30 * 86_400_000) });
      return row;
    };
    const year = new Date().getUTCFullYear();
    rows.h1 = await mk(h1, { name: 'Anaya', gender: 'female', dob: `${year - 23}-01-01`, city: 'Mumbai', languages: ['Hindi', 'English'], fresh: false }, 60, true);
    rows.h2 = await mk(h2, { name: 'Bina', gender: 'female', dob: `${year - 30}-01-01`, city: 'Delhi', languages: ['Punjabi'], fresh: false }, 120, false);
    rows.h3 = await mk(h3, { name: 'Carl', gender: 'male', dob: `${year - 27}-01-01`, city: 'Pune', languages: ['English', 'Marathi'], fresh: true }, 40, true);
    for (const [lg, n, g] of [[u1, 'Uma', 'female'], [u2, 'Vik', 'male']]) {
      await M.User.update({ name: n, gender: g, dob: `${year - 25}-05-05`, city: 'Goa' }, { where: { publicId: lg.user.id } });
    }
    rows.u1 = await M.User.findOne({ where: { publicId: u1.user.id } }); rows.u2 = await M.User.findOne({ where: { publicId: u2.user.id } });
  });

  await check('a user sees only approved hosts, online ones first', async () => {
    const r = await call('GET', '/discover', { token: u1.accessToken });
    assert.equal(r.status, 200);
    const mine = ids(r).filter((x) => [h1.user.id, h2.user.id, h3.user.id].includes(x));
    assert.equal(mine.length, 3);
    assert.ok(r.json.people.every((p) => p.role === 'host'), 'only hosts');
    assert.ok(!ids(r).includes(u1.user.id) && !ids(r).includes(u2.user.id), 'no normal users in a user list');
    assert.equal(r.json.people.find((p) => p.id === h2.user.id).online, false);
    assert.equal(r.json.people.find((p) => p.id === h1.user.id).callRate, 60);
    const order = r.json.people.map((p) => p.id);
    assert.ok(order.indexOf(h2.user.id) > order.indexOf(h1.user.id), 'the offline host is after the online one');
  });

  await check('a host sees users, never other hosts', async () => {
    const r = await call('GET', '/discover', { token: h1.accessToken });
    assert.equal(r.status, 200);
    assert.ok(r.json.people.every((p) => p.role === 'user'));
    assert.ok(ids(r).includes(u1.user.id) && ids(r).includes(u2.user.id));
    assert.ok(!ids(r).includes(h2.user.id) && !ids(r).includes(h3.user.id));
  });

  await check('a not-yet-approved applicant is invisible', async () => {
    await rows.h2.update({ hostStatus: 'pending' });
    assert.ok(!ids(await call('GET', '/discover', { token: u1.accessToken })).includes(h2.user.id));
    await rows.h2.update({ hostStatus: 'approved' });
  });

  await check('filters: gender, online, price, age, language, search', async () => {
    const q = async (s) => ids(await call('GET', `/discover?${s}`, { token: u1.accessToken })).filter((x) => [h1.user.id, h2.user.id, h3.user.id].includes(x));
    assert.deepStrictEqual((await q('gender=male')), [h3.user.id]);
    assert.deepStrictEqual((await q('online=1')).sort(), [h1.user.id, h3.user.id].sort());
    assert.deepStrictEqual((await q('maxPrice=60')).sort(), [h1.user.id, h3.user.id].sort());
    assert.deepStrictEqual((await q('ageMin=25&ageMax=40')).sort(), [h2.user.id, h3.user.id].sort());
    assert.deepStrictEqual((await q('languages=Punjabi')), [h2.user.id]);
    assert.deepStrictEqual((await q('languages=English,Marathi')), [h3.user.id], 'needs ALL the chosen languages');
    assert.deepStrictEqual((await q('q=mumb')), [h1.user.id], 'search by city');
    assert.deepStrictEqual((await q('q=bin')), [h2.user.id], 'search by name');
    assert.equal((await call('GET', '/discover?ageMin=5', { token: u1.accessToken })).status, 400, 'bad filter value');
    assert.equal((await call('GET', '/discover?evil=1', { token: u1.accessToken })).status, 400, 'unknown filter');
  });

  await check('"New" tab shows only hosts approved in the last 7 days', async () => {
    const r = await call('GET', '/discover?tab=new', { token: u1.accessToken });
    const mine = ids(r).filter((x) => [h1.user.id, h2.user.id, h3.user.id].includes(x));
    assert.deepStrictEqual(mine, [h3.user.id]);
    assert.equal(r.json.people.find((p) => p.id === h3.user.id).isNew, true);
  });

  await check('profile: shows the host, files with lock state, and counts as a visit (once a day)', async () => {
    const r = await call('GET', `/profile/${h1.user.id}`, { token: u1.accessToken });
    assert.equal(r.status, 200);
    assert.equal(r.json.person.name, 'Anaya');
    assert.ok(r.json.media && Array.isArray(r.json.media.photos));
    await call('GET', `/profile/${h1.user.id}`, { token: u1.accessToken });
    assert.equal(await M.Visit.count({ where: { visitedId: rows.h1.id, visitorId: rows.u1.id } }), 1, 'same day = one visit');
  });

  await check('who can look at whom: host-to-host and user-to-user are "not found"', async () => {
    assert.equal((await call('GET', `/profile/${h2.user.id}`, { token: h1.accessToken })).status, 404);
    assert.equal((await call('GET', `/profile/${u2.user.id}`, { token: u1.accessToken })).status, 404, 'user-to-user is not a thing either');
  });

  await check('follow / unfollow, counts, and the Following tab', async () => {
    const f = await call('POST', `/follow/${h1.user.id}`, { token: u1.accessToken });
    assert.equal(f.json.followers, 1);
    await call('POST', `/follow/${h1.user.id}`, { token: u1.accessToken });
    assert.equal((await call('POST', `/follow/${h1.user.id}`, { token: u2.accessToken })).json.followers, 2, 'a second follower; the first is not counted twice');
    const tab = await call('GET', '/discover?tab=following', { token: u1.accessToken });
    assert.deepStrictEqual(ids(tab), [h1.user.id]);
    assert.equal(tab.json.people[0].following, true);
    const mine = await call('GET', '/me/following', { token: u1.accessToken });
    assert.deepStrictEqual(ids(mine), [h1.user.id]);
    const fans = await call('GET', '/me/followers', { token: h1.accessToken });
    assert.equal(fans.json.people.length, 2);
    assert.equal((await call('DELETE', `/follow/${h1.user.id}`, { token: u2.accessToken })).json.followers, 1);
    assert.equal((await call('POST', `/follow/${h2.user.id}`, { token: h1.accessToken })).status, 404, 'a host cannot follow a host');
  });

  await check('visitors: the host sees who looked at the profile (nobody is hidden)', async () => {
    await call('GET', `/profile/${h1.user.id}`, { token: u2.accessToken });
    const r = await call('GET', '/me/visitors', { token: h1.accessToken });
    assert.equal(r.status, 200);
    assert.equal(r.json.total, 2);
    assert.deepStrictEqual(r.json.visitors.map((v) => v.name).sort(), ['Uma', 'Vik']);
    assert.ok(r.json.visitors[0].visitedAt);
    assert.equal(r.json.visitors[0].phone, undefined, 'a visitor phone number never leaks');
  });

  await check('a host can open a user profile (and so shows up in that user\'s visitors)', async () => {
    assert.equal((await call('GET', `/profile/${u1.user.id}`, { token: h1.accessToken })).status, 200);
    assert.equal((await call('GET', '/me/visitors', { token: u1.accessToken })).json.locked, true, 'not VIP: only the count');
    await M.User.update({ vipUntil: new Date(Date.now() + 86_400_000) }, { where: { id: rows.u1.id } });   // the full list is a VIP benefit
    const r = await call('GET', '/me/visitors', { token: u1.accessToken });
    assert.ok(r.json.visitors.some((v) => v.name === 'Anaya'));
  });

  await check('blocking removes people from lists, profiles, follows and visitors both ways', async () => {
    await M.Block.create({ blockerId: rows.u1.id, blockedId: rows.h1.id });
    assert.ok(!ids(await call('GET', '/discover', { token: u1.accessToken })).includes(h1.user.id), 'gone from the blocker\'s list');
    assert.ok(!ids(await call('GET', '/discover', { token: h1.accessToken })).includes(u1.user.id), 'and the blocker is gone from the host\'s list');
    assert.equal((await call('GET', `/profile/${h1.user.id}`, { token: u1.accessToken })).status, 404);
    assert.equal((await call('GET', `/profile/${u1.user.id}`, { token: h1.accessToken })).status, 404);
    assert.ok(!(await call('GET', '/me/visitors', { token: h1.accessToken })).json.visitors.some((v) => v.name === 'Uma'));
    assert.ok(!ids(await call('GET', '/me/following', { token: u1.accessToken })).includes(h1.user.id));
    await M.Block.destroy({ where: { blockerId: rows.u1.id } });
  });

  await check('pages: 20 per page with a next page marker', async () => {
    const r = await call('GET', '/discover?page=1', { token: u1.accessToken });
    assert.ok(r.json.people.length <= 20);
    assert.ok(r.json.nextPage === null || r.json.nextPage === 2);
    assert.equal((await call('GET', '/discover?page=0', { token: u1.accessToken })).status, 400);
  });

  await check('Me page numbers come from the server', async () => {
    const r = await call('GET', '/me/counts', { token: u1.accessToken });
    assert.equal(r.status, 200, JSON.stringify(r.json));
    const list = await call('GET', '/me/following', { token: u1.accessToken });
    assert.equal(r.json.following, list.json.people.length);
    const hv = await call('GET', '/me/counts', { token: h1.accessToken });
    const vis = await call('GET', '/me/visitors', { token: h1.accessToken });
    assert.equal(hv.json.visitors, vis.json.total);
  });

  await kit.cleanup([U1, U2, H1, H2, H3]);
  await kit.stop();
  console.log(`\n${passed} passed, ${failed} failed`);
  process.exit(failed ? 1 : 0);
})().catch((e) => { console.error('crashed:', e); process.exit(1); });
