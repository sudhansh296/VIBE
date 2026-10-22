const express = require('express');
const { z } = require('zod');
const { Op } = require('sequelize');
const rateLimit = require('express-rate-limit');
const cfg = require('../config');
const { sequelize, User, HostProfile, Wallet, Transaction, Conversation, Message, Gift } = require('../models');
const settings = require('../services/settings');
const { canInteract } = require('../services/rules');
const realtime = require('../realtime');
const { requireAuth } = require('../middleware/auth');
const { bad, forbidden, notFound, wrap, rateLimitHandler, ApiError } = require('../utils/errors');
const { publicUser } = require('./me');

const r = express.Router();
const pid = z.coerce.number().int().positive();

// ---------------------------------------------------------------- helpers
const fileUrl = (rel) => (rel ? `${cfg.publicUrl}/uploads/${rel}` : null);
const giftView = (g) => g && ({
  id: g.id, name: g.name, emoji: g.emoji || null, price: g.price, category: g.category,
  thumbUrl: fileUrl(g.thumbPath), animUrl: fileUrl(g.animPath), animType: g.animType || null,
});

/** The other person of a chat, and whether these two may talk at all (only a user and an approved host, nobody blocked). */
async function peerOf(me, publicId) {
  const peer = await User.findOne({ where: { publicId, status: 'active' } });
  if (!peer) throw notFound('user_not_found', 'Not found');
  const hostSide = me.role === 'host' ? me : peer;
  const userSide = me.role === 'host' ? peer : me;
  if (hostSide.role !== 'host' || hostSide.hostStatus !== 'approved' || userSide.role === 'host') throw notFound('user_not_found', 'Not found');
  if (!(await canInteract(me, peer)).ok) throw notFound('user_not_found', 'Not found'); // blocked looks like "does not exist"
  return { peer, pair: { userId: userSide.id, hostId: hostSide.id } };
}

function messageView(m, meId, gifts) {
  return {
    id: Number(m.id), mine: String(m.senderId) === String(meId), kind: m.kind, text: m.text || null,
    gift: m.kind === 'gift' ? giftView(gifts.get(String(m.giftId))) : null,
    quantity: m.quantity || 1, coins: m.coins, at: m.createdAt, read: !!m.readAt, clientId: m.clientId || null,
  };
}
async function giftsFor(msgs) {
  const ids = [...new Set(msgs.filter((m) => m.giftId).map((m) => m.giftId))];
  const rows = ids.length ? await Gift.findAll({ where: { id: ids } }) : [];
  return new Map(rows.map((g) => [String(g.id), g]));
}

/** Hosts never pay for messages; VIP users neither (VIP free chat). */
const noCharge = (u) => u.role === 'host' || require('../services/vip').isVip(u);

/** What the user still gets free in this chat, and what one message costs after that. */
async function terms(conv, hostId) {
  const freeLimit = Number(await settings.get('free_messages_per_chat'));
  const hp = await HostProfile.findOne({ where: { userId: hostId } });
  return { freeLeft: Math.max(0, freeLimit - (conv ? conv.userFreeUsed : 0)), messagePrice: hp ? hp.messagePrice : Number(await settings.get('host_message_price_default')) };
}

/** Moves coins for one paid message / gift inside a transaction: the sender pays, the host earns its share. Locks both wallets in id order. */
async function charge(t, payerId, hostId, price, kinds, ref, out = {}) {
  const share = Number(await settings.get('host_share_pct'));
  const hostGets = Math.floor((price * share) / 100);
  out.hostGets = hostGets;
  const ids = [payerId, hostId].sort((a, b) => Number(a) - Number(b));
  const w = {};
  for (const id of ids) w[id] = await Wallet.findByPk(id, { transaction: t, lock: t.LOCK.UPDATE });
  const payer = w[payerId]; const host = w[hostId];
  if (Number(payer.coins) < price) throw new ApiError(402, 'not_enough_coins', `You need ${price} coins`);
  await payer.update({ coins: Number(payer.coins) - price }, { transaction: t });
  await host.update({ earnings: Number(host.earnings) + hostGets }, { transaction: t });
  await Transaction.create({ userId: payerId, wallet: 'coins', kind: kinds[0], coins: -price, balanceAfter: Number(payer.coins), ref: { ...ref, hostId: Number(hostId) } }, { transaction: t });
  await Transaction.create({ userId: hostId, wallet: 'earnings', kind: kinds[1], coins: hostGets, balanceAfter: Number(host.earnings), ref: { ...ref, buyerId: Number(payerId), price, sharePct: share } }, { transaction: t });
  return Number(payer.coins);
}

