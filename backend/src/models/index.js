const { DataTypes } = require('sequelize');
const sequelize = require('../db');

// MariaDB keeps JSON columns as text, so read them back as real values (not strings).
const json = (name, extra = {}) => ({
  type: DataTypes.JSON,
  ...extra,
  get() {
    const v = this.getDataValue(name);
    if (typeof v !== 'string') return v;
    try { return JSON.parse(v); } catch { return v; }
  },
});

/**
 * Phase 1 tables. Money rules: balances live only here; every change of a balance is also written to `transactions`
 * (an append-only ledger). The app can never change any of these numbers.
 */

const User = sequelize.define('User', {
  id: { type: DataTypes.BIGINT.UNSIGNED, autoIncrement: true, primaryKey: true },
  publicId: { type: DataTypes.BIGINT.UNSIGNED, allowNull: false, unique: true },   // the 8-digit "ID: 10159076" shown in the app
  phone: { type: DataTypes.STRING(20), unique: true },                              // E.164, e.g. +919876543210
  firebaseUid: { type: DataTypes.STRING(128), unique: true },
  name: { type: DataTypes.STRING(24) },
  gender: { type: DataTypes.ENUM('male', 'female', 'other') },                      // can be set once
  dob: { type: DataTypes.DATEONLY },                                                // can be set once, must be 18+
  city: { type: DataTypes.STRING(40) },
  bio: { type: DataTypes.STRING(150) },
  languages: json('languages'),
  dpPath: { type: DataTypes.STRING(200) },
  role: { type: DataTypes.ENUM('user', 'host', 'admin'), allowNull: false, defaultValue: 'user' },
  hostStatus: { type: DataTypes.ENUM('none', 'pending', 'approved', 'rejected'), allowNull: false, defaultValue: 'none' },
  vipUntil: { type: DataTypes.DATE },                                            // VIP while this is in the future
  xp: { type: DataTypes.INTEGER.UNSIGNED, allowNull: false, defaultValue: 0 },
  level: { type: DataTypes.INTEGER.UNSIGNED, allowNull: false, defaultValue: 1 },
  checkinStreak: { type: DataTypes.INTEGER.UNSIGNED, allowNull: false, defaultValue: 0 },   // days collected in this 7-day round
  checkinLast: { type: DataTypes.DATEONLY },                                    // last day collected (India time)
  referralCode: { type: DataTypes.STRING(12), unique: true },
  referredBy: { type: DataTypes.BIGINT.UNSIGNED },                              // who invited me
  referralRewarded: { type: DataTypes.BOOLEAN, allowNull: false, defaultValue: false },
  status: { type: DataTypes.ENUM('active', 'blocked', 'deleted'), allowNull: false, defaultValue: 'active' },
  lastSeenAt: { type: DataTypes.DATE },
}, { tableName: 'users', indexes: [{ fields: ['role', 'status'] }] });

const Wallet = sequelize.define('Wallet', {
  userId: { type: DataTypes.BIGINT.UNSIGNED, primaryKey: true },
  coins: { type: DataTypes.BIGINT, allowNull: false, defaultValue: 0 },     // spendable coins (users and hosts)
  earnings: { type: DataTypes.BIGINT, allowNull: false, defaultValue: 0 },  // host earnings in coins, withdrawable
}, { tableName: 'wallets' });

const Transaction = sequelize.define('Transaction', {
  id: { type: DataTypes.BIGINT.UNSIGNED, autoIncrement: true, primaryKey: true },
  userId: { type: DataTypes.BIGINT.UNSIGNED, allowNull: false },
  wallet: { type: DataTypes.ENUM('coins', 'earnings'), allowNull: false, defaultValue: 'coins' },  // which balance this row changed
  kind: { type: DataTypes.STRING(40), allowNull: false },                   // signup_bonus, recharge, media_unlock, media_earn ...
  coins: { type: DataTypes.BIGINT, allowNull: false },                      // + added / - spent
  balanceAfter: { type: DataTypes.BIGINT, allowNull: false },
  ref: json('ref'),
}, { tableName: 'transactions', updatedAt: false, indexes: [{ fields: ['userId', 'createdAt'] }] });

