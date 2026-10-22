// Shared helpers for the end-to-end test scripts: start the API, log test phones in, clean everything up afterwards.
process.env.LOGIN_RATE_LIMIT = '2000';
const fs = require('fs');
const path = require('path');
const { Op } = require('sequelize');
const M = require('../models');
const cfg = require('../config');
const { seedSettings } = require('../services/settings');

const app = require('../app');
let base;
let server;

async function start() {
  await M.sequelize.sync();
  await seedSettings();
  process.env.REALTIME_GRACE_MS = process.env.REALTIME_GRACE_MS || '400';
  server = require('http').createServer(app);
  require('../realtime').init(server);
  await new Promise((ok) => server.listen(0, ok));
  base = `http://127.0.0.1:${server.address().port}`;
}

async function stop() {
  await require('../realtime').close();
  server.close();
  await M.sequelize.close();
}

async function call(method, p, { token, body, form, raw, headers: extra } = {}) {
  const headers = { ...(extra || {}) };
  if (token) headers.Authorization = `Bearer ${token}`;
  let payload;
  if (form) payload = form;
  else if (raw !== undefined) { headers['Content-Type'] = 'application/json'; payload = raw; }
  else if (body !== undefined) { headers['Content-Type'] = 'application/json'; payload = JSON.stringify(body); }
  const res = await fetch(base + p, { method, headers, body: payload });
  const type = res.headers.get('content-type') || '';
  let json = null;
  let buffer = null;
  if (type.includes('json')) { try { json = await res.json(); } catch { /* empty */ } } else { buffer = Buffer.from(await res.arrayBuffer()); }
  return { status: res.status, json, buffer, type };
}

async function login(phone10) {
  await call('POST', '/auth/dev/request-otp', { body: { phone: phone10 } });
  return (await call('POST', '/auth/dev/verify-otp', { body: { phone: phone10, code: '123456' } })).json;
}

/** Removes the test accounts, everything that points at them and their files. Safe to call before and after a run. */
async function cleanup(phones10, extraIds = []) {
  const users = await M.User.findAll({ where: { phone: phones10.map((p) => '+91' + p) } });
  const ids = [...users.map((u) => u.id), ...extraIds];
  if (!ids.length) return;
  const media = await M.Media.findAll({ where: { userId: ids }, attributes: ['id'] });
  const mediaIds = media.map((m) => m.id);
  await M.MediaUnlock.destroy({ where: { [Op.or]: [{ userId: ids }, { mediaId: mediaIds }] } });
  const convs = await M.Conversation.findAll({ where: { [Op.or]: [{ userId: ids }, { hostId: ids }] }, attributes: ['id'] });
  await M.Message.destroy({ where: { conversationId: convs.map((c) => c.id) } });
  await M.Conversation.destroy({ where: { id: convs.map((c) => c.id) } });
  await Promise.all([
    M.Media.destroy({ where: { userId: ids } }), M.Follow.destroy({ where: { [Op.or]: [{ followerId: ids }, { followedId: ids }] } }),
    M.Visit.destroy({ where: { [Op.or]: [{ visitedId: ids }, { visitorId: ids }] } }),
    M.HostApplication.destroy({ where: { userId: ids } }), M.HostProfile.destroy({ where: { userId: ids } }),
    M.Transaction.destroy({ where: { userId: ids } }), M.Session.destroy({ where: { userId: ids } }),
    M.UserSetting.destroy({ where: { userId: ids } }), M.Block.destroy({ where: { [Op.or]: [{ blockerId: ids }, { blockedId: ids }] } }),
    M.Report.destroy({ where: { [Op.or]: [{ reporterId: ids }, { targetId: ids }] } }),
    M.Payment.destroy({ where: { userId: ids } }),
    M.Call.destroy({ where: { [Op.or]: [{ userId: ids }, { hostId: ids }] } }),
  ]);
  await M.Wallet.destroy({ where: { userId: ids } });
  await M.User.destroy({ where: { id: ids } });
  for (const id of ids) {
    fs.rmSync(path.join(cfg.uploadDir, 'media', String(id)), { recursive: true, force: true });
    fs.rmSync(path.join(cfg.uploadDir, 'private', String(id)), { recursive: true, force: true });
    const dp = path.join(cfg.uploadDir, 'dp');
    if (fs.existsSync(dp)) for (const f of fs.readdirSync(dp)) if (f.startsWith(`${id}_`)) fs.rmSync(path.join(dp, f), { force: true });
  }
}

// ---- tiny fake files
const PNG = Buffer.from('89504E470D0A1A0A0000000D49484452000000010000000108060000001F15C4890000000D4944415478DA63F8CFC0F01F0005000201E2216BC80000000049454E44AE426082', 'hex');
const png = (n = 0) => Buffer.concat([PNG, Buffer.from([n])]); // a different file each time

function box(type, ...parts) {
  const body = Buffer.concat(parts);
  const h = Buffer.alloc(8); h.writeUInt32BE(8 + body.length, 0); h.write(type, 4, 'ascii');
  return Buffer.concat([h, body]);
}
/** A minimal MP4 that only says how long it is (enough for the server's header check). */
function mp4(seconds) {
  const mvhd = Buffer.alloc(100); // version 0
  mvhd.writeUInt32BE(1000, 12);   // timescale
  mvhd.writeUInt32BE(seconds * 1000, 16);
  const ftyp = box('ftyp', Buffer.from('isom'), Buffer.alloc(4), Buffer.from('isom'));
  return Buffer.concat([ftyp, box('moov', box('mvhd', mvhd)), box('mdat', Buffer.from('xxxx'))]);
}
const upload = (token, buf, name = 'f', type = 'application/octet-stream') => {
  const f = new FormData(); f.append('file', new Blob([buf], { type }), name);
  return call('POST', '/me/media', { token, form: f });
};

module.exports = { start, stop, call, login, cleanup, png, mp4, upload, M };
