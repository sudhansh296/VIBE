const jwt = require('jsonwebtoken');
const { Server } = require('socket.io');
const cfg = require('./config');
const { User, Session, HostProfile } = require('./models');
const ringing = require('./services/ringing');

/**
 * Live connection of every signed-in app. A person is "online" exactly while the app holds a connection to this server:
 * app opened -> online, app closed / killed / internet lost -> offline (the server notices within seconds).
 * The online flag of a host is set ONLY here, never by the phone.
 */
// a short reconnect (lift, network switch) must not flicker; read when used (tests shorten it after this file is loaded)
const graceMs = () => Number(process.env.REALTIME_GRACE_MS || 4000);

let io = null;
const sockets = new Map();   // userId -> Set of socket ids
const timers = new Map();    // userId -> pending "go offline" timer

const key = (id) => String(id);

function isConnected(userId) {
  const s = sockets.get(key(userId));
  return !!(s && s.size);
}

async function setHostOnline(userId, online) {
  const u = await User.findByPk(userId);
  if (!u) return;
  // users: nothing stored, but hosts see them online / offline live (hosts can call users)
  if (u.role !== 'host' || u.hostStatus !== 'approved') { if (io) io.emit('presence', { id: Number(u.publicId), online }); return; }
  const [p] = await HostProfile.findOrCreate({ where: { userId }, defaults: { callRate: 60, messagePrice: 200 } });
  await p.update({ isOnline: online, onlineAt: online ? new Date() : p.onlineAt });
  // everyone who is connected learns about it at once (the Home list updates without a refresh)
  if (io) io.emit('presence', { id: Number(u.publicId), online });
}

function onConnected(userId) {
  const t = timers.get(key(userId));
  if (t) { clearTimeout(t); timers.delete(key(userId)); return; } // came back within the grace time: never went offline
  setHostOnline(userId, true).catch((e) => console.error('[presence]', e.message));
}

function onGone(userId) {
  const t = setTimeout(() => {
    timers.delete(key(userId));
    if (!isConnected(userId)) setHostOnline(userId, false).catch((e) => console.error('[presence]', e.message));
  }, graceMs());
  timers.set(key(userId), t);
}

/** Starts the realtime server on top of the HTTP server. */
function init(httpServer) {
  io = new Server(httpServer, {
    cors: { origin: false },
    maxHttpBufferSize: 100_000,      // messages are tiny; nobody can flood the server with big ones
    pingInterval: 8000, pingTimeout: 8000, // a dead connection (app killed, no network) is noticed in about 16 s
    serveClient: false,
  });

  // same checks as the REST API: a valid token, a session that is still open, an account that is active
  io.use(async (socket, next) => {
    try {
      const token = socket.handshake.auth && socket.handshake.auth.token;
      if (!token) return next(new Error('unauthorized'));
      const payload = jwt.verify(token, cfg.jwt.accessSecret, { algorithms: ['HS256'] });
      const session = payload.sid ? await Session.findByPk(payload.sid, { attributes: ['id', 'userId', 'revokedAt'] }) : null;
      if (!session || session.revokedAt || String(session.userId) !== String(payload.sub)) return next(new Error('session_ended'));
      const user = await User.findByPk(payload.sub);
      if (!user || user.status !== 'active') return next(new Error('unauthorized'));
      socket.data = { userId: String(user.id), sessionId: String(session.id), publicId: Number(user.publicId) };
      next();
    } catch (e) {
      next(new Error(e.name === 'TokenExpiredError' ? 'token_expired' : 'unauthorized'));
    }
  });

  io.on('connection', (socket) => {
    const { userId } = socket.data;
    socket.join(`user:${userId}`);
    socket.join(`session:${socket.data.sessionId}`);
    if (!sockets.has(userId)) sockets.set(userId, new Set());
    sockets.get(userId).add(socket.id);
    if (sockets.get(userId).size === 1) onConnected(userId);
    socket.on('disconnect', () => {
      const set = sockets.get(userId);
      if (!set) return;
      set.delete(socket.id);
      if (set.size === 0) { sockets.delete(userId); onGone(userId); }
    });
    ringing.attach(io, socket, isConnected); // after the bookkeeping above, so isConnected() is up to date on disconnect
    // "typing…" in a chat: forwarded only between two people who may talk; at most one every 2 seconds
    let lastTyping = 0;
    socket.on('chat:typing', async (msg) => {
      const now = Date.now();
      if (now - lastTyping < 2000) return;
      lastTyping = now;
      try {
        const me = await User.findByPk(userId);
        const { peer } = await require('./routes/chat').peerOf(me, Number(msg && msg.peerId));
        io.to(`user:${peer.id}`).emit('chat_typing', { peerId: Number(me.publicId) });
      } catch { /* not allowed or unknown: ignore */ }
    });
  });
  return io;
}

/** Ends the live connection(s) of a login that was just revoked (sign out, "log out of other devices", delete, block). */
function kickSession(sessionId) {
  if (io) io.in(`session:${sessionId}`).disconnectSockets(true);
}
function kickUser(userId) {
  if (io) io.in(`user:${userId}`).disconnectSockets(true);
}

/** At start nobody is connected: clear every online flag left over from a crash or an earlier run. */
async function resetPresence() {
  await HostProfile.update({ isOnline: false }, { where: { isOnline: true } });
}

async function close() {
  for (const t of timers.values()) clearTimeout(t);
  timers.clear(); sockets.clear(); ringing.reset();
  if (io) { await io.close(); io = null; }
}

function port() { return io && io.httpServer ? io.httpServer.address().port : null; }

/** Sends an event to every open app of one person (all their phones). */
function emitToUser(userId, event, payload) {
  if (io) io.to(`user:${userId}`).emit(event, payload);
}

module.exports = { emitToUser, port, init, kickSession, kickUser, isConnected, resetPresence, close };