// Coin packs for sale. Prices are in paise (₹49 = 4900); only the admin changes them.
const CoinPack = sequelize.define('CoinPack', {
  id: { type: DataTypes.INTEGER.UNSIGNED, autoIncrement: true, primaryKey: true },
  coins: { type: DataTypes.INTEGER.UNSIGNED, allowNull: false },
  bonusCoins: { type: DataTypes.INTEGER.UNSIGNED, allowNull: false, defaultValue: 0 },
  pricePaise: { type: DataTypes.INTEGER.UNSIGNED, allowNull: false },
  tag: { type: DataTypes.STRING(20) },                       // e.g. "Popular", "Best value"
  active: { type: DataTypes.BOOLEAN, allowNull: false, defaultValue: true },
  sort: { type: DataTypes.INTEGER, allowNull: false, defaultValue: 0 },
}, { tableName: 'coin_packs' });

// One coin purchase. Coins are credited exactly once, when the payment is proven (signature from the app or the Razorpay webhook).
const Payment = sequelize.define('Payment', {
  id: { type: DataTypes.BIGINT.UNSIGNED, autoIncrement: true, primaryKey: true },
  userId: { type: DataTypes.BIGINT.UNSIGNED, allowNull: false },
  packId: { type: DataTypes.INTEGER.UNSIGNED },
  vipPlanId: { type: DataTypes.INTEGER.UNSIGNED },                           // a VIP purchase (then coins = the plan's bonus)
  vipDays: { type: DataTypes.INTEGER.UNSIGNED },
  coins: { type: DataTypes.INTEGER.UNSIGNED, allowNull: false },          // pack coins + bonus, fixed when the order is made
  amountPaise: { type: DataTypes.INTEGER.UNSIGNED, allowNull: false },
  currency: { type: DataTypes.STRING(3), allowNull: false, defaultValue: 'INR' },
  provider: { type: DataTypes.STRING(12), allowNull: false },             // razorpay | fake
  orderId: { type: DataTypes.STRING(60), allowNull: false, unique: true },
  paymentId: { type: DataTypes.STRING(60), unique: true },
  status: { type: DataTypes.ENUM('created', 'paid', 'failed'), allowNull: false, defaultValue: 'created' },
  paidAt: { type: DataTypes.DATE },
  paidVia: { type: DataTypes.STRING(10) },                                 // app | webhook
  failReason: { type: DataTypes.STRING(200) },
}, { tableName: 'payments', indexes: [{ fields: ['userId', 'createdAt'] }] });

// One chat between a user and a host (never two hosts, never two users). The user starts it; the host replies.
const Conversation = sequelize.define('Conversation', {
  id: { type: DataTypes.BIGINT.UNSIGNED, autoIncrement: true, primaryKey: true },
  userId: { type: DataTypes.BIGINT.UNSIGNED, allowNull: false },
  hostId: { type: DataTypes.BIGINT.UNSIGNED, allowNull: false },
  lastMessageId: { type: DataTypes.BIGINT.UNSIGNED },
  lastAt: { type: DataTypes.DATE },
  userUnread: { type: DataTypes.INTEGER.UNSIGNED, allowNull: false, defaultValue: 0 },
  hostUnread: { type: DataTypes.INTEGER.UNSIGNED, allowNull: false, defaultValue: 0 },
  userFreeUsed: { type: DataTypes.INTEGER.UNSIGNED, allowNull: false, defaultValue: 0 },   // free messages the user has used here
  startedBy: { type: DataTypes.BIGINT.UNSIGNED },                                            // who sent the first message (host chat limit)
}, { tableName: 'conversations', indexes: [{ unique: true, fields: ['userId', 'hostId'] }, { fields: ['hostId', 'lastAt'] }, { fields: ['userId', 'lastAt'] }] });

const Message = sequelize.define('Message', {
  id: { type: DataTypes.BIGINT.UNSIGNED, autoIncrement: true, primaryKey: true },
  conversationId: { type: DataTypes.BIGINT.UNSIGNED, allowNull: false },
  senderId: { type: DataTypes.BIGINT.UNSIGNED, allowNull: false },
  kind: { type: DataTypes.ENUM('text', 'gift', 'call'), allowNull: false, defaultValue: 'text' },   // call: text = missed | declined | cancelled | <seconds>
  text: { type: DataTypes.STRING(1000) },
  giftId: { type: DataTypes.INTEGER.UNSIGNED },
  quantity: { type: DataTypes.INTEGER.UNSIGNED, allowNull: false, defaultValue: 1 },        // gifts: how many at once
  coins: { type: DataTypes.INTEGER.UNSIGNED, allowNull: false, defaultValue: 0 },        // what the sender paid for it (0 = free)
  clientId: { type: DataTypes.STRING(40) },                                               // the phone's id: a resend never pays twice
  readAt: { type: DataTypes.DATE },
}, { tableName: 'messages', updatedAt: false, indexes: [{ fields: ['conversationId', 'id'] }, { unique: true, fields: ['senderId', 'clientId'] }] });

