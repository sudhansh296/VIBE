const { Setting } = require('../models');

/**
 * Every number that can change money or rules lives in the `settings` table and is edited from the admin panel.
 * These are only the first-run defaults; an existing value is never overwritten.
 */
const DEFAULTS = [
  ['min_age', 18, 'Minimum age to use the app', true],
  ['signup_bonus_coins', 100, 'Free coins given once to a new account', false],
  ['free_messages_per_chat', 5, 'Free messages before paid chat starts', true],
  ['host_new_chats_per_day', 50, 'New chats a host can start with users per day (replies are never limited)', false],
  ['allowed_languages', ['Hindi', 'English', 'Punjabi', 'Bengali', 'Tamil', 'Telugu', 'Marathi', 'Kannada', 'Gujarati', 'Malayalam', 'Urdu', 'Odia'], 'Languages a profile can list', true],
  ['max_profile_languages', 6, 'How many languages one profile can list', true],
  ['report_reasons', ['fake_profile', 'nudity', 'abuse', 'asked_money', 'under_18', 'spam', 'other'], 'Allowed report reasons', true],
  ['host_call_rate_min', 40, 'Lowest video call price a host may set (coins per minute)', true],
  ['host_call_rate_max', 400, 'Highest video call price a host may set (coins per minute)', true],
  ['host_call_rate_default', 60, 'Video call price of a new host', false],
  ['host_message_price_min', 50, 'Lowest paid-chat price a host may set (coins per message)', true],
  ['host_message_price_max', 1000, 'Highest paid-chat price a host may set (coins per message)', true],
  ['host_message_price_default', 200, 'Paid-chat price of a new host', false],
  ['host_share_pct', 60, 'Share of what users spend that goes to the host (percent)', true],
  ['media_max_photos', 6, 'Most photos one profile can have', true],
  ['media_max_videos', 6, 'Most videos one profile can have', true],
  ['media_free_photos', 3, 'The first photos are always free to see', true],
  ['media_free_videos', 1, 'The first videos are always free to see', true],
  ['media_price_photo', 200, 'Coins to unlock one locked photo', true],
  ['media_price_video', 500, 'Coins to unlock one locked video', true],
  ['media_max_photo_mb', 5, 'Biggest photo file (MB)', true],
  ['media_max_video_mb', 50, 'Biggest video file (MB)', true],
  ['media_max_video_seconds', 30, 'Longest video (seconds), the same for every video', true],
  ['host_min_photos', 3, 'Photos needed to apply as a host', true],
  ['host_min_videos', 1, 'Videos needed to apply as a host', true],
  ['host_bio_min_chars', 10, 'Shortest bio to apply as a host', true],
  ['host_id_types', ['aadhaar', 'driving_licence', 'voter_id', 'passport'], 'ID documents accepted from a host applicant', true],
  ['watch_notify_seconds', 5, 'Seconds a user stays on a host profile before the host gets a "someone is viewing you" notification', true],
  ['watch_autocall_seconds', 10, 'Seconds a user stays on a host profile before a video call starts ringing on its own (host online and free)', true],
  ['autocall_enabled', true, 'Automatic call after staying on a profile (true / false)', true],
  ['call_ring_seconds', 30, 'How long a call rings before it counts as missed', true],
  ['checkin_day1', 10, 'Daily check-in coins, day 1', true], ['checkin_day2', 10, 'Daily check-in coins, day 2', true],
  ['checkin_day3', 20, 'Daily check-in coins, day 3', true], ['checkin_day4', 20, 'Daily check-in coins, day 4', true],
  ['checkin_day5', 30, 'Daily check-in coins, day 5', true], ['checkin_day6', 30, 'Daily check-in coins, day 6', true],
  ['checkin_day7', 100, 'Daily check-in coins, day 7 (jackpot)', true],
  ['invite_reward_coins', 100, 'Invite: coins for both people once the invited friend buys coins the first time', true],
  ['invite_link_base', 'http://localhost:4000', 'Invite links: the public address of this server (the link is <address>/i/<code>); set the real domain at launch', false],
  ['play_store_url', 'https://play.google.com/store/apps/details?id=com.vibe.callme', 'Invite links: the Play Store page of the app (people without the app go there; the code goes along)', false],
  ['xp_call_minute', 10, 'XP for each paid call minute (both people)', true], ['xp_message', 2, 'XP for each chat message sent', true],
  ['xp_gift_per_10_coins', 1, 'XP for every 10 coins of gifts sent', true], ['xp_checkin', 20, 'XP for a daily check-in', true],
  ['status_hours', 24, 'How long a status stays up (hours)', true],
  ['status_max_active', 10, 'Most statuses one host can have up at once', true],
  ['coins_per_bean', 1, 'Withdrawals: how many of a host\'s earned coins make 1 bean (the withdraw page shows beans only)', false],
  ['withdraw_min_beans', 5000, 'Smallest withdrawal a host can ask for (beans)', true],
  ['vip_call_discount_pct', 20, 'How much cheaper video calls are for VIP users (percent; the host earns its share of what the user pays)', true],
  ['random_match_rate', 100, 'Coins per minute for a Random match call (the same for every host; the host earns its share of it)', true],
  ['call_reconnect_seconds', 20, 'How long a call waits for someone who lost the connection before it ends', false],
  ['coin_purchase_enabled', true, 'Users can buy coins (set false to pause all purchases)', true],
  ['maintenance_mode', false, 'Set true to stop the app (shows the maintenance screen)', true],
  ['maintenance_message', 'VIBE is under maintenance. We will be back soon. Your coins and chats are safe.', 'Text shown on the maintenance screen', true],
  ['min_app_version', '1.0.0', 'Older app versions are forced to update', true],
];

