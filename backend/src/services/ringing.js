const crypto = require('crypto');
const { sequelize, User, Wallet, HostProfile, Transaction, Call } = require('../models');
const settings = require('./settings');
const { canInteract } = require('./rules');
const cfg = require('../config');

/**
 * Calls, end to end, on the server (the phones only show what happens here):
 *  - "someone is looking at you": a user opens a host's chat (host told at once) or stays on the profile (told after
 *    `watch_notify_seconds`; after `watch_autocall_seconds` a call rings by itself when the host is online and free)
 *  - ringing: button, automatic or Random match; the caller can cancel, the other one accepts or declines, nobody answering
 *    = missed. Calls go both ways: a user calls a host, and a host may call a user (the user picks up or declines). Either
 *    way the USER pays the host's rate and the host earns its share.
 *  - a running call: coins are charged at the START of every minute (the first at pick-up) and the host earns its share of
 *    each minute. The minute running when the call ends: if the USER hangs up, the host keeps it; if the HOST hangs up
 *    (or the host's connection is lost), the user gets that minute back and the host earns nothing for it.
 *    When the coins run out the call ends at the minute boundary.
 *  - WebRTC: offer / answer / ICE are passed between the two phones of the call and nobody else
 *  - a dropped connection gets `call_reconnect_seconds` to come back before the call ends
 * No cooldown: a user may call a host as often as he likes; the host decides to pick up or not.
 */
const calls = new Map();          // callId -> live call (see startCall)
const busy = new Map();           // userId -> callId (a person is in at most one call)
const watches = new Map();        // socket.id -> { timers: [] }
const lastNotify = new Map();     // `${where}:${userId}:${hostId}` -> time
let ioRef = null;
// one billing minute (tests shorten it; read when used)
const minuteMs = () => Number(process.env.CALL_MINUTE_MS || 60_000);
// how long a host's call keeps ringing while the user recharges to pick it up (tests shorten it)
const toppingSeconds = () => Number(process.env.CALL_TOPPING_SECONDS || 90);

const card = (u) => ({ id: Number(u.publicId), name: u.name, dp: u.dpPath ? `${cfg.publicUrl}/uploads/${u.dpPath}` : null, city: u.city || null, vip: require('./vip').isVip(u) });
const toBoth = (io, c) => io.to(`user:${c.userId}`).to(`user:${c.hostId}`);
// who rang and who is being rung (a host may call a user too)
const calleeOf = (c) => (c.callerId === c.hostId ? c.userId : c.hostId);

function setBusy(io, call, on) {
  if (on) { busy.set(call.userId, call.id); busy.set(call.hostId, call.id); } else { busy.delete(call.userId); busy.delete(call.hostId); }
  io.emit('presence', { id: call.hostPublicId, busy: on }); // the Home list shows Busy / Online at once
}

// ---------------------------------------------------------------- money for one minute
/** Charges the next minute (user pays, host earns its share at once). Returns false (and ends the call) when the user cannot pay it. */
async function chargeMinute(io, c) {
  if (!calls.has(c.id) || c.state !== 'active') return false;
  const minute = c.minutes + 1;
  const share = Number(await settings.get('host_share_pct'));
  const hostGets = Math.floor((c.rate * share) / 100);
  let coinsLeft;
  try {
    coinsLeft = await sequelize.transaction(async (t) => {
      const ids = [c.userId, c.hostId].sort((a, b) => Number(a) - Number(b));
      const w = {};
      for (const id of ids) w[id] = await Wallet.findByPk(id, { transaction: t, lock: t.LOCK.UPDATE });
      const payer = w[c.userId]; const host = w[c.hostId];
      if (Number(payer.coins) < c.rate) return null;
      await payer.update({ coins: Number(payer.coins) - c.rate }, { transaction: t });
      await Transaction.create({ userId: c.userId, wallet: 'coins', kind: 'call_spend', coins: -c.rate, balanceAfter: Number(payer.coins), ref: { callId: c.id, minute, hostId: Number(c.hostId) } }, { transaction: t });
      if (hostGets > 0) {
        await host.update({ earnings: Number(host.earnings) + hostGets }, { transaction: t });
        await Transaction.create({ userId: c.hostId, wallet: 'earnings', kind: 'call_earn', coins: hostGets, balanceAfter: Number(host.earnings), ref: { callId: c.id, minute, buyerId: Number(c.userId), price: c.rate, sharePct: share } }, { transaction: t });
      }
      await Call.increment({ minutesCharged: 1, coinsCharged: c.rate, hostEarned: hostGets }, { where: { id: c.id }, transaction: t });
      return Number(payer.coins);
    });
  } catch (e) { console.error('[billing]', e.message); coinsLeft = null; }
  if (coinsLeft === null) { await endCall(io, c, 'no_coins'); return false; }
  c.minutes = minute; c.coins += c.rate; c.earned += hostGets;
  c.last = { minute, hostGets, at: Date.now() };          // the running minute, for a refund if the host hangs up
  const levels = require('./levels');
  levels.xpFor('xp_call_minute').then((x) => { levels.addXp(c.userId, x); levels.addXp(c.hostId, x); });
  io.to(`user:${c.userId}`).emit('call_billing', { callId: c.id, minute, coins: c.coins, coinsLeft, rate: c.rate, low: coinsLeft < c.rate });
  io.to(`user:${c.hostId}`).emit('call_billing', { callId: c.id, minute, earned: c.earned + c.giftEarned });   // the host's whole take so far (minutes + gifts)
  return true;
}