// Gifts: made in the admin panel (name, price, category, picture and an animation file of any kind: mp4, svga, lottie, webp, gif).
const Gift = sequelize.define('Gift', {
  id: { type: DataTypes.INTEGER.UNSIGNED, autoIncrement: true, primaryKey: true },
  name: { type: DataTypes.STRING(40), allowNull: false },
  emoji: { type: DataTypes.STRING(16) },                 // shown until the admin uploads a picture
  price: { type: DataTypes.INTEGER.UNSIGNED, allowNull: false },
  category: { type: DataTypes.STRING(30), allowNull: false, defaultValue: 'Popular' },
  sort: { type: DataTypes.INTEGER, allowNull: false, defaultValue: 0 },
  active: { type: DataTypes.BOOLEAN, allowNull: false, defaultValue: true },
  thumbPath: { type: DataTypes.STRING(200) },
  animPath: { type: DataTypes.STRING(200) },
  animType: { type: DataTypes.STRING(10) },              // mp4 | svga | lottie | webp | gif
}, { tableName: 'gifts' });

// One video call. Coins are charged at the start of every minute while it runs; the server keeps the clock.
const Call = sequelize.define('Call', {
  id: { type: DataTypes.STRING(36), primaryKey: true },
  userId: { type: DataTypes.BIGINT.UNSIGNED, allowNull: false },
  hostId: { type: DataTypes.BIGINT.UNSIGNED, allowNull: false },
  kind: { type: DataTypes.ENUM('direct', 'auto', 'random'), allowNull: false, defaultValue: 'direct' },
  byHost: { type: DataTypes.BOOLEAN, allowNull: false, defaultValue: false },     // true = the host rang the user
  rate: { type: DataTypes.INTEGER.UNSIGNED, allowNull: false },                  // coins per minute, fixed when it starts ringing
  status: { type: DataTypes.ENUM('ringing', 'active', 'ended'), allowNull: false, defaultValue: 'ringing' },
  endReason: { type: DataTypes.STRING(20) },                                     // ended | missed | declined | cancelled | no_coins | disconnected
  acceptedAt: { type: DataTypes.DATE },
  endedAt: { type: DataTypes.DATE },
  seconds: { type: DataTypes.INTEGER.UNSIGNED, allowNull: false, defaultValue: 0 },
  minutesCharged: { type: DataTypes.INTEGER.UNSIGNED, allowNull: false, defaultValue: 0 },
  coinsCharged: { type: DataTypes.INTEGER.UNSIGNED, allowNull: false, defaultValue: 0 },
  hostEarned: { type: DataTypes.INTEGER.UNSIGNED, allowNull: false, defaultValue: 0 },
  rating: { type: DataTypes.TINYINT.UNSIGNED },                                  // 1..5 from the user after the call
  ratingTags: json('ratingTags'),
}, { tableName: 'calls', indexes: [{ fields: ['userId', 'createdAt'] }, { fields: ['hostId', 'createdAt'] }] });

const Setting = sequelize.define('Setting', {
  key: { type: DataTypes.STRING(80), primaryKey: true },
  value: json('value', { allowNull: false }),
  description: { type: DataTypes.STRING(200) },
  isPublic: { type: DataTypes.BOOLEAN, allowNull: false, defaultValue: false }, // true = the app may read it via GET /config
  updatedBy: { type: DataTypes.BIGINT.UNSIGNED },
}, { tableName: 'settings' });

const Block = sequelize.define('Block', {
  id: { type: DataTypes.BIGINT.UNSIGNED, autoIncrement: true, primaryKey: true },
  blockerId: { type: DataTypes.BIGINT.UNSIGNED, allowNull: false },
  blockedId: { type: DataTypes.BIGINT.UNSIGNED, allowNull: false },
}, { tableName: 'blocks', updatedAt: false, indexes: [{ unique: true, fields: ['blockerId', 'blockedId'] }, { fields: ['blockedId'] }] });

