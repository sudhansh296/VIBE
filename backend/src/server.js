const http = require('http');
const cfg = require('./config');
const app = require('./app');
const { sequelize } = require('./models');
const { seedSettings } = require('./services/settings');
const realtime = require('./realtime');

async function start() {
  await sequelize.authenticate();
  await seedSettings();
  await realtime.resetPresence();
  const server = http.createServer(app);
  realtime.init(server);
  server.listen(cfg.port, () => {
    console.log(`VIBE backend on ${cfg.publicUrl}  (dev login: ${cfg.devAuth ? 'ON' : 'off'}, realtime: ON)`);
  });
  const stop = async () => {
    await realtime.resetPresence(); // everybody is offline once the server is gone
    await realtime.close();
    server.close(() => sequelize.close().then(() => process.exit(0)));
  };
  process.on('SIGINT', stop);
  process.on('SIGTERM', stop);
}

start().catch((e) => { console.error('Could not start:', e.message); process.exit(1); });
