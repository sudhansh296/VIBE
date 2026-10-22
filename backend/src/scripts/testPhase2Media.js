// Phase 2, part 1: photos and videos (limits, free / locked, main photo = DP, private files, paid unlock). Run: npm run test:p2media
const assert = require('assert');
const kit = require('./testKit');
const { call, login, png, mp4, upload, M } = kit;

const HOST = '6000000081';
const VIEWER = '6000000082';
let passed = 0; let failed = 0;
async function check(name, fn) { try { await fn(); passed++; console.log('PASS', name); } catch (e) { failed++; console.log('FAIL', name, '->', e.message); } }

(async () => {
  await kit.start();
  await kit.cleanup([HOST, VIEWER]);
  let H; let V; let host; let viewer; let photos = []; let videos = [];

  await check('setup: two accounts', async () => {
    H = await login(HOST); V = await login(VIEWER);
    host = await M.User.findOne({ where: { publicId: H.user.id } });
    viewer = await M.User.findOne({ where: { publicId: V.user.id } });
    assert.ok(host && viewer);
  });

  await check('photo: first one becomes the main photo and the public DP', async () => {
    const r = await upload(H.accessToken, png(1), 'a.png');
    assert.equal(r.status, 200, JSON.stringify(r.json));
    assert.equal(r.json.photos.length, 1);
    assert.equal(r.json.photos[0].isMain, true);
    const me = await call('GET', '/me', { token: H.accessToken });
    assert.ok(me.json.user.dp && me.json.user.dp.includes('/uploads/dp/'), 'DP set from the main photo');
  });

  await check('photo: fake image and wrong type refused', async () => {
    assert.equal((await upload(H.accessToken, Buffer.from('<script>alert(1)</script>'), 'x.png', 'image/png')).json.error.code, 'unsupported_file');
  });

  await check('video: length is read on the server; over 30 s refused, bad file refused, 12 s accepted', async () => {
    assert.equal((await upload(H.accessToken, mp4(31), 'v.mp4', 'video/mp4')).json.error.code, 'video_too_long');
    assert.equal((await upload(H.accessToken, Buffer.concat([Buffer.from([0, 0, 0, 16]), Buffer.from('ftypisom'), Buffer.alloc(8)]), 'v.mp4', 'video/mp4')).json.error.code, 'unreadable_video');
    const ok = await upload(H.accessToken, mp4(12), 'v.mp4', 'video/mp4');
    assert.equal(ok.status, 200, JSON.stringify(ok.json));
    assert.equal(ok.json.videos[0].durationSec, 12);
    assert.equal((await upload(H.accessToken, mp4(30), 'v2.mp4', 'video/mp4')).status, 200, 'exactly 30 s is fine');
  });

  await check('limits: at most 6 photos', async () => {
    for (let i = 2; i <= 6; i++) assert.equal((await upload(H.accessToken, png(i), 'p.png')).status, 200);
    const seventh = await upload(H.accessToken, png(7), 'p.png');
    assert.equal(seventh.status, 400);
    assert.equal(seventh.json.error.code, 'limit_reached');
    const g = await call('GET', '/me/media', { token: H.accessToken });
    photos = g.json.photos; videos = g.json.videos;
    assert.equal(photos.length, 6); assert.equal(videos.length, 2);
  });

  await check('lock rules: first 3 photos and first video can never be locked; others can', async () => {
    assert.equal((await call('PUT', `/me/media/${photos[0].id}`, { token: H.accessToken, body: { locked: true } })).json.error.code, 'always_free');
    assert.equal((await call('PUT', `/me/media/${photos[2].id}`, { token: H.accessToken, body: { locked: true } })).status, 403);
    assert.equal((await call('PUT', `/me/media/${videos[0].id}`, { token: H.accessToken, body: { locked: true } })).status, 403);
    const l = await call('PUT', `/me/media/${photos[3].id}`, { token: H.accessToken, body: { locked: true } });
    assert.equal(l.status, 200);
    assert.equal(l.json.photos[3].lockedByHost, true);
    assert.equal(l.json.photos[3].price, 200);
    assert.equal((await call('PUT', `/me/media/${videos[1].id}`, { token: H.accessToken, body: { locked: true } })).status, 200);
  });

  await check('a stranger cannot see an applicant\'s files (host not approved yet)', async () => {
    assert.equal((await call('GET', `/media/${photos[0].id}/file`, { token: V.accessToken })).status, 404);
  });

  await check('after approval: free files open, locked files answer 403 with the price', async () => {
    await host.update({ role: 'host', hostStatus: 'approved' });
    const free = await call('GET', `/media/${photos[0].id}/file`, { token: V.accessToken });
    assert.equal(free.status, 200);
    assert.ok(free.type.includes('image/png'));
    const locked = await call('GET', `/media/${photos[3].id}/file`, { token: V.accessToken });
    assert.equal(locked.status, 403);
    assert.equal(locked.json.error.code, 'locked');
    assert.equal((await call('GET', `/media/${photos[3].id}/file`)).status, 401, 'no token, no file');
    assert.equal((await call('GET', `/media/${photos[3].id}/file`, { token: H.accessToken })).status, 200, 'the owner always can');
  });

  await check('unlock: not enough coins -> 402, nothing charged', async () => {
    await M.Wallet.update({ coins: 50 }, { where: { userId: viewer.id } });
    const r = await call('POST', `/media/${photos[3].id}/unlock`, { token: V.accessToken });
    assert.equal(r.status, 402);
    assert.equal(r.json.error.code, 'not_enough_coins');
    assert.equal(Number((await M.Wallet.findByPk(viewer.id)).coins), 50);
  });

  await check('unlock: charged once, host earns the admin share, ledger has both rows', async () => {
    await M.Wallet.update({ coins: 1000 }, { where: { userId: viewer.id } });
    const r = await call('POST', `/media/${photos[3].id}/unlock`, { token: V.accessToken });
    assert.equal(r.status, 200, JSON.stringify(r.json));
    assert.equal(r.json.coins, 800);
    assert.equal(Number((await M.Wallet.findByPk(host.id)).earnings), 120, '60% of 200');
    const rows = await M.Transaction.findAll({ where: { kind: ['media_unlock', 'media_earn'], userId: [viewer.id, host.id] } });
    assert.equal(rows.length, 2);
    assert.equal(rows.find((x) => x.kind === 'media_earn').wallet, 'earnings');
    assert.equal((await call('GET', `/media/${photos[3].id}/file`, { token: V.accessToken })).status, 200, 'now it opens');
  });

  await check('unlock again (and twice at the same moment): no second charge', async () => {
    const again = await call('POST', `/media/${photos[3].id}/unlock`, { token: V.accessToken });
    assert.equal(again.json.alreadyOpen, true);
    const [a, b] = await Promise.all([call('POST', `/media/${videos[1].id}/unlock`, { token: V.accessToken }), call('POST', `/media/${videos[1].id}/unlock`, { token: V.accessToken })]);
    assert.ok(a.status === 200 && b.status === 200);
    assert.equal(Number((await M.Wallet.findByPk(viewer.id)).coins), 800 - 500, 'the video was charged exactly once');
    assert.equal(await M.MediaUnlock.count({ where: { userId: viewer.id, mediaId: videos[1].id } }), 1);
  });

  await check('blocked viewer cannot open anything', async () => {
    await M.Block.create({ blockerId: host.id, blockedId: viewer.id });
    assert.equal((await call('GET', `/media/${photos[0].id}/file`, { token: V.accessToken })).status, 404);
    await M.Block.destroy({ where: { blockerId: host.id } });
  });

  await check('main photo: switching it changes the DP; a locked photo cannot be main', async () => {
    const before = (await call('GET', '/me', { token: H.accessToken })).json.user.dp;
    const r = await call('POST', `/me/media/${photos[1].id}/main`, { token: H.accessToken });
    assert.equal(r.status, 200, JSON.stringify(r.json));
    const g = await call('GET', '/me/media', { token: H.accessToken });
    assert.equal(g.json.photos[0].id, photos[1].id, 'the main photo is shown first');
    const after = (await call('GET', '/me', { token: H.accessToken })).json.user.dp;
    assert.notEqual(before, after);
    assert.equal((await call('POST', `/me/media/${photos[3].id}/main`, { token: H.accessToken })).json.error.code, 'locked_photo');
  });

  await check('a host cannot go below the minimum number of photos', async () => {
    const g = (await call('GET', '/me/media', { token: H.accessToken })).json;
    for (const p of g.photos.slice(3)) assert.equal((await call('DELETE', `/me/media/${p.id}`, { token: H.accessToken })).status, 200);
    const left = (await call('GET', '/me/media', { token: H.accessToken })).json.photos;
    assert.equal(left.length, 3);
    const r = await call('DELETE', `/me/media/${left[2].id}`, { token: H.accessToken });
    assert.equal(r.status, 403);
    assert.equal(r.json.error.code, 'minimum_media');
  });

  await check('deleting the main photo promotes the next one', async () => {
    const g = (await call('GET', '/me/media', { token: H.accessToken })).json;
    await upload(H.accessToken, png(9), 'n.png'); // 4 photos so a delete is allowed
    const main = g.photos[0];
    assert.equal((await call('DELETE', `/me/media/${main.id}`, { token: H.accessToken })).status, 200);
    const after = (await call('GET', '/me/media', { token: H.accessToken })).json;
    assert.equal(after.photos[0].isMain, true);
    assert.notEqual(after.photos[0].id, main.id);
  });

  await check('private files are NOT served as plain files', async () => {
    const r = await call('GET', `/uploads/media/${host.id}/${photos[0].id}.png`);
    assert.equal(r.status, 404);
  });

  await kit.cleanup([HOST, VIEWER]);
  await kit.stop();
  console.log(`\n${passed} passed, ${failed} failed`);
  process.exit(failed ? 1 : 0);
})().catch((e) => { console.error('crashed:', e); process.exit(1); });
