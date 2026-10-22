// Withdrawals in beans (host) + the admin queue + the bell list. Run: npm run test:payouts
const assert = require('assert');
const kit = require('./testKit');
const settings = require('../services/settings');
const { call, login, M } = kit;
const H = '6000000141'; const AD = '6000000142';
let passed = 0; let failed = 0;
async function check(name, fn) { try { await fn(); passed++; console.log('PASS', name); } catch (e) { failed++; console.log('FAIL', name, '->', e.message); } }

(async () => {
  await kit.start();
  await kit.cleanup([H, AD]);
  const saved = { per: await settings.get('coins_per_bean'), min: await settings.get('withdraw_min_beans') };
  await settings.update('coins_per_bean', 2, null); await settings.update('withdraw_min_beans', 100, null);
  let h; let ad; let hu;
  await check('setup', async () => {
    h = await login(H); ad = await login(AD);
    await M.User.update({ role: 'host', hostStatus: 'approved', name: 'Pay Host' }, { where: { publicId: h.user.id } });
    await M.User.update({ role: 'admin' }, { where: { publicId: ad.user.id } });
    h = await login(H); ad = await login(AD);
    hu = await M.User.findOne({ where: { publicId: h.user.id } });
    await M.Wallet.update({ earnings: 1000 }, { where: { userId: hu.id } });
  });
  await check('withdraw page shows beans only (earnings / coins_per_bean); UPI needed first', async () => {
    const p = (await call('GET', '/host/payout', { token: h.accessToken })).json;
    assert.equal(p.beans, 500); assert.equal(p.minBeans, 100); assert.equal(p.upi, null);
    assert.equal((await call('POST', '/host/withdrawals', { token: h.accessToken, body: { beans: 200 } })).json.error.code, 'no_upi');
    assert.equal((await call('PUT', '/host/payout', { token: h.accessToken, body: { upi: 'bad', name: 'Pay Host' } })).status, 400);
    const s = (await call('PUT', '/host/payout', { token: h.accessToken, body: { upi: 'payhost@okaxis', name: 'Pay Host' } })).json;
    assert.ok(s.upi.endsWith('@okaxis') && !s.upi.includes('payhost'), 'masked');
  });
  await check('request: minimum, balance, one at a time; coins are held', async () => {
    assert.equal((await call('POST', '/host/withdrawals', { token: h.accessToken, body: { beans: 50 } })).json.error.code, 'below_minimum');
    assert.equal((await call('POST', '/host/withdrawals', { token: h.accessToken, body: { beans: 600 } })).json.error.code, 'not_enough');
    const r = (await call('POST', '/host/withdrawals', { token: h.accessToken, body: { beans: 200 } })).json;
    assert.equal(r.withdrawal.status, 'pending'); assert.equal(r.beans, 300);
    assert.equal(Number((await M.Wallet.findByPk(hu.id)).earnings), 600);
    assert.equal((await call('POST', '/host/withdrawals', { token: h.accessToken, body: { beans: 100 } })).status, 409);
  });
  await check('admin: reject returns the coins and notifies; paid notifies; users cannot see the queue', async () => {
    assert.equal((await call('GET', '/admin/withdrawals', { token: h.accessToken })).status, 403);
    const q = (await call('GET', '/admin/withdrawals', { token: ad.accessToken })).json.withdrawals;
    const mine = q.find((w) => w.host && w.host.id === h.user.id);
    assert.equal(mine.upi, 'payhost@okaxis'); assert.equal(mine.beans, 200);
    await call('POST', `/admin/withdrawals/${mine.id}/reject`, { token: ad.accessToken, body: { note: 'UPI name mismatch' } });
    assert.equal(Number((await M.Wallet.findByPk(hu.id)).earnings), 1000);
    const again = (await call('POST', '/host/withdrawals', { token: h.accessToken, body: { beans: 100 } })).json.withdrawal;
    assert.equal((await call('POST', `/admin/withdrawals/${again.id}/paid`, { token: ad.accessToken, body: {} })).status, 200);
    assert.equal((await call('POST', `/admin/withdrawals/${again.id}/paid`, { token: ad.accessToken, body: {} })).status, 409);
    const n = (await call('GET', '/notifications', { token: h.accessToken })).json;
    assert.ok(n.unread >= 2 && n.items.some((x) => x.kind === 'withdraw_paid') && n.items.some((x) => x.kind === 'withdraw_rejected'));
    await call('POST', '/notifications/read', { token: h.accessToken });
    assert.equal((await call('GET', '/notifications/unread', { token: h.accessToken })).json.unread, 0);
  });
  await check('support: send a request, the admin answers, the reply shows and arrives as a notification', async () => {
    assert.equal((await call('POST', '/support', { token: h.accessToken, body: { topic: 'Payment', text: 'hi' } })).status, 400, 'too short');
    const t = (await call('POST', '/support', { token: h.accessToken, body: { topic: 'Payment', text: 'My coins did not arrive after paying' } })).json.ticket;
    assert.equal(t.status, 'open');
    assert.equal((await call('GET', '/admin/support', { token: h.accessToken })).status, 403);
    const q = (await call('GET', '/admin/support', { token: ad.accessToken })).json.tickets;
    assert.ok(q.some((x) => x.id === t.id && x.from && x.from.id === h.user.id));
    await call('POST', `/admin/support/${t.id}/reply`, { token: ad.accessToken, body: { text: 'Added now, sorry for the wait' } });
    const mine = (await call('GET', '/support', { token: h.accessToken })).json.tickets.find((x) => x.id === t.id);
    assert.equal(mine.reply, 'Added now, sorry for the wait'); assert.equal(mine.newReply, true);
    await call('POST', `/support/${t.id}/seen`, { token: h.accessToken });
    assert.equal((await call('GET', '/support', { token: h.accessToken })).json.tickets.find((x) => x.id === t.id).newReply, false);
    assert.ok((await call('GET', '/notifications', { token: h.accessToken })).json.items.some((x) => x.kind === 'support'));
  });

  await settings.update('coins_per_bean', saved.per, null); await settings.update('withdraw_min_beans', saved.min, null);
  await kit.cleanup([H, AD]);
  await kit.stop();
  console.log(`\n${passed} passed, ${failed} failed`);
  process.exit(failed ? 1 : 0);
})().catch((e) => { console.error('crashed:', e); process.exit(1); });
