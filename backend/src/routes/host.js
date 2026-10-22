const express = require('express');
const multer = require('multer');
const { z } = require('zod');
const { User, HostApplication, Media } = require('../models');
const settings = require('../services/settings');
const { requireAuth, requireRole } = require('../middleware/auth');
const files = require('../utils/files');
const pii = require('../utils/pii');
const idNumbers = require('../utils/idNumbers');
const { bad, forbidden, notFound, conflict, wrap } = require('../utils/errors');
const { limits, syncDpFromMain } = require('./media');

const r = express.Router();
const PAN = /^[A-Z]{5}[0-9]{4}[A-Z]$/;

const upload = multer({ storage: multer.memoryStorage(), limits: { fileSize: 8 * 1024 * 1024, files: 1 } });

async function getApp(userId) {
  const [a] = await HostApplication.findOrCreate({ where: { userId } });
  return a;
}

/** What the applicant may know about their own application (the PAN is shown masked, files only as yes / no). */
async function shape(user, a) {
  const l = await limits();
  const photos = await Media.count({ where: { userId: user.id, kind: 'photo' } });
  const videos = await Media.count({ where: { userId: user.id, kind: 'video' } });
  const bioMin = Number(await settings.get('host_bio_min_chars'));
  const idTypes = await settings.get('host_id_types');
  let pan = null;
  try { pan = a.panEnc ? pii.maskPan(pii.decrypt(a.panEnc)) : null; } catch { /* unreadable: treated as missing */ }
  let idNo = null;
  try { idNo = a.idNumberEnc ? idNumbers.mask(pii.decrypt(a.idNumberEnc)) : null; } catch { /* unreadable: treated as missing */ }
  const missing = [];
  if (photos < l.minPhotos) missing.push(`at least ${l.minPhotos} photos`);
  if (videos < l.minVideos) missing.push(`${l.minVideos} intro video`);
  if (!a.bio || a.bio.trim().length < bioMin) missing.push(`a bio of ${bioMin}+ characters`);
  if (!a.languages || !a.languages.length) missing.push('a language');
  if (!a.selfiePath) missing.push('a live selfie');
  if (!a.panName || !a.panEnc) missing.push('PAN name and number');
  if (!a.idType || !a.idPath) missing.push('an ID document');
  if (!idNo) missing.push('the ID number');
  return {
    status: user.hostStatus,                       // none | pending | approved | rejected
    rejectReason: user.hostStatus === 'rejected' ? a.rejectReason : null,
    bio: a.bio || '', languages: a.languages || [], panName: a.panName || '', panMasked: pan, idType: a.idType || null, idNumberMasked: idNo,
    hasSelfie: !!a.selfiePath, hasId: !!a.idPath, agencyCode: a.agencyCode || '',
    counts: { photos, videos }, requirements: { minPhotos: l.minPhotos, minVideos: l.minVideos, bioMinChars: bioMin, idTypes },
    missing, canSubmit: missing.length === 0 && ['none', 'rejected'].includes(user.hostStatus),
    submittedAt: a.submittedAt,
  };
}

const editable = (user) => ['none', 'rejected'].includes(user.hostStatus) && user.role !== 'host';

r.get('/host/application', requireAuth, wrap(async (req, res) => {
  res.json({ application: await shape(req.user, await getApp(req.user.id)) });
}));

/** Step by step: any subset of the written fields. Photos / videos go through /me/media. */
r.put('/host/application', requireAuth, wrap(async (req, res) => {
  if (!editable(req.user)) throw forbidden('not_editable', 'The application cannot be changed now');
  const allowedLangs = await settings.get('allowed_languages');
  const idTypes = await settings.get('host_id_types');
  const body = z.object({
    bio: z.string().trim().max(150).optional(),
    languages: z.array(z.string()).min(1).max(6).optional(),
    panName: z.string().trim().min(3).max(60).regex(/^[\p{L}\p{M} .'-]+$/u, 'Use letters only').optional(),
    panNumber: z.string().trim().toUpperCase().regex(PAN, 'Enter a valid PAN like ABCDE1234F').optional(),
    idType: z.string().optional(),
    agencyCode: z.string().trim().toUpperCase().max(20).regex(/^[A-Z0-9]*$/).optional(),
    idNumber: z.string().trim().max(30).optional(),
  }).strict().parse(req.body);
  if (body.languages) {
    const unknown = body.languages.find((x) => !allowedLangs.includes(x));
    if (unknown) throw bad('invalid_language', `${unknown} is not supported`);
  }
  if (body.idType && !idTypes.includes(body.idType)) throw bad('invalid_id_type', 'Choose one of the accepted ID documents');
  const a = await getApp(req.user.id);
  const patch = { bio: body.bio, languages: body.languages, panName: body.panName, idType: body.idType, agencyCode: body.agencyCode };
  if (body.panNumber) patch.panEnc = pii.encrypt(body.panNumber);
  // the ID number is checked against the document type (the phone reads it from the card, the server never trusts that)
  const type = body.idType || a.idType;
  if (body.idNumber !== undefined) {
    if (!type) throw bad('choose_id_type', 'Choose the ID document first');
    const c = idNumbers.check(type, body.idNumber);
    if (!c.ok) throw bad('invalid_id_number', c.hint);
    patch.idNumberEnc = pii.encrypt(c.value);
  } else if (body.idType && body.idType !== a.idType) patch.idNumberEnc = null; // a different document needs its own number
  for (const k of Object.keys(patch)) if (patch[k] === undefined) delete patch[k];
  await a.update(patch);
  res.json({ application: await shape(req.user, a) });
}));

// selfie and ID photo: private files, only an admin can ever open them
async function savePrivate(req, kind) {
  if (!editable(req.user)) throw forbidden('not_editable', 'The application cannot be changed now');
  if (!req.file) throw bad('no_file', 'Choose a photo');
  const t = files.sniff(req.file.buffer);
  if (!t || t.kind !== 'photo') throw bad('not_an_image', 'Only JPG, PNG or WEBP photos');
  const a = await getApp(req.user.id);
  const old = kind === 'selfie' ? a.selfiePath : a.idPath;
  const rel = files.save(`private/${req.user.id}/${kind}_${Date.now()}.${t.ext}`, req.file.buffer);
  await a.update(kind === 'selfie' ? { selfiePath: rel } : { idPath: rel });
  files.remove(old);
  return shape(req.user, a);
}
r.post('/host/application/selfie', requireAuth, upload.single('photo'), wrap(async (req, res) => res.json({ application: await savePrivate(req, 'selfie') })));
r.post('/host/application/id', requireAuth, upload.single('photo'), wrap(async (req, res) => res.json({ application: await savePrivate(req, 'id') })));

r.post('/host/application/submit', requireAuth, wrap(async (req, res) => {
  if (!editable(req.user)) throw forbidden('not_editable', 'The application cannot be submitted now');
  const a = await getApp(req.user.id);
  const s = await shape(req.user, a);
  if (!s.canSubmit) throw bad('application_incomplete', 'Still needed: ' + s.missing.join(', '));
  await a.update({ submittedAt: new Date(), rejectReason: null, reviewedAt: null, reviewedBy: null });
  await req.user.update({ hostStatus: 'pending' });
  res.json({ application: await shape(req.user, a) });
}));

module.exports = r;
module.exports.shape = shape;
