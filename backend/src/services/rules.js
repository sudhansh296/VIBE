const { Op } = require('sequelize');
const { Block } = require('../models');

/**
 * Who may interact with whom. Every list, call, chat and gift in the later phases must go through this, on the server:
 *  - a host sees only users, a user sees all hosts, and there is NO host-to-host contact of any kind;
 *  - a blocked pair can never contact each other (either direction);
 *  - only active accounts take part.
 * `a` and `b` are User rows (or plain objects with id, role, status).
 */
async function canInteract(a, b) {
  if (!a || !b || a.id === b.id) return { ok: false, reason: 'self' };
  if (a.status !== 'active' || b.status !== 'active') return { ok: false, reason: 'inactive' };
  if (a.role === 'host' && b.role === 'host') return { ok: false, reason: 'host_to_host' };
  const blocked = await Block.findOne({
    where: { [Op.or]: [{ blockerId: a.id, blockedId: b.id }, { blockerId: b.id, blockedId: a.id }] },
    attributes: ['id'],
  });
  if (blocked) return { ok: false, reason: 'blocked' };
  return { ok: true };
}

/** Role filter for "who can I discover" lists: a user discovers hosts, a host discovers users (never other hosts). */
function discoverRoleFor(viewer) {
  return viewer.role === 'host' ? 'user' : 'host';
}

module.exports = { canInteract, discoverRoleFor };
