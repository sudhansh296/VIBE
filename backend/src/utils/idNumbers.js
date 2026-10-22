// ID document numbers: format checks per document type and what may be stored.
// Aadhaar: by law we keep only the last 4 digits (stored as XXXX XXXX 1234); the full number never reaches the database.

// Verhoeff checksum (the last digit of every Aadhaar number)
const D = [
  [0, 1, 2, 3, 4, 5, 6, 7, 8, 9], [1, 2, 3, 4, 0, 6, 7, 8, 9, 5], [2, 3, 4, 0, 1, 7, 8, 9, 5, 6], [3, 4, 0, 1, 2, 8, 9, 5, 6, 7],
  [4, 0, 1, 2, 3, 9, 5, 6, 7, 8], [5, 9, 8, 7, 6, 0, 4, 3, 2, 1], [6, 5, 9, 8, 7, 1, 0, 4, 3, 2], [7, 6, 5, 9, 8, 2, 1, 0, 4, 3],
  [8, 7, 6, 5, 9, 3, 2, 1, 0, 4], [9, 8, 7, 6, 5, 4, 3, 2, 1, 0],
];
const P = [
  [0, 1, 2, 3, 4, 5, 6, 7, 8, 9], [1, 5, 7, 6, 2, 8, 3, 0, 9, 4], [5, 8, 0, 3, 7, 9, 6, 1, 4, 2], [8, 9, 1, 6, 0, 4, 3, 5, 2, 7],
  [9, 4, 5, 3, 1, 2, 6, 8, 7, 0], [4, 2, 8, 6, 5, 7, 3, 9, 0, 1], [2, 7, 9, 3, 8, 0, 6, 4, 1, 5], [7, 0, 4, 6, 9, 1, 3, 2, 5, 8],
];
function verhoeffOk(num) {
  let c = 0;
  num.split('').reverse().forEach((ch, i) => { c = D[c][P[i % 8][Number(ch)]]; });
  return c === 0;
}

const RULES = {
  aadhaar: { re: /^[2-9][0-9]{11}$/, extra: verhoeffOk, hint: 'Aadhaar is 12 digits' },
  driving_licence: { re: /^[A-Z]{2}[0-9A-Z]{11,14}$/, hint: 'Driving licence looks like MH1420110062821' },
  voter_id: { re: /^[A-Z]{3}[0-9]{7}$/, hint: 'Voter ID is 3 letters and 7 digits' },
  passport: { re: /^[A-Z][0-9]{7}$/, hint: 'Passport is 1 letter and 7 digits' },
};

/** Cleans spaces / dashes and checks the format. Returns { ok, value (what to store), hint }. */
function check(type, raw) {
  const rule = RULES[type];
  const n = String(raw || '').toUpperCase().replace(/[\s-]/g, '');
  if (!rule) return { ok: n.length >= 4 && n.length <= 20 && /^[A-Z0-9]+$/.test(n), value: n, hint: 'Enter the number on your ID' };
  if (!rule.re.test(n) || (rule.extra && !rule.extra(n))) return { ok: false, hint: rule.hint };
  return { ok: true, value: type === 'aadhaar' ? `XXXX XXXX ${n.slice(-4)}` : n };
}

/** For the applicant's own screen: enough to recognise it, not the whole number. */
function mask(stored) {
  if (!stored) return null;
  if (stored.startsWith('XXXX')) return stored;
  return stored.slice(0, 2) + '*'.repeat(Math.max(stored.length - 4, 2)) + stored.slice(-2);
}

module.exports = { check, mask, verhoeffOk };