const Report = sequelize.define('Report', {
  id: { type: DataTypes.BIGINT.UNSIGNED, autoIncrement: true, primaryKey: true },
  reporterId: { type: DataTypes.BIGINT.UNSIGNED, allowNull: false },
  targetId: { type: DataTypes.BIGINT.UNSIGNED, allowNull: false },
  reason: { type: DataTypes.STRING(40), allowNull: false },
  detail: { type: DataTypes.STRING(500) },
  status: { type: DataTypes.ENUM('open', 'reviewing', 'resolved', 'dismissed'), allowNull: false, defaultValue: 'open' },
  handledBy: { type: DataTypes.BIGINT.UNSIGNED },
}, { tableName: 'reports', indexes: [{ fields: ['status', 'createdAt'] }, { fields: ['targetId'] }] });

const Session = sequelize.define('Session', {
  id: { type: DataTypes.BIGINT.UNSIGNED, autoIncrement: true, primaryKey: true },
  userId: { type: DataTypes.BIGINT.UNSIGNED, allowNull: false },
  tokenHash: { type: DataTypes.CHAR(64), allowNull: false, unique: true },  // sha256 of the refresh token; the token itself is never stored
  expiresAt: { type: DataTypes.DATE, allowNull: false },
  revokedAt: { type: DataTypes.DATE },
  // why it ended: only a 'rotated' token shown again means theft; logout / other devices / delete are normal ends
  revokeReason: { type: DataTypes.STRING(16) },
  userAgent: { type: DataTypes.STRING(200) },
}, { tableName: 'sessions', updatedAt: false, indexes: [{ fields: ['userId'] }] });


// ---- Phase 2 ----
// Host application. PAN is stored encrypted; selfie and ID photo are private files only an admin can open.
const HostApplication = sequelize.define('HostApplication', {
  userId: { type: DataTypes.BIGINT.UNSIGNED, primaryKey: true },
  bio: { type: DataTypes.STRING(150) },
  languages: json('languages'),
  panName: { type: DataTypes.STRING(60) },
  panEnc: { type: DataTypes.STRING(200) },
  idType: { type: DataTypes.STRING(20) },
  idPath: { type: DataTypes.STRING(200) },
  idNumberEnc: { type: DataTypes.STRING(200) }, // encrypted; for Aadhaar only XXXX XXXX 1234 is ever stored
  selfiePath: { type: DataTypes.STRING(200) },
  agencyCode: { type: DataTypes.STRING(20) },
  rejectReason: { type: DataTypes.STRING(200) },
  submittedAt: { type: DataTypes.DATE },
  reviewedAt: { type: DataTypes.DATE },
  reviewedBy: { type: DataTypes.BIGINT.UNSIGNED },
}, { tableName: 'host_applications' });

// Photos and videos of a person. Files live in a private folder and are only handed out by the server after a check.
const Media = sequelize.define('Media', {
  id: { type: DataTypes.BIGINT.UNSIGNED, autoIncrement: true, primaryKey: true },
  userId: { type: DataTypes.BIGINT.UNSIGNED, allowNull: false },
  kind: { type: DataTypes.ENUM('photo', 'video'), allowNull: false },
  path: { type: DataTypes.STRING(200), allowNull: false },
  mime: { type: DataTypes.STRING(40), allowNull: false },
  sizeBytes: { type: DataTypes.INTEGER.UNSIGNED, allowNull: false },
  durationSec: { type: DataTypes.INTEGER.UNSIGNED },
  isMain: { type: DataTypes.BOOLEAN, allowNull: false, defaultValue: false },   // the main photo is the DP (card, chats, status ring)
  locked: { type: DataTypes.BOOLEAN, allowNull: false, defaultValue: false },   // users pay coins to open it (never true for the free ones)
}, { tableName: 'media', indexes: [{ fields: ['userId', 'kind'] }] });

const MediaUnlock = sequelize.define('MediaUnlock', {
  id: { type: DataTypes.BIGINT.UNSIGNED, autoIncrement: true, primaryKey: true },
  userId: { type: DataTypes.BIGINT.UNSIGNED, allowNull: false },
  mediaId: { type: DataTypes.BIGINT.UNSIGNED, allowNull: false },
  price: { type: DataTypes.INTEGER.UNSIGNED, allowNull: false },
}, { tableName: 'media_unlocks', updatedAt: false, indexes: [{ unique: true, fields: ['userId', 'mediaId'] }] });

const Follow = sequelize.define('Follow', {
  id: { type: DataTypes.BIGINT.UNSIGNED, autoIncrement: true, primaryKey: true },
  followerId: { type: DataTypes.BIGINT.UNSIGNED, allowNull: false },
  followedId: { type: DataTypes.BIGINT.UNSIGNED, allowNull: false },
}, { tableName: 'follows', updatedAt: false, indexes: [{ unique: true, fields: ['followerId', 'followedId'] }, { fields: ['followedId'] }] });

