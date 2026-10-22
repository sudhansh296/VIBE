// Age in whole years on today's date (UTC). dob is "YYYY-MM-DD".
function ageFromDob(dob, now = new Date()) {
  const [y, m, d] = String(dob).split('-').map(Number);
  let age = now.getUTCFullYear() - y;
  if (now.getUTCMonth() + 1 < m || (now.getUTCMonth() + 1 === m && now.getUTCDate() < d)) age -= 1;
  return age;
}
module.exports = { ageFromDob };