/** Finds the chat of a pair or starts it. A host starting chats is limited per day (admin setting) against spam. */
async function openConversation(t, me, pair) {
  let conv = await Conversation.findOne({ where: pair, transaction: t, lock: t.LOCK.UPDATE });
  if (conv) return conv;
  if (me.role === 'host') {
    const limit = Number(await settings.get('host_new_chats_per_day'));
    const since = new Date(); since.setHours(0, 0, 0, 0);
    const started = await Conversation.count({ where: { hostId: me.id, startedBy: me.id, createdAt: { [Op.gte]: since } }, transaction: t });
    if (started >= limit) throw forbidden('daily_chat_limit', 'You have started the most new chats allowed today. You can still reply to your chats.');
  }
  try {
    conv = await Conversation.create({ ...pair, startedBy: me.id }, { transaction: t });
  } catch (e) {
    if (e.name !== 'SequelizeUniqueConstraintError') throw e;
    conv = await Conversation.findOne({ where: pair, transaction: t, lock: t.LOCK.UPDATE });
  }
  return conv;
}

/** Tells both people's open apps about a new message (the sender's other phones too). */
async function pushMessage(m, sender, peer, conv) {
  const gifts = await giftsFor([m]);
  const card = await publicUser(sender);
  for (const [who, other] of [[peer, sender], [sender, peer]]) {
    realtime.emitToUser(who.id, 'chat_message', {
      peerId: Number(other.publicId), message: messageView(m, who.id, gifts), from: String(who.id) === String(peer.id) ? card : null,
      unread: String(who.id) === String(conv.userId) ? conv.userUnread : conv.hostUnread,
    });
  }
}

// ---------------------------------------------------------------- the chat list
r.get('/chats', requireAuth, wrap(async (req, res) => {
  const me = req.user;
  const mine = me.role === 'host' ? 'hostId' : 'userId';
  const convs = await Conversation.findAll({ where: { [mine]: me.id, lastMessageId: { [Op.ne]: null } }, order: [['lastAt', 'DESC']], limit: 200 });
  const peerIds = convs.map((c) => (me.role === 'host' ? c.userId : c.hostId));
  const { cards } = require('./social');
  const users = await User.findAll({ where: { id: peerIds, status: 'active' } });
  const blocked = await require('../models').Block.findAll({ where: { [Op.or]: [{ blockerId: me.id }, { blockedId: me.id }] }, attributes: ['blockerId', 'blockedId'] });
  const hide = new Set(blocked.map((b) => String(String(b.blockerId) === String(me.id) ? b.blockedId : b.blockerId)));
  const byId = new Map((await cards(users.filter((u) => !hide.has(String(u.id))), me)).map((c, i) => [c.id, c]));
  const userById = new Map(users.map((u) => [String(u.id), u]));
  const lasts = await Message.findAll({ where: { id: convs.map((c) => c.lastMessageId) } });
  const lastById = new Map(lasts.map((m) => [String(m.id), m]));
  const gifts = await giftsFor(lasts);
  const out = [];
  for (const c of convs) {
    const u = userById.get(String(me.role === 'host' ? c.userId : c.hostId));
    const card = u && byId.get(Number(u.publicId));
    if (!card) continue;
    const last = lastById.get(String(c.lastMessageId));
    out.push({ peer: card, last: last ? messageView(last, me.id, gifts) : null, unread: me.role === 'host' ? c.hostUnread : c.userUnread, lastAt: c.lastAt });
  }
  res.json({ chats: out, unreadTotal: out.reduce((n, c) => n + c.unread, 0) });
}));

r.get('/chats/unread', requireAuth, wrap(async (req, res) => {
  const col = req.user.role === 'host' ? 'hostUnread' : 'userUnread';
  const n = await Conversation.sum(col, { where: { [req.user.role === 'host' ? 'hostId' : 'userId']: req.user.id } });
  res.json({ unread: Number(n || 0) });
}));

