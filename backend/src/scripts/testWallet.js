// Phase 3: coin packs, buying coins (pretend Razorpay), webhook, ledger, admin packs, host earnings. Run: npm run test:wallet
const assert = require('assert');
const kit = require('./testKit');
const settings = require('../services/settings');
const razorpay = require('../services/razorpay');
const { call, login, M } = kit;

const BUYER = '6000000111'; const OTHER = '6000000112'; const ADMIN = '6000000113'; const HOST = '6000000114';
let passed = 0; let failed = 0;
async function check(name, fn) { try { await fn(); passed++; console.log('PASS', name); } catch (e) { failed++; console.log('FAIL', name, '->', e.message); } }

async function webhook(body, sig) {
  const raw = JSON.stringify(body);
  return call('POST', '/webhooks/razorpay', { raw, headers: { 'X-Razorpay-Signature': sig === undefined ? razorpay.fakeWebhookSignature(raw) : sig } });
}
const captured = (orderId, amount, id = `pay_wh_${Date.now()}`) => ({ event: 'payment.captured', payload: { payment: { entity: { id, order_id: orderId, amount, currency: 'INR', status: 'captured' } } } });

(async () => {
  await kit.start();
  await kit.cleanup([BUYER, OTHER, ADMIN, HOST]);
  let B; let O; let AD; let H; let packs;
  const coins = async (t) => (await call('GET', '/wallet', { token: t.accessToken })).json.coins;

  await check('setup', async () => {
    B = await login(BUYER); O = await login(OTHER); AD = await login(ADMIN); H = await login(HOST);
    await M.User.update({ role: 'admin' }, { where: { publicId: AD.user.id } }); AD = await login(ADMIN);
    await M.User.update({ role: 'host', hostStatus: 'approved' }, { where: { publicId: H.user.id } }); H = await login(HOST);
  });

  await check('packs: only active ones, price from the server', async () => {
    const r = await call('GET', '/wallet/packs', { token: B.accessToken });
    assert.equal(r.status, 200);
    packs = r.json.packs;
    assert.ok(packs.length >= 1 && packs.every((p) => p.priceInr > 0 && p.totalCoins === p.coins + p.bonusCoins));
    assert.equal(r.json.purchaseEnabled, true);
    assert.equal((await call('GET', '/wallet/packs')).status, 401);
  });

  let order;
  await check('order: server fixes price and coins; unknown pack refused', async () => {
    assert.equal((await call('POST', '/wallet/orders', { token: B.accessToken, body: { packId: 999999 } })).status, 404);
    assert.equal((await call('POST', '/wallet/orders', { token: B.accessToken, body: { packId: packs[0].id, coins: 999999 } })).status, 400, 'the app cannot send its own coins');
    const r = await call('POST', '/wallet/orders', { token: B.accessToken, body: { packId: packs[0].id } });
    assert.equal(r.status, 200, JSON.stringify(r.json));
    order = r.json;
    assert.equal(order.provider, 'fake');
    assert.equal(order.amountPaise, packs[0].pricePaise);
    assert.equal(order.coins, packs[0].totalCoins);
  });

  await check('verify: a wrong signature never gives coins', async () => {
    const before = await coins(B);
    const r = await call('POST', '/wallet/verify', { token: B.accessToken, body: { orderId: order.orderId, paymentId: 'pay_x', signature: 'nope' } });
    assert.equal(r.json.error.code, 'bad_signature');
    assert.equal(await coins(B), before);
  });

  await check('someone else cannot confirm or see my order', async () => {
    const pay = (await call('POST', `/wallet/dev/pay/${order.orderId}`, { token: B.accessToken })).json;
    assert.equal((await call('POST', '/wallet/verify', { token: O.accessToken, body: { orderId: order.orderId, paymentId: pay.paymentId, signature: pay.signature } })).status, 404);
    assert.equal((await call('GET', `/wallet/payments/${order.orderId}`, { token: O.accessToken })).status, 404);
  });

  await check('pay + verify: coins credited once, written in the ledger', async () => {
    const before = await coins(B);
    const pay = (await call('POST', `/wallet/dev/pay/${order.orderId}`, { token: B.accessToken })).json;
    const r = await call('POST', '/wallet/verify', { token: B.accessToken, body: { orderId: order.orderId, paymentId: pay.paymentId, signature: pay.signature } });
    assert.equal(r.status, 200, JSON.stringify(r.json));
    assert.equal(r.json.credited, true);
    assert.equal(r.json.coins, before + order.coins);
    const again = await call('POST', '/wallet/verify', { token: B.accessToken, body: { orderId: order.orderId, paymentId: pay.paymentId, signature: pay.signature } });
    assert.equal(again.json.credited, false, 'second confirmation gives nothing');
    assert.equal((await webhook(captured(order.orderId, order.amountPaise, pay.paymentId))).status, 200);
    assert.equal(await coins(B), before + order.coins, 'the webhook after it gives nothing either');
    const h = await call('GET', '/wallet/history', { token: B.accessToken });
    assert.equal(h.json.items[0].kind, 'recharge');
    assert.equal(h.json.items[0].coins, order.coins);
    assert.equal((await call('GET', `/wallet/payments/${order.orderId}`, { token: B.accessToken })).json.payment.status, 'paid');
  });

  await check('webhook alone credits (app closed mid-payment); bad signature / wrong amount refused', async () => {
    const o = (await call('POST', '/wallet/orders', { token: B.accessToken, body: { packId: packs[0].id } })).json;
    const before = await coins(B);
    assert.equal((await webhook(captured(o.orderId, o.amountPaise), 'forged')).status, 400);
    await webhook(captured(o.orderId, o.amountPaise + 100));
    assert.equal(await coins(B), before, 'a different amount is not trusted');
    await webhook(captured(o.orderId, o.amountPaise));
    assert.equal(await coins(B), before + o.coins);
    const p = (await call('GET', `/wallet/payments/${o.orderId}`, { token: B.accessToken })).json.payment;
    assert.equal(p.status, 'paid');
  });

  await check('app confirmation and webhook at the same moment: coins only once', async () => {
    for (let i = 0; i < 5; i++) {
      const o = (await call('POST', '/wallet/orders', { token: B.accessToken, body: { packId: packs[0].id } })).json;
      const pay = (await call('POST', `/wallet/dev/pay/${o.orderId}`, { token: B.accessToken })).json;
      const before = await coins(B);
      await Promise.all([
        call('POST', '/wallet/verify', { token: B.accessToken, body: { orderId: o.orderId, paymentId: pay.paymentId, signature: pay.signature } }),
        webhook(captured(o.orderId, o.amountPaise, pay.paymentId)),
        call('POST', '/wallet/verify', { token: B.accessToken, body: { orderId: o.orderId, paymentId: pay.paymentId, signature: pay.signature } }),
      ]);
      assert.equal(await coins(B), before + o.coins, `round ${i + 1}`);
    }
  });

  await check('cancelled checkout is marked failed; a later captured webhook still credits', async () => {
    const o = (await call('POST', '/wallet/orders', { token: B.accessToken, body: { packId: packs[0].id } })).json;
    const f = await call('POST', `/wallet/orders/${o.orderId}/failed`, { token: B.accessToken, body: { reason: 'Payment cancelled' } });
    assert.equal(f.json.payment.status, 'failed');
    const before = await coins(B);
    await webhook(captured(o.orderId, o.amountPaise));
    assert.equal(await coins(B), before + o.coins);
    await webhook({ event: 'payment.failed', payload: { payment: { entity: { id: 'x', order_id: o.orderId, amount: o.amountPaise, currency: 'INR' } } } });
    assert.equal((await call('GET', `/wallet/payments/${o.orderId}`, { token: B.accessToken })).json.payment.status, 'paid', 'paid never goes back to failed');
  });

  await check('admin can pause purchases', async () => {
    await settings.update('coin_purchase_enabled', false, null);
    const r = await call('POST', '/wallet/orders', { token: B.accessToken, body: { packId: packs[0].id } });
    assert.equal(r.json.error.code, 'purchases_paused');
    assert.equal((await call('GET', '/wallet/packs', { token: B.accessToken })).json.purchaseEnabled, false);
    await settings.update('coin_purchase_enabled', true, null);
  });

  let newPack;
  await check('admin packs: only admin; a new price applies to new orders only', async () => {
    assert.equal((await call('GET', '/admin/packs', { token: B.accessToken })).status, 403);
    assert.equal((await call('POST', '/admin/packs', { token: AD.accessToken, body: { coins: 10, priceInr: 1.234 } })).status, 400, 'paise only');
    const c = await call('POST', '/admin/packs', { token: AD.accessToken, body: { coins: 100, bonusCoins: 5, priceInr: 9.99, tag: 'Test', sort: 999 } });
    assert.equal(c.status, 200, JSON.stringify(c.json));
    newPack = c.json.pack;
    const o = (await call('POST', '/wallet/orders', { token: B.accessToken, body: { packId: newPack.id } })).json;
    assert.equal(o.amountPaise, 999);
    await call('PUT', `/admin/packs/${newPack.id}`, { token: AD.accessToken, body: { priceInr: 19 } });
    const p = await M.Payment.findOne({ where: { orderId: o.orderId } });
    assert.equal(p.amountPaise, 999, 'the old order keeps its price');
    assert.equal((await call('POST', '/wallet/orders', { token: B.accessToken, body: { packId: newPack.id } })).json.amountPaise, 1900);
    await call('PUT', `/admin/packs/${newPack.id}`, { token: AD.accessToken, body: { active: false } });
    assert.ok(!(await call('GET', '/wallet/packs', { token: B.accessToken })).json.packs.some((x) => x.id === newPack.id), 'inactive pack hidden');
    assert.equal((await call('POST', '/wallet/orders', { token: B.accessToken, body: { packId: newPack.id } })).status, 404);
    const list = await call('GET', '/admin/payments', { token: AD.accessToken });
    assert.ok(list.json.payments.length >= 3);
  });

  await check('host earnings: hosts only, with history', async () => {
    assert.equal((await call('GET', '/host/earnings', { token: B.accessToken })).status, 403);
    const r = await call('GET', '/host/earnings', { token: H.accessToken });
    assert.equal(r.status, 200);
    assert.ok(Number.isInteger(r.json.earnings) && Array.isArray(r.json.items));
  });

  await check('VIP: buy a plan (days + bonus coins), buy again to extend; hosts cannot buy VIP', async () => {
    const v = (await call('GET', '/vip', { token: B.accessToken })).json;
    assert.ok(v.plans.length >= 1 && v.vip === false && v.callDiscountPct >= 0);
    const plan = v.plans[0];
    const before = Number((await M.Wallet.findByPk((await M.User.findOne({ where: { publicId: B.user.id } })).id)).coins);
    const buy = async () => {
      const o = (await call('POST', '/vip/orders', { token: B.accessToken, body: { planId: plan.id } })).json;
      assert.equal(o.vip, true); assert.equal(o.coins, plan.bonusCoins);
      const pay = (await call('POST', `/wallet/dev/pay/${o.orderId}`, { token: B.accessToken })).json;
      const r = await call('POST', '/wallet/verify', { token: B.accessToken, body: { orderId: o.orderId, paymentId: pay.paymentId, signature: pay.signature } });
      assert.equal(r.status, 200); assert.equal(r.json.credited, true);
    };
    await buy();
    let me = (await call('GET', '/me', { token: B.accessToken })).json.user;
    assert.equal(me.vip, true);
    const days = (new Date(me.vipUntil) - Date.now()) / 86_400_000;
    assert.ok(days > plan.days - 1 && days <= plan.days + 0.01, 'vip for the plan days');
    assert.equal(me.coins, before + plan.bonusCoins);
    await buy();
    me = (await call('GET', '/me', { token: B.accessToken })).json.user;
    assert.ok((new Date(me.vipUntil) - Date.now()) / 86_400_000 > 2 * plan.days - 1, 'buying again adds the days');
    const hist = (await call('GET', '/wallet/history', { token: B.accessToken })).json.items;
    assert.ok(hist.some((x) => x.kind === 'vip' && x.title.startsWith('VIP · ')));
    assert.equal((await call('POST', '/vip/orders', { token: H.accessToken, body: { planId: plan.id } })).status, 403);
  });

  await check('VIP benefits: cheaper call price on host cards, free chat, VIP badge on the profile', async () => {
    const hu = await M.User.findOne({ where: { publicId: H.user.id } });
    await M.HostProfile.findOrCreate({ where: { userId: hu.id }, defaults: { callRate: 60, messagePrice: 200 } });
    await M.HostProfile.update({ callRate: 60 }, { where: { userId: hu.id } });
    const off = Number((await call('GET', '/vip', { token: B.accessToken })).json.callDiscountPct);
    const card = (await call('GET', `/profile/${H.user.id}`, { token: B.accessToken })).json.person;
    assert.equal(card.callRate, Math.max(1, Math.round(60 * (100 - off) / 100)));
    assert.equal((await call('GET', `/profile/${H.user.id}`, { token: O.accessToken })).json.person.callRate, 60, 'not VIP: full price');
    const chat = (await call('GET', `/chats/${H.user.id}/messages`, { token: B.accessToken })).json;
    assert.equal(chat.freeLeft, null); assert.equal(chat.messagePrice, 0);
    const bu = await M.User.findOne({ where: { publicId: B.user.id } });
    const coins0 = Number((await M.Wallet.findByPk(bu.id)).coins);
    for (let i = 0; i < 6; i++) assert.equal((await call('POST', `/chats/${H.user.id}/messages`, { token: B.accessToken, body: { text: 'hi ' + i, clientId: 'vipmsg-' + i + '-' + Date.now() } })).status, 200);
    assert.equal(Number((await M.Wallet.findByPk(bu.id)).coins), coins0, 'VIP messages are free');
    assert.equal((await call('GET', `/profile/${B.user.id}`, { token: H.accessToken })).json.person.vip, true);
  });

  if (newPack) await M.CoinPack.destroy({ where: { id: newPack.id } });
  await kit.cleanup([BUYER, OTHER, ADMIN, HOST]);
  await kit.stop();
  console.log(`\n${passed} passed, ${failed} failed`);
  process.exit(failed ? 1 : 0);
})().catch((e) => { console.error('crashed:', e); process.exit(1); });