/** The host hung up (or dropped) during a minute: the user gets that minute back and the host's share of it is taken back. */
async function refundRunningMinute(c) {
  const last = c.last;
  if (!last) return;
  c.last = null;
  await sequelize.transaction(async (t) => {
    const ids = [c.userId, c.hostId].sort((a, b) => Number(a) - Number(b));
    const w = {};
    for (const id of ids) w[id] = await Wallet.findByPk(id, { transaction: t, lock: t.LOCK.UPDATE });
    const payer = w[c.userId]; const host = w[c.hostId];
    await payer.update({ coins: Number(payer.coins) + c.rate }, { transaction: t });
    await Transaction.create({ userId: c.userId, wallet: 'coins', kind: 'call_refund', coins: c.rate, balanceAfter: Number(payer.coins), ref: { callId: c.id, minute: last.minute, hostId: Number(c.hostId), reason: 'host_ended' } }, { transaction: t });
    if (last.hostGets > 0) {
      await host.update({ earnings: Number(host.earnings) - last.hostGets }, { transaction: t });
      await Transaction.create({ userId: c.hostId, wallet: 'earnings', kind: 'call_earn_reversed', coins: -last.hostGets, balanceAfter: Number(host.earnings), ref: { callId: c.id, minute: last.minute, buyerId: Number(c.userId), reason: 'host_ended' } }, { transaction: t });
    }
    await Call.increment({ coinsCharged: -c.rate, hostEarned: -last.hostGets }, { where: { id: c.id }, transaction: t });
  });
  c.coins -= c.rate; c.earned -= last.hostGets;
}

