// Status (stories): hosts post photo/video, users see them (not blocked, no host-to-host), views, expiry, delete.
// Run: npm run test:stories
const assert = require('assert');
const kit = require('./testKit');
const { call, login, png, mp4, M } = kit;
const H = '6000000161'; const H2 = '6000000162'; const U = '6000000163'; const U2 = '6000000164';
let passed = 0; let failed = 0;
async function check(name, fn) { try { await fn(); passed++; console.log('PASS', name); } catch (e) { failed++; console.log('FAIL', name, '->', e.message); } }
const post = (token, buf, name, type, caption) => {
  const f = new FormData(); f.append('file', new Blob([buf], { type }), name); if (caption) f.append('caption', caption);
  return call('POST', '/stories', { token, form: f });
};

(async () => {
  await kit.start();
  await kit.cleanup([H, H2, U, U2]);
  let h; let h2; let u; let u2; let photoId; let videoId;
  await check('setup', async () => {
    h = await login(H); h2 = await login(H2); u = await login(U); u2 = await login(U2);
    for (const [t, n] of [[h, 'Story Host'], [h2, 'Story Host 2']]) await M.User.update({ name: n, role: 'host', hostStatus: 'approved' }, { where: { publicId: t.user.id } });
    await M.User.update({ name: 'Story User' }, { where: { publicId: u.user.id } });
    h = await login(H); h2 = await login(H2);
  });

  await check('only hosts post; photo + video (length limit) + caption', async () => {
    assert.equal((await post(u.accessToken, png(), 'a.png', 'image/png')).status, 403);
    const p = (await post(h.accessToken, png(), 'a.png', 'image/png', 'Hello from Delhi')).json.story;
    assert.equal(p.kind, 'photo'); assert.equal(p.caption, 'Hello from Delhi'); photoId = p.id;
    assert.equal((await post(h.accessToken, mp4(31), 'v.mp4', 'video/mp4')).json.error.code, 'video_too_long');
    const v = (await post(h.accessToken, mp4(8), 'v.mp4', 'video/mp4')).json.story;
    assert.equal(v.kind, 'video'); assert.equal(v.durationSec, 8); videoId = v.id;
  });

  await check('users see hosts\' statuses, unseen first; hosts never see other hosts\'; the file is protected', async () => {
    const list = (await call('GET', '/stories', { token: u.accessToken })).json;
    const mine = list.people.find((x) => x.person.id === h.user.id);
    assert.ok(mine && mine.stories.length === 2 && !mine.allSeen); assert.equal(list.canPost, false);
    assert.ok(!(await call('GET', '/stories', { token: h2.accessToken })).json.people.some((x) => x.person.id === h.user.id), 'no host-to-host');
    assert.equal((await call('GET', `/stories/${photoId}/file`, { token: u.accessToken })).status, 200);
    assert.equal((await call('GET', `/stories/${photoId}/file`, { token: h2.accessToken })).status, 404);
  });

  await check('views: counted once per person, the poster sees who; blocked people see nothing', async () => {
    await call('POST', `/stories/${photoId}/view`, { token: u.accessToken });
    await call('POST', `/stories/${photoId}/view`, { token: u.accessToken });
    await call('POST', `/stories/${photoId}/view`, { token: h.accessToken });          // own view: not counted
    const mine = (await call('GET', '/stories/mine', { token: h.accessToken })).json.stories;
    assert.equal(mine.find((s) => s.id === photoId).views, 1);
    const who = (await call('GET', `/stories/${photoId}/viewers`, { token: h.accessToken })).json.viewers;
    assert.equal(who.length, 1); assert.equal(who[0].name, 'Story User');
    assert.equal((await call('GET', `/stories/${photoId}/viewers`, { token: u.accessToken })).status, 404, 'only the poster');
    const seen = (await call('GET', '/stories', { token: u.accessToken })).json.people.find((x) => x.person.id === h.user.id);
    assert.equal(seen.stories.find((s) => s.id === photoId).seen, true);
    const [hu, u2u] = [await M.User.findOne({ where: { publicId: h.user.id } }), await M.User.findOne({ where: { publicId: u2.user.id } })];
    await M.Block.create({ blockerId: u2u.id, blockedId: hu.id });
    assert.ok(!(await call('GET', '/stories', { token: u2.accessToken })).json.people.some((x) => x.person.id === h.user.id));
    assert.equal((await call('GET', `/stories/${videoId}/file`, { token: u2.accessToken })).status, 404);
  });

  await check('expired statuses disappear; delete removes one', async () => {
    await M.Story.update({ expiresAt: new Date(Date.now() - 1000) }, { where: { id: videoId } });
    const list = (await call('GET', '/stories', { token: u.accessToken })).json.people.find((x) => x.person.id === h.user.id);
    assert.equal(list.stories.length, 1);
    assert.equal((await call('GET', `/stories/${videoId}/file`, { token: u.accessToken })).status, 404);
    assert.equal((await call('DELETE', `/stories/${photoId}`, { token: u.accessToken })).status, 404, 'not mine');
    assert.equal((await call('DELETE', `/stories/${photoId}`, { token: h.accessToken })).status, 200);
    assert.equal((await call('GET', '/stories/mine', { token: h.accessToken })).json.stories.length, 0);
  });

  await kit.cleanup([H, H2, U, U2]);
  await kit.stop();
  console.log(`\n${passed} passed, ${failed} failed`);
  process.exit(failed ? 1 : 0);
})().catch((e) => { console.error('crashed:', e); process.exit(1); });
