// ذخیره‌سازی سبک مبتنی بر فایل JSON — بدون وابستگی نیتیو، قابل اجرا روی هر هاستی.
// برای بار بالا بعداً می‌توان به PostgreSQL/SQLite ارتقا داد؛ رابط این ماژول همان می‌ماند.

import fs from 'node:fs';
import path from 'node:path';

const DATA_DIR = process.env.DATA_DIR || path.join(process.cwd(), 'data');
const DB_FILE = path.join(DATA_DIR, 'db.json');

function ensureDir() {
  if (!fs.existsSync(DATA_DIR)) fs.mkdirSync(DATA_DIR, { recursive: true });
}

const empty = () => ({
  users: {},          // phone -> { id, phone, displayName, isSubscribed, subscriptionExpiryMillis, createdAt }
  otps: {},           // phone -> { code, expiresAt, lastSentAt, attempts }
  feedback: [],       // { id, userId, phone, message, reply, submittedAt, repliedAt }
  activationCodes: {} // code -> { days, usedByPhone|null, createdAt, usedAt|null }
});

let db = empty();

function load() {
  ensureDir();
  if (fs.existsSync(DB_FILE)) {
    try {
      db = { ...empty(), ...JSON.parse(fs.readFileSync(DB_FILE, 'utf8')) };
    } catch {
      db = empty();
    }
  } else {
    persist();
  }
}

function persist() {
  ensureDir();
  const tmp = DB_FILE + '.tmp';
  fs.writeFileSync(tmp, JSON.stringify(db, null, 2));
  fs.renameSync(tmp, DB_FILE); // نوشتن اتمیک
}

load();

export const store = {
  raw: () => db,
  save: persist,

  // -------- users --------
  getUser(phone) {
    return db.users[phone] || null;
  },
  upsertUser(phone, displayName = '') {
    let u = db.users[phone];
    if (!u) {
      u = {
        id: 'u_' + Math.random().toString(36).slice(2, 10),
        phone,
        displayName,
        isSubscribed: false,
        subscriptionExpiryMillis: 0,
        createdAt: Date.now()
      };
      db.users[phone] = u;
      persist();
    } else if (displayName && u.displayName !== displayName) {
      u.displayName = displayName;
      persist();
    }
    return u;
  },
  setSubscription(phone, expiryMillis) {
    const u = db.users[phone];
    if (!u) return null;
    u.isSubscribed = expiryMillis > Date.now();
    u.subscriptionExpiryMillis = expiryMillis;
    persist();
    return u;
  },

  // -------- otp --------
  getOtp(phone) {
    return db.otps[phone] || null;
  },
  setOtp(phone, record) {
    db.otps[phone] = record;
    persist();
  },
  clearOtp(phone) {
    delete db.otps[phone];
    persist();
  },

  // -------- feedback --------
  addFeedback(entry) {
    db.feedback.push(entry);
    persist();
    return entry;
  },
  feedbackForUser(userId) {
    return db.feedback
      .filter((f) => f.userId === userId)
      .sort((a, b) => b.submittedAt - a.submittedAt);
  },
  allFeedback() {
    return [...db.feedback].sort((a, b) => b.submittedAt - a.submittedAt);
  },
  replyFeedback(id, reply) {
    const f = db.feedback.find((x) => x.id === id);
    if (!f) return null;
    f.reply = reply;
    f.repliedAt = Date.now();
    persist();
    return f;
  },

  // -------- activation codes --------
  addActivationCode(code, days) {
    db.activationCodes[code] = { days, usedByPhone: null, createdAt: Date.now(), usedAt: null };
    persist();
  },
  getActivationCode(code) {
    return db.activationCodes[code] || null;
  },
  useActivationCode(code, phone) {
    const c = db.activationCodes[code];
    if (!c || c.usedByPhone) return null;
    c.usedByPhone = phone;
    c.usedAt = Date.now();
    persist();
    return c;
  }
};