// ---------------------------------------------------------------- starting and ending
/** [viewer] = the user, [host] = the host; [hostCalls] = the host rang the user (the user still pays). */
async function startCall(io, isConnected, viewer, host, kind, rate, hostCalls = false) {
  const auto = kind === 'auto';
  if (auto && !(await settings.get('autocall_enabled'))) return null;
  const hostId = String(host.id); const userId = String(viewer.id);
  if (!isConnected(hostId) || busy.has(hostId) || busy.has(userId)) return null;
  if (hostCalls && !isConnected(userId)) return null;
  const fresh = await User.findByPk(host.id);
  const profile = await HostProfile.findOne({ where: { userId: host.id } });
  if (!fresh || fresh.status !== 'active' || fresh.hostStatus !== 'approved' || !profile || !profile.isOnline) return null;
  if (!(await canInteract(viewer, fresh)).ok) return null;
  const perMin = await require('./vip').callRateFor(viewer, rate || profile.callRate);   // VIP users pay less
  const wallet = await Wallet.findOne({ where: { userId: viewer.id } });
  if (!hostCalls && (!wallet || wallet.coins < perMin)) return null;   // the caller cannot pay even one minute
  if (busy.has(hostId) || busy.has(userId)) return null;       // someone else got there during the checks

  const id = crypto.randomUUID();
  const ring = Number(await settings.get('call_ring_seconds')) * 1000;
  const call = {
    id, userId, hostId, callerId: hostCalls ? hostId : userId, toppingUp: false, hostPublicId: Number(fresh.publicId), userPublicId: Number(viewer.publicId), state: 'ringing', auto, kind, rate: perMin,
    minutes: 0, coins: 0, earned: 0, giftEarned: 0, gifts: 0, timers: [], acceptedAt: null, last: null, graceTimer: null,
  };
  calls.set(id, call); setBusy(io, call, true);
  call.timers.push(setTimeout(() => endCall(io, call, 'missed'), ring));
  await Call.create({ id, userId: viewer.id, hostId: host.id, kind, rate: perMin, byHost: hostCalls });
  // [role] = the receiver's side in this call; `from` / `host` = the other person
  const ringing = { callId: id, auto, kind, ratePerMinute: perMin, ringSeconds: ring / 1000 };
  if (hostCalls) {
    io.to(`user:${userId}`).emit('incoming_call', { ...ringing, from: card(fresh), role: 'user' });
    io.to(`user:${hostId}`).emit('call_ringing', { ...ringing, host: card(viewer), role: 'host' });
  } else {
    io.to(`user:${hostId}`).emit('incoming_call', { ...ringing, from: card(viewer), role: 'host' });
    io.to(`user:${userId}`).emit('call_ringing', { ...ringing, host: card(fresh), role: 'user' });
  }
  return id;
}

async function accept(io, c) {
  c.timers.forEach(clearTimeout); c.timers = [];
  c.state = 'active'; c.acceptedAt = Date.now();
  await Call.update({ status: 'active', acceptedAt: new Date(c.acceptedAt) }, { where: { id: c.id } });
  toBoth(io, c).emit('call_accepted', { callId: c.id, rate: c.rate, kind: c.kind });
  if (!(await chargeMinute(io, c))) return;                     // the first minute at pick-up
  // then at the start of every following minute
  c.timers.push(setInterval(() => { chargeMinute(io, c).catch((e) => console.error('[billing]', e.message)); }, minuteMs()));
}

/** Ends a call. [endedBy] = internal id of who hung up / dropped (null for missed, no coins ...): it decides the running minute. */
async function endCall(io, call, reason, endedBy = null) {
  if (!calls.delete(call.id)) return;
  call.timers.forEach((t) => { clearTimeout(t); clearInterval(t); });
  clearTimeout(call.graceTimer);
  setBusy(io, call, false);
  // the host hung up / dropped during a minute: that minute goes back to the user
  if (endedBy && String(endedBy) === String(call.hostId) && call.state === 'active') {
    try { await refundRunningMinute(call); } catch (e) { console.error('[refund]', e.message); }
  }
  const seconds = call.acceptedAt ? Math.round((Date.now() - call.acceptedAt) / 1000) : 0;
  const by = !endedBy ? null : String(endedBy) === String(call.hostId) ? 'host' : 'user';
  // earned = the host's whole take from this call: its minutes + its share of gifts sent during the call
  const summary = {
    callId: call.id, reason, endedBy: by, seconds, minutes: call.minutes, coins: call.coins, earned: call.earned + call.giftEarned,
    callEarned: call.earned, giftEarned: call.giftEarned, gifts: call.gifts, rate: call.rate, kind: call.kind,
  };
  toBoth(io, call).emit('call_ended', summary);
  try {
    await Call.update({ status: 'ended', endReason: reason, endedAt: new Date(), seconds }, { where: { id: call.id } });
    // a line in their chat: "Missed video call" / "Video call · 06:24" (not for Random match: they may not know each other)
    // (no line for Random match, nor for an automatic profile call the user left before it could be answered)
    if (call.kind !== 'random' && !(call.kind === 'auto' && reason === 'cancelled' && !call.acceptedAt)) await require('../routes/chat').callLine(call, reason, seconds);
    // the one who was rung and did not answer gets a "missed call" in the bell list
    // (an automatic profile call the user left before it could be answered is not a missed call for the host)
    if ((reason === 'missed' || (reason === 'cancelled' && call.kind !== 'auto')) && !call.acceptedAt) {
      const callerId = call.callerId || call.userId; const rung = callerId === call.hostId ? call.userId : call.hostId;
      const caller = await User.findByPk(callerId);
      await require('./notify').notify(rung, 'missed_call', `Missed video call from ${caller ? caller.name : 'someone'}`, null, { peerId: caller ? Number(caller.publicId) : null });
    }
  } catch (e) { console.error('[call end]', e.message); }
}

