// Phase 2, part 2: becoming a host (application, private files, admin approve / reject, online switch). Run: npm run test:p2host
const assert = require('assert');
const kit = require('./testKit');
const { call, login, png, mp4, upload, M } = kit;

const APPLICANT = '6000000071';
const ADMIN = '6000000072';
const OTHER = '6000000073';
let passed = 0; let failed = 0;
async function check(name, fn) { try { await fn(); passed++; console.log('PASS', name); } catch (e) { failed++; console.log('FAIL', name, '->', e.message); } }
const img = (buf) => { const f = new FormData(); f.append('photo', new Blob([buf], { type: 'image/png' }), 'x.png'); return f; };

(async () => {
  await kit.start();
  await kit.cleanup([APPLICANT, ADMIN, OTHER]);
  let A; let AD; let O;

  await check('setup', async () => {
    A = await login(APPLICANT); AD = await login(ADMIN); O = await login(OTHER);
    await M.User.update({ role: 'admin' }, { where: { publicId: AD.user.id } });
    AD = await login(ADMIN); // fresh token carries the admin role
  });

  await check('a new account has an empty application and a list of what is missing', async () => {
    const r = await call('GET', '/host/application', { token: A.accessToken });
    assert.equal(r.status, 200);
    assert.equal(r.json.application.status, 'none');
    assert.equal(r.json.application.canSubmit, false);
    assert.ok(r.json.application.missing.length >= 6);
    assert.equal(r.json.application.requirements.minPhotos, 3);
  });

  await check('submit refused while incomplete', async () => {
    const r = await call('POST', '/host/application/submit', { token: A.accessToken });
    assert.equal(r.status, 400);
    assert.equal(r.json.error.code, 'application_incomplete');
  });

  await check('fields: validation (PAN format, languages, id type, unknown fields)', async () => {
    const put = (body) => call('PUT', '/host/application', { token: A.accessToken, body });
    assert.equal((await put({ panNumber: 'ABC123' })).status, 400);
    assert.equal((await put({ languages: ['Klingon'] })).json.error.code, 'invalid_language');
    assert.equal((await put({ idType: 'library_card' })).json.error.code, 'invalid_id_type');
    assert.equal((await put({ role: 'admin' })).status, 400, 'cannot send fields that are not part of the form');
    const ok = await put({ bio: 'I love music and talking', languages: ['Hindi', 'English'], panName: 'Test Applicant', panNumber: 'abcde1234f', idType: 'aadhaar', agencyCode: 'ag12' });
    assert.equal(ok.status, 200, JSON.stringify(ok.json));
    assert.equal(ok.json.application.panMasked, 'ABCDE****F', 'PAN comes back masked');
    assert.equal(ok.json.application.agencyCode, 'AG12');
  });

  await check('ID number: checked per document, Aadhaar stored only as last 4 digits', async () => {
    const put = (body) => call('PUT', '/host/application', { token: A.accessToken, body });
    assert.equal((await put({ idNumber: '1234 5678 9012' })).json.error.code, 'invalid_id_number', 'not a valid Aadhaar (checksum)');
    assert.equal((await put({ idNumber: 'abc' })).json.error.code, 'invalid_id_number');
    const ok = await put({ idNumber: '2345 6789 0124' });
    assert.equal(ok.status, 200, JSON.stringify(ok.json));
    assert.equal(ok.json.application.idNumberMasked, 'XXXX XXXX 0124');
    const row = await M.HostApplication.findByPk((await M.User.findOne({ where: { publicId: A.user.id } })).id);
    assert.ok(!require('../utils/pii').decrypt(row.idNumberEnc).includes('23456789'), 'the full Aadhaar never reaches the database');
    const pass = await put({ idType: 'passport' });
    assert.equal(pass.json.application.idNumberMasked, null, 'a new document type needs its own number');
    assert.equal((await put({ idNumber: 'K1234567' })).json.application.idNumberMasked, 'K1****67');
    assert.equal((await put({ idType: 'aadhaar', idNumber: '234567890124' })).status, 200);
  });

  await check('PAN is encrypted in the database', async () => {
    const row = await M.HostApplication.findByPk((await M.User.findOne({ where: { publicId: A.user.id } })).id);
    assert.ok(row.panEnc && !row.panEnc.includes('ABCDE'), 'no plain PAN in the table');
  });

  await check('selfie and ID photo: real images only', async () => {
    const bad = new FormData(); bad.append('photo', new Blob([Buffer.from('not an image')], { type: 'image/png' }), 'x.png');
    assert.equal((await call('POST', '/host/application/selfie', { token: A.accessToken, form: bad })).json.error.code, 'not_an_image');
    assert.equal((await call('POST', '/host/application/selfie', { token: A.accessToken, form: img(png(1)) })).status, 200);
    const id = await call('POST', '/host/application/id', { token: A.accessToken, form: img(png(2)) });
    assert.equal(id.status, 200);
    assert.equal(id.json.application.hasSelfie, true);
    assert.equal(id.json.application.hasId, true);
  });

  await check('photos and a video are added through the media route, then it can be submitted', async () => {
    for (let i = 1; i <= 3; i++) assert.equal((await upload(A.accessToken, png(10 + i), 'p.png')).status, 200);
    assert.equal((await upload(A.accessToken, mp4(15), 'v.mp4', 'video/mp4')).status, 200);
    const g = await call('GET', '/host/application', { token: A.accessToken });
    assert.deepStrictEqual(g.json.application.missing, []);
    const s = await call('POST', '/host/application/submit', { token: A.accessToken });
    assert.equal(s.status, 200, JSON.stringify(s.json));
    assert.equal(s.json.application.status, 'pending');
  });

  await check('after submitting the form is locked', async () => {
    assert.equal((await call('PUT', '/host/application', { token: A.accessToken, body: { bio: 'changed my mind...' } })).status, 403);
    assert.equal((await call('POST', '/host/application/submit', { token: A.accessToken })).status, 403);
  });

  await check('admin routes: only an admin gets in', async () => {
    assert.equal((await call('GET', '/admin/applications', { token: O.accessToken })).status, 403);
    assert.equal((await call('GET', '/admin/applications')).status, 401);
    const l = await call('GET', '/admin/applications', { token: AD.accessToken });
    assert.equal(l.status, 200);
    assert.ok(l.json.applications.some((x) => x.userId === A.user.id));
  });

  await check('admin sees the full application and the private files', async () => {
    const d = await call('GET', `/admin/applications/${A.user.id}`, { token: AD.accessToken });
    assert.equal(d.status, 200);
    assert.equal(d.json.application.pan, 'ABCDE1234F');
    assert.equal(d.json.application.idNumber, 'XXXX XXXX 0124');
    assert.equal(d.json.media.length, 4);
    const f = await call('GET', d.json.application.selfieUrl, { token: AD.accessToken });
    assert.equal(f.status, 200);
    assert.equal((await call('GET', d.json.application.selfieUrl, { token: O.accessToken })).status, 403, 'nobody else can open the selfie');
    assert.equal((await call('GET', d.json.application.selfieUrl)).status, 401);
  });

  await check('reject needs a reason; the applicant sees it and can apply again', async () => {
    assert.equal((await call('POST', `/admin/applications/${A.user.id}/reject`, { token: AD.accessToken, body: {} })).status, 400);
    assert.equal((await call('POST', `/admin/applications/${A.user.id}/reject`, { token: AD.accessToken, body: { reason: 'Selfie is not clear' } })).status, 200);
    const g = await call('GET', '/host/application', { token: A.accessToken });
    assert.equal(g.json.application.status, 'rejected');
    assert.equal(g.json.application.rejectReason, 'Selfie is not clear');
    assert.equal((await call('POST', '/host/application/selfie', { token: A.accessToken, form: img(png(5)) })).status, 200, 'rejected applicants can fix and resubmit');
    assert.equal((await call('POST', '/host/application/submit', { token: A.accessToken })).status, 200);
    assert.equal((await call('POST', `/admin/applications/${A.user.id}/reject`, { token: AD.accessToken, body: { reason: 'again' } })).status, 200);
    assert.equal((await call('POST', `/admin/applications/${A.user.id}/reject`, { token: AD.accessToken, body: { reason: 'twice' } })).status, 409, 'a decision is made once');
    await call('POST', '/host/application/submit', { token: A.accessToken });
  });

  await check('approve: the person becomes a host with the default rates and the main photo as DP', async () => {
    const r = await call('POST', `/admin/applications/${A.user.id}/approve`, { token: AD.accessToken });
    assert.equal(r.status, 200, JSON.stringify(r.json));
    const me = await call('GET', '/me', { token: A.accessToken });
    assert.equal(me.json.user.role, 'host');
    assert.equal(me.json.user.hostStatus, 'approved');
    assert.ok(me.json.user.dp, 'DP from the main photo');
    assert.deepStrictEqual(me.json.user.languages, ['Hindi', 'English']);
    const rates = await call('GET', '/host/rates', { token: A.accessToken });
    assert.equal(rates.json.callRate, 60);
  });

  await check('an approved host cannot send a new application', async () => {
    assert.equal((await call('PUT', '/host/application', { token: A.accessToken, body: { bio: 'new bio here again' } })).status, 403);
  });

  await check('there is no online switch any more: the phone cannot set it', async () => {
    assert.equal((await call('PUT', '/host/online', { token: A.accessToken, body: { online: true } })).status, 404);
  });

  await kit.cleanup([APPLICANT, ADMIN, OTHER]);
  await kit.stop();
  console.log(`\n${passed} passed, ${failed} failed`);
  process.exit(failed ? 1 : 0);
})().catch((e) => { console.error('crashed:', e); process.exit(1); });
