// The admin can change every important number; the change applies at once. Run: npm run test:admin
const assert = require('assert');
const kit = require('./testKit');
const { call, login, png, mp4, upload, M } = kit;
const settings = require('../services/settings');

const ADMIN = '6000000051'; const USER = '6000000052';
let passed = 0; let failed = 0;
async function check(name, fn) { try { await fn(); passed++; console.log('PASS', name); } catch (e) { failed++; console.log('FAIL', name, '->', e.message); } }

(async () => {
  await kit.start();
  await kit.cleanup([ADMIN, USER]);
  const original = Object.fromEntries((await M.Setting.findAll()).map((s) => [s.key, s.value]));
  let AD; let U;
  const put = (key, value, extra = {}) => call('PUT', `/admin/settings/${key}`, { token: AD.accessToken, body: { value, ...extra } });

  try {
    await check('setup', async () => {
      AD = await login(ADMIN); U = await login(USER);
      await M.User.update({ role: 'admin' }, { where: { publicId: AD.user.id } });
      AD = await login(ADMIN);
    });

    await check('only an admin can read or change settings', async () => {
      assert.equal((await call('GET', '/admin/settings', { token: U.accessToken })).status, 403);
      assert.equal((await call('PUT', '/admin/settings/min_age', { token: U.accessToken, body: { value: 18 } })).status, 403);
      assert.equal((await call('GET', '/admin/settings')).status, 401);
    });

    await check('the list shows every setting with its description and allowed range', async () => {
      const r = await call('GET', '/admin/settings', { token: AD.accessToken });
      assert.equal(r.status, 200);
      const keys = r.json.settings.map((s) => s.key);
      for (const k of ['media_max_video_seconds', 'media_max_videos', 'media_max_photos', 'media_price_photo', 'host_share_pct', 'host_call_rate_min', 'signup_bonus_coins']) assert.ok(keys.includes(k), k);
      const v = r.json.settings.find((s) => s.key === 'media_max_video_seconds');
      assert.equal(v.value, 30);
      assert.deepStrictEqual(v.range, [5, 300]);
      assert.ok(v.description);
    });

    await check('bad values are refused: wrong type, out of range, unknown key', async () => {
      assert.equal((await put('media_max_video_seconds', 'long')).json.error.code, 'invalid_value');
      assert.equal((await put('media_max_video_seconds', 2)).json.error.code, 'out_of_range');
      assert.equal((await put('media_max_video_seconds', 12.5)).json.error.code, 'invalid_value');
      assert.equal((await put('host_share_pct', 150)).json.error.code, 'out_of_range');
      assert.equal((await put('no_such_key', 1)).json.error.code, 'unknown_setting');
      assert.equal((await put('maintenance_mode', 'yes', { confirm: true })).json.error.code, 'invalid_value');
    });

    await check('settings that depend on each other cannot contradict', async () => {
      assert.equal((await put('host_call_rate_min', 500)).json.error.code, 'conflicts', 'lowest above highest');
      assert.equal((await put('media_free_photos', 10)).json.error.code, 'conflicts', 'more free than allowed');
      assert.equal((await put('host_call_rate_default', 5)).json.error.code, 'conflicts', 'start price below the lowest');
    });

    await check('video length: a new limit applies to the very next upload', async () => {
      assert.equal((await upload(U.accessToken, mp4(12), 'v.mp4', 'video/mp4')).status, 200, 'allowed at 30 s');
      assert.equal((await put('media_max_video_seconds', 10)).status, 200);
      const r = await upload(U.accessToken, mp4(12), 'w.mp4', 'video/mp4');
      assert.equal(r.status, 400);
      assert.equal(r.json.error.code, 'video_too_long');
      assert.ok(r.json.error.message.includes('10 seconds'), 'the message shows the new number');
      assert.equal((await put('media_max_video_seconds', 60)).status, 200);
      assert.equal((await upload(U.accessToken, mp4(45), 'x.mp4', 'video/mp4')).status, 200, 'now 45 s is fine');
    });

    await check('number of videos and photos: lowered, it applies at once', async () => {
      assert.equal((await put('media_max_photos', 2)).json.error.code, 'conflicts', 'free / needed photos are still 3');
      assert.equal((await put('media_free_photos', 1)).status, 200);
      assert.equal((await put('host_min_photos', 1)).status, 200);
      assert.equal((await put('media_max_photos', 2)).status, 200);
      assert.equal((await upload(U.accessToken, png(1), 'a.png')).status, 200);
      assert.equal((await upload(U.accessToken, png(2), 'b.png')).status, 200);
      assert.equal((await upload(U.accessToken, png(3), 'c.png')).json.error.code, 'limit_reached');
      assert.equal((await put('media_free_videos', 1)).status, 200);
      assert.equal((await put('media_max_videos', 2)).status, 200);
      assert.equal((await upload(U.accessToken, mp4(5), 'z.mp4', 'video/mp4')).json.error.code, 'limit_reached', 'already has 2 videos');
    });

    await check('prices and the host share: unlock uses the new numbers', async () => {
      const g = await call('GET', '/me/media', { token: U.accessToken });
      assert.equal(g.json.limits.maxPhotos, 2);
      assert.equal((await put('media_price_photo', 333)).status, 200);
      assert.equal((await call('GET', '/me/media', { token: U.accessToken })).json.limits.pricePhoto, 333);
    });

    await check('host price range: a new minimum rejects a price below it', async () => {
      const row = await M.User.findOne({ where: { publicId: U.user.id } });
      await row.update({ role: 'host', hostStatus: 'approved' });
      const H = await login(USER);
      assert.equal((await put('host_call_rate_min', 70)).json.error.code, 'conflicts', 'the starting price 60 would fall below 70');
      assert.equal((await put('host_call_rate_default', 80)).status, 200);
      assert.equal((await put('host_call_rate_min', 70)).status, 200);
      const r = await call('PUT', '/host/rates', { token: H.accessToken, body: { callRate: 60 } });
      assert.equal(r.json.error.code, 'rate_out_of_range');
      assert.ok(r.json.error.message.includes('between 70'), r.json.error.message);
      const ok = await call('PUT', '/host/rates', { token: H.accessToken, body: { callRate: 90 } });
      assert.equal(ok.status, 200);
      assert.equal((await call('GET', '/host/rates', { token: H.accessToken })).json.limits.callRateMin, 70);
    });

    await check('the app config only shows public settings and follows the change', async () => {
      const c = await call('GET', '/config');
      assert.equal(c.json.config.media_max_video_seconds, 60);
      assert.equal(c.json.config.media_max_photos, 2);
      assert.equal(c.json.config.signup_bonus_coins, undefined, 'private numbers never reach the app');
      assert.equal(c.json.config.host_share_pct, 60);
    });

    await check('dangerous settings need confirm: true, then work and can be switched back', async () => {
      assert.equal((await put('maintenance_mode', true)).json.error.code, 'confirm_needed');
      assert.equal((await put('min_app_version', '9.0.0')).json.error.code, 'confirm_needed');
      assert.equal((await put('min_app_version', 'new', { confirm: true })).json.error.code, 'invalid_value');
      assert.equal((await put('maintenance_mode', true, { confirm: true })).status, 200);
      assert.equal((await call('GET', '/config')).json.config.maintenance_mode, true);
      assert.equal((await put('maintenance_mode', false, { confirm: true })).status, 200);
      assert.equal((await call('GET', '/config')).json.config.maintenance_mode, false);
    });

    await check('who changed it is recorded', async () => {
      const row = await M.Setting.findByPk('media_price_photo');
      assert.equal(String(row.updatedBy), String((await M.User.findOne({ where: { publicId: AD.user.id } })).id));
    });
  } finally {
    // put every setting back exactly as it was before this run
    for (const [key, value] of Object.entries(original)) await M.Setting.update({ value }, { where: { key } });
    settings.clearCache();
    await kit.cleanup([ADMIN, USER]);
    await kit.stop();
  }
  console.log(`\n${passed} passed, ${failed} failed`);
  process.exit(failed ? 1 : 0);
})().catch((e) => { console.error('crashed:', e); process.exit(1); });