function clearWatch(socket) {
  const w = watches.get(socket.id);
  if (w) { w.timers.forEach(clearTimeout); watches.delete(socket.id); }
}

/** Someone with a running call came back (new connection): the call goes on and the phones reconnect the video. */
function resumed(io, socket, userId) {
  const id = busy.get(String(userId)); const c = id && calls.get(id);
  if (!c) return;
  if (c.graceTimer) { clearTimeout(c.graceTimer); c.graceTimer = null; }
  socket.emit('call_resume', {
    callId: c.id, state: c.state, kind: c.kind, rate: c.rate, role: String(c.userId) === String(userId) ? 'user' : 'host',
    peerId: String(c.userId) === String(userId) ? c.hostPublicId : c.userPublicId, startedAt: c.acceptedAt, minutes: c.minutes, coins: c.coins,
  });
  io.to(`user:${String(c.userId) === String(userId) ? c.hostId : c.userId}`).emit('call_peer_back', { callId: c.id });
}

// ---------------------------------------------------------------- one phone's events
function attach(io, socket, isConnected) {
  ioRef = io;
  const me = socket.data.userId;
  const load = () => User.findByPk(me);
  resumed(io, socket, me);

  async function watch(msg, where) {
    try {
      clearWatch(socket);
      const publicId = Number(msg && msg.hostId);
      if (!Number.isInteger(publicId)) return;
      const viewer = await load();
      const host = await User.findOne({ where: { publicId, role: 'host', hostStatus: 'approved' } });
      if (!viewer || !host || viewer.role === 'host' || !(await canInteract(viewer, host)).ok) return;
      const notifyMs = Number(await settings.get('watch_notify_seconds')) * 1000;
      const key = `${where}:${viewer.id}:${host.id}`;
      const w = { timers: [] };
      watches.set(socket.id, w);
      w.timers.push(setTimeout(() => {
        if (!isConnected(host.id) || Date.now() - (lastNotify.get(key) || 0) < 60000) return; // no spam by re-entering
        lastNotify.set(key, Date.now());
        io.to(`user:${host.id}`).emit('profile_watch', { user: card(viewer), where });
      }, where === 'chat' ? 0 : notifyMs)); // opening a chat is a clear sign: the host is told at once
      if (where === 'profile') {
        const callMs = Number(await settings.get('watch_autocall_seconds')) * 1000;
        w.timers.push(setTimeout(() => startCall(io, isConnected, viewer, host, 'auto').catch((e) => console.error('[autocall]', e.message)), callMs));
      }
    } catch (e) { console.error('[watch]', e.message); }
  }
  socket.on('profile:open', (msg) => watch(msg, 'profile'));
  socket.on('profile:close', () => clearWatch(socket));
  socket.on('chat:open', (msg) => watch(msg, 'chat'));
  socket.on('chat:close', () => clearWatch(socket));

  // a host calls a user (from the user's profile): the user must be online, free and able to pay one minute of the host's rate
  async function hostCalls(host, publicId) {
    const user = await User.findOne({ where: { publicId, role: 'user' } });
    if (!user || host.hostStatus !== 'approved' || !(await canInteract(host, user)).ok) return { ok: false, reason: 'unavailable' };
    if (!isConnected(user.id)) return { ok: false, reason: 'offline' };
    if (busy.has(String(user.id))) return { ok: false, reason: 'busy' };
    if (busy.has(String(host.id))) return { ok: false, reason: 'in_call' };
    const profile = await HostProfile.findOne({ where: { userId: host.id } });
    const wallet = await Wallet.findOne({ where: { userId: user.id } });
    if (!profile) return { ok: false, reason: 'unavailable' };
    // a user without enough coins still gets the call: picking up asks them to recharge first (see call:accept / call:topping)
    const id = await startCall(io, isConnected, user, host, 'direct', null, true);
    return id ? { ok: true, callId: id } : { ok: false, reason: 'unavailable' };
  }

  // the call button: a user calls a host (same checks: online, free, coins, not blocked), or a host calls a user
  socket.on('call:start', async (msg, ack) => {
    const reply = typeof ack === 'function' ? ack : () => {};
    try {
      const viewer = await load();
      if (viewer && viewer.role === 'host') return reply(await hostCalls(viewer, Number(msg && (msg.userId || msg.hostId))));
      const host = await User.findOne({ where: { publicId: Number(msg && msg.hostId), role: 'host', hostStatus: 'approved' } });
      if (!viewer || !host || viewer.role === 'host' || !(await canInteract(viewer, host)).ok) return reply({ ok: false, reason: 'unavailable' });
      clearWatch(socket); // he is calling now: no second automatic call from the same visit
      const wallet = await Wallet.findOne({ where: { userId: viewer.id } });
      const profile = await HostProfile.findOne({ where: { userId: host.id } });
      if (!isConnected(host.id) || !profile || !profile.isOnline) return reply({ ok: false, reason: 'offline' });
      if (busy.has(String(host.id))) return reply({ ok: false, reason: 'busy' });
      if (busy.has(String(viewer.id))) return reply({ ok: false, reason: 'in_call' });
      const need = await require('./vip').callRateFor(viewer, profile.callRate);
      if (!wallet || wallet.coins < need) return reply({ ok: false, reason: 'not_enough_coins', need });
      const id = await startCall(io, isConnected, viewer, host, 'direct');
      reply(id ? { ok: true, callId: id } : { ok: false, reason: 'unavailable' });
    } catch (e) { console.error('[call:start]', e.message); reply({ ok: false, reason: 'error' }); }
  });

  // Random match: any online, free host the user may talk to, at the fixed random rate (the same for every host)
  socket.on('random:start', async (msg, ack) => {
    const reply = typeof ack === 'function' ? ack : () => {};
    try {
      const viewer = await load();
      if (!viewer || viewer.role === 'host') return reply({ ok: false, reason: 'unavailable' });
      if (busy.has(String(viewer.id))) return reply({ ok: false, reason: 'in_call' });
      const rate = await require('./vip').callRateFor(viewer, Number(await settings.get('random_match_rate')));
      const wallet = await Wallet.findOne({ where: { userId: viewer.id } });
      if (!wallet || wallet.coins < rate) return reply({ ok: false, reason: 'not_enough_coins', need: rate });
      const gender = ['female', 'male'].includes(msg && msg.gender) ? msg.gender : null;
      const skip = new Set(((msg && msg.skip) || []).map(Number));   // hosts the user just met / skipped
      const online = await HostProfile.findAll({ where: { isOnline: true }, attributes: ['userId'] });
      const ids = online.map((p) => String(p.userId)).filter((id) => isConnected(id) && !busy.has(id) && id !== String(viewer.id));
      const hosts = ids.length ? await User.findAll({ where: { id: ids, role: 'host', hostStatus: 'approved', status: 'active', ...(gender ? { gender } : {}) } }) : [];
      const pool = [];
      for (const h of hosts) if (!skip.has(Number(h.publicId)) && (await canInteract(viewer, h)).ok) pool.push(h);
      if (!pool.length) return reply({ ok: false, reason: 'no_hosts' });
      const host = pool[crypto.randomInt(pool.length)];
      const id = await startCall(io, isConnected, viewer, host, 'random', rate);
      reply(id ? { ok: true, callId: id, host: card(host), rate } : { ok: false, reason: 'no_hosts' });
    } catch (e) { console.error('[random]', e.message); reply({ ok: false, reason: 'error' }); }
  });

  // answering: only the two people in that call may touch it
  const own = (msg) => { const c = calls.get(msg && msg.callId); return c && (c.userId === me || c.hostId === me) ? c : null; };
  socket.on('call:cancel', (m) => { const c = own(m); if (c && c.state === 'ringing' && c.callerId === me) endCall(io, c, 'cancelled'); });
  socket.on('call:decline', (m) => { const c = own(m); if (c && c.state === 'ringing' && calleeOf(c) === me) endCall(io, c, 'declined'); });
  socket.on('call:accept', async (m) => {
    const c = own(m);
    if (!c || c.state !== 'ringing' || calleeOf(c) !== me) return;
    // the user picks up a host's call without coins for the first minute: no pick-up, the phone shows the recharge sheet
    if (calleeOf(c) === c.userId) {
      const w = await Wallet.findOne({ where: { userId: c.userId } });
      if (!w || Number(w.coins) < c.rate) return socket.emit('call_need_coins', { callId: c.id, need: c.rate, coins: w ? Number(w.coins) : 0 });
    }
    accept(io, c).catch((e) => console.error('[accept]', e.message));
  });
  // the rung user is recharging to pick up: the call keeps ringing a while longer (once) and the host is told
  socket.on('call:topping', (m) => {
    const c = own(m);
    if (!c || c.state !== 'ringing' || calleeOf(c) !== me || c.callerId !== c.hostId || c.toppingUp) return;
    c.toppingUp = true;
    c.timers.forEach(clearTimeout); c.timers = [];
    const extra = toppingSeconds() * 1000;
    c.timers.push(setTimeout(() => endCall(io, c, 'missed'), extra));
    toBoth(io, c).emit('call_topping', { callId: c.id, seconds: extra / 1000 });
  });
  socket.on('call:end', (m) => { const c = own(m); if (c) endCall(io, c, 'ended', me); });

  // WebRTC: offer / answer / ICE candidates go to the other phone of this call only
  socket.on('rtc:signal', (m) => {
    const c = own(m);
    if (!c || !m.data || typeof m.data !== 'object') return;
    const other = c.userId === me ? c.hostId : c.userId;
    io.to(`user:${other}`).emit('rtc_signal', { callId: c.id, data: m.data });
  });
  // short text messages inside a running call (not stored, not charged)
  socket.on('call:chat', (m) => {
    const c = own(m);
    if (!c || c.state !== 'active' || typeof m.text !== 'string') return;
    const text = m.text.trim().slice(0, 300);
    if (!text) return;
    io.to(`user:${c.userId === me ? c.hostId : c.userId}`).emit('call_chat', { callId: c.id, text });
  });

  socket.on('disconnect', () => {
    clearWatch(socket);
    const id = busy.get(me); const c = id && calls.get(id);
    if (!c || isConnected(me)) return;
    if (c.state === 'ringing') return endCall(io, c, 'disconnected');
    // a running call waits a little for the phone to come back (lift, tunnel, network switch)
    settings.get('call_reconnect_seconds').then((sec) => {
      if (!calls.has(c.id) || isConnected(me)) return;
      toBoth(io, c).emit('call_peer_lost', { callId: c.id, seconds: Number(sec) });
      // whoever's connection is gone counts as the one who hung up (it decides the running minute)
      c.graceTimer = setTimeout(() => { if (!isConnected(me)) endCall(io, c, 'disconnected', me); }, Number(sec) * 1000);
    });
  });
}

