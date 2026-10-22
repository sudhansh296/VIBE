class ApiError extends Error {
  constructor(status, code, message) {
    super(message || code);
    this.status = status;
    this.code = code;
  }
}
const bad = (code, msg) => new ApiError(400, code, msg);
const unauthorized = (code = 'unauthorized', msg = 'Please log in again') => new ApiError(401, code, msg);
const forbidden = (code = 'forbidden', msg = 'Not allowed') => new ApiError(403, code, msg);
const notFound = (code = 'not_found', msg = 'Not found') => new ApiError(404, code, msg);
const conflict = (code, msg) => new ApiError(409, code, msg);

// Express 4 does not catch rejected promises: wrap every async handler with this.
const wrap = (fn) => (req, res, next) => Promise.resolve(fn(req, res, next)).catch(next);

// express-rate-limit answers in the same JSON shape as every other error
const rateLimitHandler = (req, res) => res.status(429).json({ error: { code: 'too_many_requests', message: 'Too many tries, wait a few minutes' } });

module.exports = { rateLimitHandler, ApiError, bad, unauthorized, forbidden, notFound, conflict, wrap };
