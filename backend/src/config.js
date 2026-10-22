require('dotenv').config({ path: require('path').join(__dirname, '..', '.env') });

const env = process.env;
const isProd = env.NODE_ENV === 'production';

function need(name) {
  const v = env[name];
  if (!v) throw new Error(`Missing ${name} in .env`);
  return v;
}

module.exports = {
  isProd,
  port: Number(env.PORT || 4000),
  publicUrl: (env.PUBLIC_URL || 'http://localhost:4000').replace(/\/$/, ''),
  db: {
    host: env.DB_HOST || '127.0.0.1',
    port: Number(env.DB_PORT || 3306),
    name: env.DB_NAME || 'vibe_callme',
    user: env.DB_USER || 'root',
    pass: env.DB_PASS || '',
  },
  jwt: {
    accessSecret: need('JWT_ACCESS_SECRET'),
    refreshSecret: need('JWT_REFRESH_SECRET'),
    accessTtl: env.ACCESS_TTL || '15m',
    refreshDays: Number(env.REFRESH_TTL_DAYS || 30),
  },
  piiKey: need('PII_KEY'),
  firebaseServiceAccount: env.FIREBASE_SERVICE_ACCOUNT || '',
  // the dev login can never be on in production
  devAuth: !isProd && env.DEV_AUTH === 'true',
  // logins per IP per 15 minutes (bots); tests raise it
  loginRateLimit: Number(env.LOGIN_RATE_LIMIT || 20),
  uploadDir: require('path').join(__dirname, '..', 'uploads'),
  // coin payments. Without keys (and never in production) a pretend Razorpay is used so everything can be built and tested.
  razorpay: {
    keyId: env.RAZORPAY_KEY_ID || '',
    keySecret: env.RAZORPAY_KEY_SECRET || '',
    webhookSecret: env.RAZORPAY_WEBHOOK_SECRET || '',
    fake: !isProd && !env.RAZORPAY_KEY_ID,
  },
};
