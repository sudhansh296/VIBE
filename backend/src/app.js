const express = require('express');
const helmet = require('helmet');
const cors = require('cors');
const rateLimit = require('express-rate-limit');
const cfg = require('./config');
const settings = require('./services/settings');
const { wrap, rateLimitHandler } = require('./utils/errors');
const { errorHandler, notFoundHandler } = require('./middleware/error');

const app = express();
app.disable('x-powered-by');
app.set('trust proxy', 1);

app.use(helmet());
// the Android app does not need CORS; browsers (admin panel later) get an explicit allow-list then
app.use(cors({ origin: false }));
// Razorpay webhook: needs the exact raw bytes to check its signature, so it is parsed before the JSON parser
const walletRoutes = require('./routes/wallet');
app.post('/webhooks/razorpay', express.raw({ type: '*/*', limit: '200kb' }), walletRoutes.webhook);
app.use(express.json({ limit: '100kb' }));
app.use(rateLimit({ windowMs: 15 * 60_000, limit: 600, standardHeaders: true, legacyHeaders: false, handler: rateLimitHandler }));

// uploaded files: served as plain files, never executed or sniffed as HTML
// only profile pictures (DP) are public files; photos, videos, selfies and IDs are in private folders the server hands out after a check
app.use('/uploads/dp', express.static(require('path').join(cfg.uploadDir, 'dp'), {
  dotfiles: 'deny', index: false, fallthrough: false,
  setHeaders: (res) => { res.set('X-Content-Type-Options', 'nosniff'); res.set('Content-Security-Policy', "default-src 'none'"); },
}));

// gift pictures and animations (made in the admin panel) are public files, like profile pictures
app.use('/uploads/gifts', express.static(require('path').join(cfg.uploadDir, 'gifts'), {
  dotfiles: 'deny', index: false, fallthrough: false, maxAge: '7d',
  setHeaders: (res) => { res.set('X-Content-Type-Options', 'nosniff'); res.set('Content-Security-Policy', "default-src 'none'"); },
}));

app.get('/', (req, res) => res.json({ name: 'VIBE backend', ok: true, health: '/health' }));
app.get('/health', (req, res) => res.json({ ok: true }));
// settings the app is allowed to know (min age, languages, maintenance, min version ...)
app.get('/config', wrap(async (req, res) => res.json({ config: await settings.publicConfig() })));

app.use('/auth', require('./routes/auth'));
app.use('/me', require('./routes/me'));
app.use('/', require('./routes/prefs'));
app.use('/', require('./routes/media'));
app.use('/', require('./routes/host'));
app.use('/', require('./routes/admin'));
app.use('/', require('./routes/social'));
app.use('/', require('./routes/safety'));
app.use('/', walletRoutes);
app.use('/', require('./routes/chat'));
app.use('/', require('./routes/calls'));
app.use('/', require('./routes/notifications'));
app.use('/', require('./routes/payouts'));
app.use('/', require('./routes/rewards'));
app.use('/', require('./routes/stories'));
app.use('/', require('./routes/support'));

app.use(notFoundHandler);
app.use(errorHandler);

module.exports = app;
