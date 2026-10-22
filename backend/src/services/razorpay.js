const crypto = require('crypto');
const cfg = require('../config');

/**
 * Razorpay, server side only. The key secret never leaves this server; the app only gets the public key id and an order id.
 * Without keys (development) a pretend Razorpay works the same way, signed with a secret made at start, so the whole flow
 * can be built and tested; it can never be on in production (see config).
 */
const FAKE_SECRET = crypto.randomBytes(32).toString('hex');
const secret = () => (cfg.razorpay.fake ? FAKE_SECRET : cfg.razorpay.keySecret);
const provider = () => (cfg.razorpay.fake ? 'fake' : 'razorpay');

async function createOrder(amountPaise, receipt, notes) {
  if (cfg.razorpay.fake) return { id: `order_fake_${crypto.randomBytes(8).toString('hex')}`, amount: amountPaise, currency: 'INR' };
  if (!cfg.razorpay.keyId || !cfg.razorpay.keySecret) throw new Error('Razorpay keys missing');
  const auth = Buffer.from(`${cfg.razorpay.keyId}:${cfg.razorpay.keySecret}`).toString('base64');
  const r = await fetch('https://api.razorpay.com/v1/orders', {
    method: 'POST',
    headers: { Authorization: `Basic ${auth}`, 'Content-Type': 'application/json' },
    body: JSON.stringify({ amount: amountPaise, currency: 'INR', receipt, notes }),
  });
  const j = await r.json();
  if (!r.ok || !j.id) throw new Error(`Razorpay order failed: ${(j.error && j.error.description) || r.status}`);
  return j;
}

const hmac = (key, text) => crypto.createHmac('sha256', key).update(text).digest('hex');
const same = (a, b) => typeof a === 'string' && typeof b === 'string' && a.length === b.length && crypto.timingSafeEqual(Buffer.from(a), Buffer.from(b));

/** The checkout result from the app is genuine only if Razorpay (or the pretend one) signed "order|payment". */
function verifyPayment(orderId, paymentId, signature) {
  return same(hmac(secret(), `${orderId}|${paymentId}`), signature);
}

/** Webhook calls are genuine only if signed with the webhook secret set in the Razorpay dashboard. */
function verifyWebhook(rawBody, signature) {
  const key = cfg.razorpay.fake ? FAKE_SECRET : cfg.razorpay.webhookSecret;
  if (!key) return false;
  return same(hmac(key, rawBody), signature);
}

/** DEV ONLY: what the pretend Razorpay hands back after a "successful" payment. */
function fakePay(orderId) {
  if (!cfg.razorpay.fake) throw new Error('not in fake mode');
  const paymentId = `pay_fake_${crypto.randomBytes(8).toString('hex')}`;
  return { paymentId, signature: hmac(FAKE_SECRET, `${orderId}|${paymentId}`) };
}
/** DEV ONLY: sign a webhook body like Razorpay would (tests). */
const fakeWebhookSignature = (rawBody) => hmac(FAKE_SECRET, rawBody);

module.exports = { createOrder, verifyPayment, verifyWebhook, fakePay, fakeWebhookSignature, provider };
