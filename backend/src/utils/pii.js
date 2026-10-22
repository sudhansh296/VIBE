const crypto = require('crypto');
const cfg = require('../config');

// PAN numbers are personal data: they are encrypted (AES-256-GCM) before they reach the database.
const key = () => Buffer.from(cfg.piiKey, 'hex');

function encrypt(text) {
  const iv = crypto.randomBytes(12);
  const c = crypto.createCipheriv('aes-256-gcm', key(), iv);
  const enc = Buffer.concat([c.update(String(text), 'utf8'), c.final()]);
  return [iv.toString('hex'), c.getAuthTag().toString('hex'), enc.toString('hex')].join(':');
}

function decrypt(blob) {
  if (!blob) return null;
  const [iv, tag, enc] = blob.split(':').map((h) => Buffer.from(h, 'hex'));
  const d = crypto.createDecipheriv('aes-256-gcm', key(), iv);
  d.setAuthTag(tag);
  return Buffer.concat([d.update(enc), d.final()]).toString('utf8');
}

/** ABCDE1234F -> ABCDE****F, for places that only need to show it is there. */
const maskPan = (pan) => (pan ? pan.slice(0, 5) + '****' + pan.slice(-1) : null);

module.exports = { encrypt, decrypt, maskPan };