async function seedSettings() {
  for (const [key, value, description, isPublic] of DEFAULTS) {
    await Setting.findOrCreate({ where: { key }, defaults: { value, description, isPublic } });
  }
}

const cache = new Map();
async function get(key) {
  const hit = cache.get(key);
  if (hit && Date.now() - hit.at < 30_000) return hit.value; // 30 s cache so an admin change applies quickly
  const row = await Setting.findByPk(key);
  const value = row ? row.value : (DEFAULTS.find((d) => d[0] === key) || [])[1];
  cache.set(key, { value, at: Date.now() });
  return value;
}

function clearCache() { cache.clear(); }

async function publicConfig() {
  const rows = await Setting.findAll({ where: { isPublic: true } });
  return Object.fromEntries(rows.map((r) => [r.key, r.value]));
}

// ---------------------------------------------------------------- editing (the admin panel calls this)
// Allowed range for each number the admin may change, so one wrong keystroke cannot break the app.
const RANGE = {
  min_age: [18, 99], signup_bonus_coins: [0, 100000], free_messages_per_chat: [0, 100], host_new_chats_per_day: [0, 10000], max_profile_languages: [1, 12],
  host_call_rate_min: [1, 100000], host_call_rate_max: [1, 100000], host_call_rate_default: [1, 100000],
  host_message_price_min: [1, 100000], host_message_price_max: [1, 100000], host_message_price_default: [1, 100000],
  host_share_pct: [0, 100], vip_call_discount_pct: [0, 90], coins_per_bean: [1, 100000], status_hours: [1, 168], status_max_active: [1, 100], checkin_day1: [0, 100000], checkin_day2: [0, 100000], checkin_day3: [0, 100000], checkin_day4: [0, 100000],
  checkin_day5: [0, 100000], checkin_day6: [0, 100000], checkin_day7: [0, 100000], invite_reward_coins: [0, 100000],
  xp_call_minute: [0, 10000], xp_message: [0, 10000], xp_gift_per_10_coins: [0, 10000], xp_checkin: [0, 10000], withdraw_min_beans: [1, 100000000], media_max_photos: [1, 30], media_max_videos: [1, 30], media_free_photos: [0, 30], media_free_videos: [0, 30],
  media_price_photo: [1, 100000], media_price_video: [1, 100000], media_max_photo_mb: [1, 50], media_max_video_mb: [1, 500],
  watch_notify_seconds: [1, 120], watch_autocall_seconds: [2, 300], call_ring_seconds: [10, 120], random_match_rate: [1, 100000], call_reconnect_seconds: [5, 120],
  media_max_video_seconds: [5, 300], host_min_photos: [1, 30], host_min_videos: [0, 30], host_bio_min_chars: [0, 150],
};
// changing these can lock every user out, so the panel must send confirm: true
const DANGEROUS = new Set(['maintenance_mode', 'min_app_version']);