// one row per visitor per profile per day (day = YYYY-MM-DD, UTC)
const Visit = sequelize.define('Visit', {
  id: { type: DataTypes.BIGINT.UNSIGNED, autoIncrement: true, primaryKey: true },
  visitedId: { type: DataTypes.BIGINT.UNSIGNED, allowNull: false },
  visitorId: { type: DataTypes.BIGINT.UNSIGNED, allowNull: false },
  day: { type: DataTypes.CHAR(10), allowNull: false },
}, { tableName: 'visits', indexes: [{ unique: true, fields: ['visitedId', 'visitorId', 'day'] }, { fields: ['visitedId', 'createdAt'] }] });

// switches the user can change in Settings (notifications only: online status and visits are always shown)
const UserSetting = sequelize.define('UserSetting', {
  userId: { type: DataTypes.BIGINT.UNSIGNED, primaryKey: true },
  notifyMessages: { type: DataTypes.BOOLEAN, allowNull: false, defaultValue: true },
  notifyMissedCalls: { type: DataTypes.BOOLEAN, allowNull: false, defaultValue: true },
  notifyOffers: { type: DataTypes.BOOLEAN, allowNull: false, defaultValue: true },
}, { tableName: 'user_settings' });

// host-only details (Phase 2 adds more): the prices the host sets, always inside the admin's range
const HostProfile = sequelize.define('HostProfile', {
  userId: { type: DataTypes.BIGINT.UNSIGNED, primaryKey: true },
  callRate: { type: DataTypes.INTEGER.UNSIGNED, allowNull: false },       // coins per minute of video call
  messagePrice: { type: DataTypes.INTEGER.UNSIGNED, allowNull: false },   // coins per paid message
  isOnline: { type: DataTypes.BOOLEAN, allowNull: false, defaultValue: false },
  ratingSum: { type: DataTypes.INTEGER.UNSIGNED, allowNull: false, defaultValue: 0 },
  ratingCount: { type: DataTypes.INTEGER.UNSIGNED, allowNull: false, defaultValue: 0 }, // the host's "I'm online" switch (Phase 4 replaces it with live presence)
  onlineAt: { type: DataTypes.DATE },
  payoutUpiEnc: { type: DataTypes.STRING(300) },                         // where withdrawals go (encrypted), saved once
  payoutName: { type: DataTypes.STRING(80) },
}, { tableName: 'host_profiles' });

User.hasOne(Wallet, { foreignKey: 'userId' });
Wallet.belongsTo(User, { foreignKey: 'userId' });
User.hasMany(Transaction, { foreignKey: 'userId' });
User.hasMany(Session, { foreignKey: 'userId' });
User.hasOne(UserSetting, { foreignKey: 'userId' });
User.hasOne(HostProfile, { foreignKey: 'userId' });
User.hasOne(HostApplication, { foreignKey: 'userId' });

// The bell list: something happened for this person (missed call, gift, new follower, VIP, coins, withdrawal ...)
const Notification = sequelize.define('Notification', {
  id: { type: DataTypes.BIGINT.UNSIGNED, autoIncrement: true, primaryKey: true },
  userId: { type: DataTypes.BIGINT.UNSIGNED, allowNull: false },
  kind: { type: DataTypes.STRING(24), allowNull: false },
  title: { type: DataTypes.STRING(120), allowNull: false },
  body: { type: DataTypes.STRING(300) },
  data: json('data'),
  readAt: { type: DataTypes.DATE },
}, { tableName: 'notifications', updatedAt: false, indexes: [{ fields: ['userId', 'id'] }] });

// A host's withdrawal: asked in BEANS (coins held = beans × coins_per_bean at that time); the admin pays outside the app
const Withdrawal = sequelize.define('Withdrawal', {
  id: { type: DataTypes.BIGINT.UNSIGNED, autoIncrement: true, primaryKey: true },
  hostId: { type: DataTypes.BIGINT.UNSIGNED, allowNull: false },
  beans: { type: DataTypes.INTEGER.UNSIGNED, allowNull: false },
  coins: { type: DataTypes.INTEGER.UNSIGNED, allowNull: false },
  upiEnc: { type: DataTypes.STRING(300), allowNull: false },
  upiName: { type: DataTypes.STRING(80), allowNull: false },
  status: { type: DataTypes.ENUM('pending', 'paid', 'rejected'), allowNull: false, defaultValue: 'pending' },
  note: { type: DataTypes.STRING(200) },
  decidedAt: { type: DataTypes.DATE },
  decidedBy: { type: DataTypes.BIGINT.UNSIGNED },
}, { tableName: 'withdrawals', indexes: [{ fields: ['hostId', 'id'] }, { fields: ['status'] }] });