function reset() {
  for (const c of calls.values()) { c.timers.forEach((t) => { clearTimeout(t); clearInterval(t); }); clearTimeout(c.graceTimer); }
  for (const w of watches.values()) w.timers.forEach(clearTimeout);
  calls.clear(); busy.clear(); watches.clear(); lastNotify.clear();
}

function clearHistory() { lastNotify.clear(); } // tests only
/** Ends every running call of one person (blocked, deleted, logged out everywhere). */
function endCallsOf(userId) {
  const id = busy.get(String(userId)); const c = id && calls.get(id);
  if (c && ioRef) endCall(ioRef, c, 'ended', userId);
}

/** A gift went from [userId] to [hostId]: if they are in a running call together, it counts for that call. */
function giftDuringCall(userId, hostId, hostGets, quantity) {
  const c = calls.get(busy.get(String(userId)));
  if (!c || c.state !== 'active' || c.hostId !== String(hostId)) return;
  c.giftEarned += hostGets; c.gifts += quantity;
  if (ioRef) ioRef.to(`user:${c.hostId}`).emit('call_earned', { callId: c.id, earned: c.earned + c.giftEarned });
}

module.exports = { attach, reset, giftDuringCall, clearHistory, endCallsOf, isBusy: (userId) => busy.has(String(userId)), _calls: calls, _chargeMinute: (id) => calls.get(id) && chargeMinute(ioRef, calls.get(id)) };