// ---------------------------------------------------------------- one chat
r.get('/chats/:peerId/messages', requireAuth, wrap(async (req, res) => {
  const { peer, pair } = await peerOf(req.user, pid.parse(req.params.peerId));
  const before = req.query.before ? pid.parse(req.query.before) : null;
  const conv = await Conversation.findOne({ where: pair });
  const PAGE = 40;
  const rows = conv ? await Message.findAll({ where: { conversationId: conv.id, ...(before ? { id: { [Op.lt]: before } } : {}) }, order: [['id', 'DESC']], limit: PAGE + 1 }) : [];
  const page = rows.slice(0, PAGE).reverse();
  const gifts = await giftsFor(page);
  const t = await terms(conv, pair.hostId);
  const [card] = await require('./social').cards([peer], req.user);
  res.json({
    peer: card, messages: page.map((m) => messageView(m, req.user.id, gifts)), more: rows.length > PAGE,
    // only the user pays for messages; a host's own messages are always free
    // hosts never pay for messages, VIP users neither (free chat): no "free messages left" line for them
    freeLeft: noCharge(req.user) ? null : t.freeLeft, messagePrice: noCharge(req.user) ? 0 : t.messagePrice,
  });
}));

const sendLimit = rateLimit({ windowMs: 60_000, limit: 60, standardHeaders: true, legacyHeaders: false, handler: rateLimitHandler, keyGenerator: (req) => 'u' + req.user.id }); // always after requireAuth: limited per person

r.post('/chats/:peerId/messages', requireAuth, sendLimit, wrap(async (req, res) => {
  const body = z.object({ text: z.string().trim().min(1).max(1000), clientId: z.string().trim().min(6).max(40) }).strict().parse(req.body);
  const me = req.user;
  const { peer, pair } = await peerOf(me, pid.parse(req.params.peerId));
  const earlier = await Message.findOne({ where: { senderId: me.id, clientId: body.clientId } });
  if (earlier) { // the phone sent it again (bad network): same message, never charged twice
    const conv = await Conversation.findByPk(earlier.conversationId);
    return res.json({ message: messageView(earlier, me.id, new Map()), coins: Number((await Wallet.findByPk(me.id)).coins), freeLeft: noCharge(me) ? null : (await terms(conv, pair.hostId)).freeLeft });
  }
  const out = await sequelize.transaction(async (t) => {
    const conv = await openConversation(t, me, pair);
    let price = 0; let coins = null;
    if (!noCharge(me)) {
      const freeLimit = Number(await settings.get('free_messages_per_chat'));
      if (conv.userFreeUsed < freeLimit) await conv.update({ userFreeUsed: conv.userFreeUsed + 1 }, { transaction: t });
      else {
        const hp = await HostProfile.findOne({ where: { userId: pair.hostId }, transaction: t });
        price = hp ? hp.messagePrice : Number(await settings.get('host_message_price_default'));
      }
    }
    const m = await Message.create({ conversationId: conv.id, senderId: me.id, kind: 'text', text: body.text, coins: price, clientId: body.clientId }, { transaction: t });
    if (price > 0) coins = await charge(t, me.id, pair.hostId, price, ['message_spend', 'message_earn'], { messageId: Number(m.id) });
    const unreadCol = String(peer.id) === String(conv.userId) ? 'userUnread' : 'hostUnread';
    await conv.update({ lastMessageId: m.id, lastAt: m.createdAt, [unreadCol]: conv[unreadCol] + 1 }, { transaction: t });
    return { m, conv, coins };
  });
  await pushMessage(out.m, me, peer, out.conv);
  { const levels = require('../services/levels'); await levels.addXp(me.id, await levels.xpFor('xp_message')); }
  const coins = out.coins !== null ? out.coins : Number((await Wallet.findByPk(me.id)).coins);
  res.json({ message: messageView(out.m, me.id, new Map()), coins, freeLeft: noCharge(me) ? null : (await terms(out.conv, pair.hostId)).freeLeft });
}));

r.post('/chats/:peerId/read', requireAuth, wrap(async (req, res) => {
  const { peer, pair } = await peerOf(req.user, pid.parse(req.params.peerId));
  const conv = await Conversation.findOne({ where: pair });
  if (!conv) return res.json({ ok: true });
  const mineCol = String(req.user.id) === String(conv.userId) ? 'userUnread' : 'hostUnread';
  await conv.update({ [mineCol]: 0 });
  const [n] = await Message.update({ readAt: new Date() }, { where: { conversationId: conv.id, senderId: peer.id, readAt: null } });
  if (n) realtime.emitToUser(peer.id, 'chat_read', { peerId: Number(req.user.publicId) });
  res.json({ ok: true });
}));

// ---------------------------------------------------------------- gifts
r.get('/gifts', requireAuth, wrap(async (req, res) => {
  const rows = await Gift.findAll({ where: { active: true }, order: [['sort', 'ASC'], ['price', 'ASC']] });
  res.json({ gifts: rows.map(giftView) });
}));