// A status (story): a host's photo or video that everyone may see for status_hours, then it is gone
const Story = sequelize.define('Story', {
  id: { type: DataTypes.BIGINT.UNSIGNED, autoIncrement: true, primaryKey: true },
  userId: { type: DataTypes.BIGINT.UNSIGNED, allowNull: false },
  kind: { type: DataTypes.ENUM('photo', 'video'), allowNull: false },
  path: { type: DataTypes.STRING(200), allowNull: false },
  mime: { type: DataTypes.STRING(40), allowNull: false },
  sizeBytes: { type: DataTypes.INTEGER.UNSIGNED, allowNull: false },
  durationSec: { type: DataTypes.INTEGER.UNSIGNED },
  caption: { type: DataTypes.STRING(150) },
  expiresAt: { type: DataTypes.DATE, allowNull: false },
}, { tableName: 'stories', updatedAt: false, indexes: [{ fields: ['userId', 'expiresAt'] }, { fields: ['expiresAt'] }] });

// who saw a status (the poster sees the list)
const StoryView = sequelize.define('StoryView', {
  id: { type: DataTypes.BIGINT.UNSIGNED, autoIncrement: true, primaryKey: true },
  storyId: { type: DataTypes.BIGINT.UNSIGNED, allowNull: false },
  viewerId: { type: DataTypes.BIGINT.UNSIGNED, allowNull: false },
}, { tableName: 'story_views', updatedAt: false, indexes: [{ unique: true, fields: ['storyId', 'viewerId'] }] });

// A help request from Help & support; the admin answers it (the answer also arrives as a notification)
const SupportTicket = sequelize.define('SupportTicket', {
  id: { type: DataTypes.BIGINT.UNSIGNED, autoIncrement: true, primaryKey: true },
  userId: { type: DataTypes.BIGINT.UNSIGNED, allowNull: false },
  topic: { type: DataTypes.STRING(40), allowNull: false },
  text: { type: DataTypes.STRING(1000), allowNull: false },
  status: { type: DataTypes.ENUM('open', 'answered'), allowNull: false, defaultValue: 'open' },
  reply: { type: DataTypes.STRING(1000) },
  repliedAt: { type: DataTypes.DATE },
  replySeen: { type: DataTypes.BOOLEAN, allowNull: false, defaultValue: false },
}, { tableName: 'support_tickets', indexes: [{ fields: ['userId', 'id'] }, { fields: ['status'] }] });

// Levels (the admin edits them): the XP needed for each level and the coins given on reaching it
const LevelRule = sequelize.define('LevelRule', {
  level: { type: DataTypes.INTEGER.UNSIGNED, primaryKey: true },
  xp: { type: DataTypes.INTEGER.UNSIGNED, allowNull: false },
  rewardCoins: { type: DataTypes.INTEGER.UNSIGNED, allowNull: false, defaultValue: 0 },
}, { tableName: 'level_rules', timestamps: false });

// VIP plans (the admin edits them): price, how many days, bonus coins, an optional tag ("Popular")
const VipPlan = sequelize.define('VipPlan', {
  id: { type: DataTypes.INTEGER.UNSIGNED, autoIncrement: true, primaryKey: true },
  name: { type: DataTypes.STRING(40), allowNull: false },
  days: { type: DataTypes.INTEGER.UNSIGNED, allowNull: false },
  pricePaise: { type: DataTypes.INTEGER.UNSIGNED, allowNull: false },
  bonusCoins: { type: DataTypes.INTEGER.UNSIGNED, allowNull: false, defaultValue: 0 },
  tag: { type: DataTypes.STRING(20) },
  sort: { type: DataTypes.INTEGER, allowNull: false, defaultValue: 0 },
  active: { type: DataTypes.BOOLEAN, allowNull: false, defaultValue: true },
}, { tableName: 'vip_plans' });

module.exports = { SupportTicket, Story, StoryView, LevelRule, Notification, Withdrawal, VipPlan, sequelize, User, Wallet, Transaction, Setting, Block, Report, Session, UserSetting, HostProfile, HostApplication, Media, MediaUnlock, Follow, Visit, CoinPack, Payment, Conversation, Message, Gift, Call, };
