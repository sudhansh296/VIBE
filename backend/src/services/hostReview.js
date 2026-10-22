const { Media, HostApplication, HostProfile } = require('../models');
const settings = require('./settings');
const { bad, conflict } = require('../utils/errors');

/** The admin's decision on a host application. Used by the admin API and by the review command (until the admin panel exists). */
async function approve(u, reviewerId) {
  if (u.hostStatus !== 'pending') throw conflict('not_pending', 'This application is not waiting for a decision');
  const { limits, syncDpFromMain } = require('../routes/media');
  const l = await limits();
  const photos = await Media.count({ where: { userId: u.id, kind: 'photo' } });
  const videos = await Media.count({ where: { userId: u.id, kind: 'video' } });
  if (photos < l.minPhotos || videos < l.minVideos) throw bad('media_missing', 'The applicant has fewer photos or videos than required');
  const a = await HostApplication.findByPk(u.id);
  await u.update({ role: 'host', hostStatus: 'approved', bio: a.bio || u.bio, languages: a.languages || u.languages });
  await HostProfile.findOrCreate({
    where: { userId: u.id },
    defaults: { callRate: Number(await settings.get('host_call_rate_default')), messagePrice: Number(await settings.get('host_message_price_default')) },
  });
  await a.update({ reviewedAt: new Date(), reviewedBy: reviewerId || null, rejectReason: null });
  await syncDpFromMain(u);
}

async function reject(u, reason, reviewerId) {
  if (u.hostStatus !== 'pending') throw conflict('not_pending', 'This application is not waiting for a decision');
  const a = await HostApplication.findByPk(u.id);
  await a.update({ rejectReason: reason, reviewedAt: new Date(), reviewedBy: reviewerId || null });
  await u.update({ hostStatus: 'rejected' });
}

module.exports = { approve, reject };
