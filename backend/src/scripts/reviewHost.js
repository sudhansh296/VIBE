// Decide host applications from the command line until the admin panel (Phase 6) exists.
//   npm run host:review                         -> list applications waiting for a decision
//   npm run host:review -- 24541195 approve      -> approve (the person becomes a host)
//   npm run host:review -- 24541195 reject "Selfie is not clear"
require('dotenv').config();
const M = require('../models');
const hostReview = require('../services/hostReview');
const pii = require('../utils/pii');

(async () => {
  const [id, action, ...rest] = process.argv.slice(2);
  if (!id) {
    const users = await M.User.findAll({ where: { hostStatus: 'pending' } });
    if (!users.length) console.log('No applications waiting.');
    for (const u of users) {
      const a = await M.HostApplication.findByPk(u.id);
      const media = await M.Media.findAll({ where: { userId: u.id } });
      console.log(`${u.publicId}  ${u.name || '-'}  ${u.phone}  photos ${media.filter((m) => m.kind === 'photo').length}  videos ${media.filter((m) => m.kind === 'video').length}` +
        `  PAN ${a && a.panEnc ? pii.maskPan(pii.decrypt(a.panEnc)) : '-'}  ID ${a && a.idType ? a.idType : '-'} ${a && a.idNumberEnc ? pii.decrypt(a.idNumberEnc) : ''}  sent ${a && a.submittedAt ? a.submittedAt.toISOString().slice(0, 16) : '-'}`);
    }
  } else {
    const u = await M.User.findOne({ where: { publicId: Number(id) } });
    if (!u) throw new Error(`No user with id ${id}`);
    if (action === 'approve') { await hostReview.approve(u); console.log(`${id} approved: now a host`); }
    else if (action === 'reject') {
      const reason = rest.join(' ').trim();
      if (reason.length < 3) throw new Error('Give a reason, e.g. reject "Selfie is not clear"');
      await hostReview.reject(u, reason); console.log(`${id} rejected: ${reason}`);
    } else throw new Error('Use approve or reject');
  }
  await M.sequelize.close();
})().catch(async (e) => { console.error('Error:', e.message); await M.sequelize.close(); process.exit(1); });
