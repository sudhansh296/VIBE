const express = require('express');
const fs = require('fs');
const path = require('path');
const multer = require('multer');
const { z } = require('zod');
const cfg = require('../config');
const settings = require('../services/settings');
const auth = require('../services/auth');
const { Wallet } = require('../models');
const { requireAuth } = require('../middleware/auth');
const { bad, forbidden, wrap } = require('../utils/errors');
const { ageFromDob } = require('../utils/age');

const r = express.Router();

/** What the app may see about a user. `own` adds private fields (phone, coins) for the user's own profile only. */
async function publicUser(u, own = false) {
  const base = {
    id: Number(u.publicId),
    name: u.name || null,
    gender: u.gender || null,
    age: u.dob ? ageFromDob(u.dob) : null,
    city: u.city || null,
    bio: u.bio || null,
    languages: u.languages || [],
    dp: u.dpPath ? `${cfg.publicUrl}/uploads/${u.dpPath}` : null,
    role: u.role,
    vip: require('../services/vip').isVip(u),
    level: u.level || 1,
  };
  if (!own) return base;
  const w = await Wallet.findByPk(u.id);
  return {
    ...base,
    phone: u.phone,
    dob: u.dob || null,
    hostStatus: u.hostStatus,
    coins: w ? Number(w.coins) : 0,
    earnings: u.role === 'host' && w ? Number(w.earnings) : undefined,
    profileComplete: !!(u.name && u.gender && u.dob),
    vipUntil: u.vipUntil || null,
    xp: u.xp || 0,
  };
}

r.get('/', requireAuth, wrap(async (req, res) => {
  res.json({ user: await publicUser(req.user, true) });
}));

/** Create / edit the profile. Gender and date of birth can be set only once (they decide who can see whom). */
r.put('/profile', requireAuth, wrap(async (req, res) => {
  const allowed = await settings.get('allowed_languages');
  const maxLangs = Number(await settings.get('max_profile_languages')) || 6;
  const minAge = Number(await settings.get('min_age')) || 18;
  const body = z.object({
    name: z.string().trim().min(2, 'Name is too short').max(24).regex(/^[\p{L}\p{M} .'-]+$/u, 'Use letters only').optional(),
    gender: z.enum(['male', 'female', 'other']).optional(),
    dob: z.string().regex(/^\d{4}-\d{2}-\d{2}$/, 'Use YYYY-MM-DD').optional(),
    city: z.string().trim().max(40).optional(),
    bio: z.string().trim().max(150).optional(),
    languages: z.array(z.string()).max(maxLangs).optional(),
  }).strict().parse(req.body);

  const u = req.user;
  if (body.gender && u.gender && body.gender !== u.gender) throw forbidden('gender_locked', 'Gender cannot be changed');
  if (body.dob && u.dob && body.dob !== u.dob) throw forbidden('dob_locked', 'Date of birth cannot be changed');
  if (body.dob) {
    const d = new Date(body.dob + 'T00:00:00Z');
    if (Number.isNaN(d.getTime()) || d > new Date()) throw bad('invalid_dob', 'Enter a real date of birth');
    if (ageFromDob(body.dob) < minAge) throw forbidden('under_age', `You must be ${minAge} or older to use VIBE`);
  }
  if (body.languages) {
    const bad1 = body.languages.find((l) => !allowed.includes(l));
    if (bad1) throw bad('invalid_language', `${bad1} is not supported`);
    body.languages = [...new Set(body.languages)];
  }
  await u.update(body);
  res.json({ user: await publicUser(u, true) });
}));

// ---------------------------------------------------------------- profile picture (DP)
const upload = multer({ storage: multer.memoryStorage(), limits: { fileSize: 5 * 1024 * 1024, files: 1 } });

// We look at the first bytes of the file, never at the name or the type the phone claims.
function sniffImage(buf) {
  if (buf.length > 3 && buf[0] === 0xff && buf[1] === 0xd8 && buf[2] === 0xff) return 'jpg';
  if (buf.length > 8 && buf.slice(0, 8).equals(Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]))) return 'png';
  if (buf.length > 12 && buf.slice(0, 4).toString('ascii') === 'RIFF' && buf.slice(8, 12).toString('ascii') === 'WEBP') return 'webp';
  return null;
}

r.post('/dp', requireAuth, upload.single('photo'), wrap(async (req, res) => {
  if (!req.file) throw bad('no_file', 'Choose a photo');
  const ext = sniffImage(req.file.buffer);
  if (!ext) throw bad('not_an_image', 'Only JPG, PNG or WEBP photos');
  const rel = `dp/${req.user.id}_${Date.now()}.${ext}`;
  fs.mkdirSync(path.join(cfg.uploadDir, 'dp'), { recursive: true });
  fs.writeFileSync(path.join(cfg.uploadDir, rel), req.file.buffer);
  const old = req.user.dpPath;
  await req.user.update({ dpPath: rel });
  if (old && old.startsWith('dp/')) fs.rm(path.join(cfg.uploadDir, old), { force: true }, () => {});
  res.json({ user: await publicUser(req.user, true) });
}));

/** Delete my account: it stops working at once, every session is logged out. */
r.delete('/', requireAuth, wrap(async (req, res) => {
  // personal details and the photo go at once; the row itself stays (ledger, reports and payouts must keep pointing at it)
  const dp = req.user.dpPath;
  await req.user.update({ status: 'deleted', firebaseUid: null, phone: null, name: null, bio: null, city: null, languages: null, dpPath: null });
  if (dp && dp.startsWith('dp/')) fs.rm(path.join(cfg.uploadDir, dp), { force: true }, () => {});
  await auth.revokeAll(req.user.id);
  res.json({ ok: true });
}));

module.exports = r;
module.exports.publicUser = publicUser;
