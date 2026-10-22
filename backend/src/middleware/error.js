const { ZodError } = require('zod');
const { ApiError } = require('../utils/errors');
const cfg = require('../config');

// Last stop for every error. The app only ever sees { error: { code, message } } and never a stack trace.
function errorHandler(err, req, res, next) { // eslint-disable-line no-unused-vars
  if (err instanceof ZodError) {
    return res.status(400).json({ error: { code: 'invalid_input', message: 'Some fields are not valid', fields: err.issues.map((i) => ({ field: i.path.join('.'), message: i.message })) } });
  }
  if (err instanceof ApiError) return res.status(err.status).json({ error: { code: err.code, message: err.message } });
  if (err && err.type === 'entity.too.large') return res.status(413).json({ error: { code: 'too_large', message: 'Request is too large' } });
  if (err && err.type === 'entity.parse.failed') return res.status(400).json({ error: { code: 'bad_json', message: 'Invalid JSON' } });
  if (err && err.code === 'LIMIT_FILE_SIZE') return res.status(413).json({ error: { code: 'file_too_large', message: 'File is too large' } });
  console.error('[error]', req.method, req.originalUrl, cfg.isProd ? err.message : err);
  res.status(500).json({ error: { code: 'server_error', message: 'Something went wrong' } });
}

function notFoundHandler(req, res) {
  res.status(404).json({ error: { code: 'not_found', message: 'Not found' } });
}

module.exports = { errorHandler, notFoundHandler };