class SettingError extends Error { constructor(code, message) { super(message); this.code = code; } }

/** Checks one new value against the kind of the default (number / true-false / text / list of texts) and its range. */
function validate(key, value) {
  const def = DEFAULTS.find((d) => d[0] === key);
  if (!def) throw new SettingError('unknown_setting', 'No such setting');
  const sample = def[1];
  if (typeof sample === 'number') {
    if (!Number.isInteger(value)) throw new SettingError('invalid_value', 'Enter a whole number');
    const [lo, hi] = RANGE[key] || [0, 1_000_000];
    if (value < lo || value > hi) throw new SettingError('out_of_range', `Must be between ${lo} and ${hi}`);
  } else if (typeof sample === 'boolean') {
    if (typeof value !== 'boolean') throw new SettingError('invalid_value', 'Must be true or false');
  } else if (Array.isArray(sample)) {
    if (!Array.isArray(value) || !value.length || value.length > 50 || value.some((x) => typeof x !== 'string' || !x.trim() || x.length > 40)) {
      throw new SettingError('invalid_value', 'Must be a list of 1 to 50 short texts');
    }
  } else if (typeof sample === 'string') {
    if (typeof value !== 'string' || !value.trim() || value.length > 300) throw new SettingError('invalid_value', 'Must be a text up to 300 characters');
    if (key === 'min_app_version' && !/^\d+(\.\d+){0,2}$/.test(value)) throw new SettingError('invalid_value', 'Use a version like 1.2.0');
  }
}

/** Rules that tie two settings together (e.g. the lowest price may not be above the highest). */
async function checkTies(key, value) {
  const num = async (k) => (k === key ? value : Number(await get(k)));
  const ties = [
    ['host_call_rate_min', 'host_call_rate_max', 'The lowest call price must not be above the highest'],
    ['host_message_price_min', 'host_message_price_max', 'The lowest message price must not be above the highest'],
    ['media_free_photos', 'media_max_photos', 'Free photos must not be more than the most photos allowed'],
    ['media_free_videos', 'media_max_videos', 'Free videos must not be more than the most videos allowed'],
    ['host_min_photos', 'media_max_photos', 'Photos needed to apply must not be more than the most photos allowed'],
    ['watch_notify_seconds', 'watch_autocall_seconds', 'The notification must come before the automatic call'],
    ['host_min_videos', 'media_max_videos', 'Videos needed to apply must not be more than the most videos allowed'],
  ];
  for (const [lowKey, highKey, msg] of ties) {
    if (key !== lowKey && key !== highKey) continue;
    if ((await num(lowKey)) > (await num(highKey))) throw new SettingError('conflicts', msg);
  }
  for (const k of ['host_call_rate_default', 'host_message_price_default']) {
    if (key.startsWith(k.replace('_default', '_')) || key === k) {
      const base = k.replace('_default', '');
      const d = await num(k); const lo = await num(base + '_min'); const hi = await num(base + '_max');
      if (d < lo || d > hi) throw new SettingError('conflicts', 'The starting price must stay between the lowest and the highest');
    }
  }
}

async function listAll() {
  const rows = await Setting.findAll({ order: [['key', 'ASC']] });
  return rows.map((r) => ({ key: r.key, value: r.value, description: r.description, isPublic: r.isPublic, dangerous: DANGEROUS.has(r.key), range: RANGE[r.key] || null, updatedAt: r.updatedAt, updatedBy: r.updatedBy }));
}

async function update(key, value, adminId) {
  validate(key, value);
  await checkTies(key, value);
  const row = await Setting.findByPk(key);
  await row.update({ value, updatedBy: adminId });
  clearCache();
  return row;
}

module.exports = { seedSettings, get, clearCache, publicConfig, listAll, update, SettingError, DANGEROUS };
