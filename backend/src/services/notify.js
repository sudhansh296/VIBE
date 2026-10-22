const { Notification } = require('../models');

const view = (n) => ({ id: Number(n.id), kind: n.kind, title: n.title, body: n.body || null, data: n.data || {}, at: n.createdAt, read: !!n.readAt });

/** Adds a line to [userId]'s bell list and tells the app at once if it is open. Never throws (a notice is never worth a failed action). */
async function notify(userId, kind, title, body = null, data = {}) {
  try {
    const n = await Notification.create({ userId, kind, title: String(title).slice(0, 120), body: body ? String(body).slice(0, 300) : null, data });
    require('../realtime').emitToUser(userId, 'notification', view(n));
    return n;
  } catch (e) { console.error('[notify]', e.message); return null; }
}

module.exports = { notify, view };
