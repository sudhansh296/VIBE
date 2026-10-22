const { Sequelize } = require('sequelize');
const cfg = require('./config');

const sequelize = new Sequelize(cfg.db.name, cfg.db.user, cfg.db.pass, {
  host: cfg.db.host,
  port: cfg.db.port,
  dialect: 'mysql',
  logging: false,
  timezone: '+00:00', // everything is stored in UTC
  pool: { max: 10, min: 0, acquire: 15000, idle: 10000 },
  dialectOptions: { connectTimeout: 10000 },
});

module.exports = sequelize;
