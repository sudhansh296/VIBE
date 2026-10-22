// Creates .env with fresh random secrets if it does not exist yet. Run: node src/scripts/makeEnv.js
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const file = path.join(__dirname, '..', '..', '.env');
if (fs.existsSync(file)) { console.log('.env already exists, left untouched'); process.exit(0); }
const r = () => crypto.randomBytes(48).toString('hex');
fs.writeFileSync(file, `NODE_ENV=development
PORT=4000
PUBLIC_URL=http://localhost:4000

# MySQL (XAMPP default: root, empty password). Change before going live.
DB_HOST=127.0.0.1
DB_PORT=3306
DB_NAME=vibe_callme
DB_USER=root
DB_PASS=

# secrets for our own tokens (never share, never put in the app)
JWT_ACCESS_SECRET=${r()}
JWT_REFRESH_SECRET=${r()}
ACCESS_TTL=15m
REFRESH_TTL_DAYS=30

# Firebase Admin (OTP + Google login). Download the service-account JSON from the Firebase console
# and put its path here. Without it, only the local DEV login below works.
FIREBASE_SERVICE_ACCOUNT=./firebase-service-account.json

# key that encrypts PAN numbers in the database (never share, never change after real data exists)
PII_KEY=${crypto.randomBytes(32).toString('hex')}

# Local testing only: lets you log in with any phone and the code 123456. Ignored when NODE_ENV=production.
DEV_AUTH=true
`);
console.log('.env created');