/** A user sends a gift to a host (from a chat, a profile or a call). It also shows up in their chat, and both apps play it at once. */
r.post('/gifts/send', requireAuth, sendLimit, wrap(async (req, res) => {
  const body = z.object({
    toId: z.number().int().positive(), giftId: z.number().int().positive(), quantity: z.number().int().min(1).max(99).default(1),
    context: z.enum(['chat', 'profile', 'call']), clientId: z.string().trim().min(6).max(40),
  }).strict().parse(req.body);
  const me = req.user;
  if (me.role === 'host') throw forbidden('hosts_cannot_send_gifts', 'Hosts receive gifts; they cannot send them');
  const { peer, pair } = await peerOf(me, body.toId);
  const gift = await Gift.findOne({ where: { id: body.giftId, active: true } });
  if (!gift) throw notFound('gift_not_found', 'This gift is not available any more');
  const earlier = await Message.findOne({ where: { senderId: me.id, clientId: body.clientId } });
  if (earlier) return res.json({ message: messageView(earlier, me.id, await giftsFor([earlier])), coins: Number((await Wallet.findByPk(me.id)).coins), alreadySent: true });
  const out = await sequelize.transaction(async (t) => {
    const conv = await openConversation(t, me, pair);
    const total = gift.price * body.quantity;
    const m = await Message.create({ conversationId: conv.id, senderId: me.id, kind: 'gift', giftId: gift.id, quantity: body.quantity, coins: total, clientId: body.clientId }, { transaction: t });
    const share = {};
    const coins = await charge(t, me.id, pair.hostId, total, ['gift_spend', 'gift_earn'], { giftId: gift.id, giftName: gift.name, quantity: body.quantity, messageId: Number(m.id), context: body.context }, share);
    await conv.update({ lastMessageId: m.id, lastAt: m.createdAt, hostUnread: conv.hostUnread + 1 }, { transaction: t });
    return { m, conv, coins, hostGets: share.hostGets };
  });
  require('../services/ringing').giftDuringCall(me.id, pair.hostId, out.hostGets, body.quantity);
  { const levels = require('../services/levels'); await levels.addXp(me.id, await levels.xpFor('xp_gift_per_10_coins', Math.floor((gift.price * body.quantity) / 10))); }
  await require('../services/notify').notify(pair.hostId, 'gift', `${me.name || 'Someone'} sent you ${body.quantity > 1 ? `${body.quantity}× ` : 'a '}${gift.name}`, `+${out.hostGets} coins`, { peerId: Number(me.publicId) });
  // both apps play the animation; the id lets the sender's phone skip the copy it already played
  const event = { id: Number(out.m.id), from: await publicUser(me), to: Number(peer.publicId), gift: giftView(gift), quantity: body.quantity, context: body.context };
  realtime.emitToUser(peer.id, 'gift', event);
  realtime.emitToUser(me.id, 'gift', event);
  await pushMessage(out.m, me, peer, out.conv);
  res.json({ message: messageView(out.m, me.id, new Map([[String(gift.id), gift]])), coins: out.coins, gift: event });
}));

/** Puts "Missed video call" / "Video call · 06:24" into the chat of a call's two people (called by the call service). */
async function callLine(call, reason, seconds) {
  const [user, host] = await Promise.all([User.findByPk(call.userId), User.findByPk(call.hostId)]);
  if (!user || !host) return;
  const pair = { userId: user.id, hostId: host.id };
  const text = seconds > 0 ? String(seconds) : ['declined', 'cancelled'].includes(reason) ? reason : 'missed';
  const hostRang = String(call.callerId || call.userId) === String(call.hostId);
  const missed = text === 'missed';
  const out = await sequelize.transaction(async (t) => {
    let conv = await Conversation.findOne({ where: pair, transaction: t, lock: t.LOCK.UPDATE });
    if (!conv) conv = await Conversation.create({ ...pair, startedBy: user.id }, { transaction: t });
    const m = await Message.create({ conversationId: conv.id, senderId: hostRang ? host.id : user.id, kind: 'call', text }, { transaction: t });
    const unread = !missed ? {} : hostRang ? { userUnread: conv.userUnread + 1 } : { hostUnread: conv.hostUnread + 1 };
    await conv.update({ lastMessageId: m.id, lastAt: m.createdAt, ...unread }, { transaction: t });
    return { m, conv };
  });
  await pushMessage(out.m, user, host, out.conv);
}

module.exports = r;
module.exports.callLine = callLine;
module.exports.giftView = giftView;
module.exports.peerOf = peerOf;
