// Normalises an Indian mobile number to E.164 (+91XXXXXXXXXX). Returns null if it is not a valid one.
function normalizePhone(input) {
  if (typeof input !== 'string') return null;
  const digits = input.replace(/[^\d]/g, '');
  let n = digits;
  if (n.length === 12 && n.startsWith('91')) n = n.slice(2);
  if (n.length === 11 && n.startsWith('0')) n = n.slice(1);
  if (!/^[6-9]\d{9}$/.test(n)) return null;
  return '+91' + n;
}
module.exports = { normalizePhone };
