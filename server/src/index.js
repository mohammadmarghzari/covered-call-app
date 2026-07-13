import express from 'express';
import { store } from './store.js';
import { issueToken, requireAuth, requireAdmin } from './auth.js';
import { normalizePhone, createOtp, verifyOtp } from './otp.js';
import { sendOtpSms, smsProvider } from './sms.js';
import { genCode } from './util.js';

const app = express();
app.use(express.json());

// نمای عمومی کاربر برای اپ
function publicUser(u) {
  return {
    id: u.id,
    phone: u.phone,
    displayName: u.displayName || '',
    isSubscribed: u.isSubscribed && u.subscriptionExpiryMillis > Date.now(),
    subscriptionExpiryMillis: u.subscriptionExpiryMillis || 0
  };
}

app.get('/health', (req, res) => {
  res.json({ ok: true, smsProvider, time: Date.now() });
});

// ---------------------------------------------------------------------------
// ورود با شماره موبایل (OTP)
// ---------------------------------------------------------------------------

app.post('/auth/request-otp', async (req, res) => {
  const phone = normalizePhone(req.body?.phone);
  if (!phone) return res.status(400).json({ error: 'invalid_phone' });

  const result = createOtp(phone);
  if (result.error === 'too_soon') {
    return res.status(429).json({ error: 'too_soon', retryAfterSec: result.retryAfterSec });
  }

  const sent = await sendOtpSms(phone, result.code);
  if (!sent.ok) {
    return res.status(502).json({ error: 'sms_failed', detail: sent.error });
  }
  res.json({ ok: true, retryAfterSec: 60 });
});

app.post('/auth/verify-otp', (req, res) => {
  const phone = normalizePhone(req.body?.phone);
  const code = req.body?.code;
  if (!phone || !code) return res.status(400).json({ error: 'invalid_input' });

  const result = verifyOtp(phone, code);
  if (!result.ok) return res.status(401).json({ error: result.error });

  const user = store.upsertUser(phone, req.body?.displayName || '');
  const token = issueToken(user);
  res.json({ token, user: publicUser(user) });
});

// ---------------------------------------------------------------------------
// کاربر
// ---------------------------------------------------------------------------

app.get('/me', requireAuth, (req, res) => {
  res.json({ user: publicUser(req.user) });
});

app.post('/me', requireAuth, (req, res) => {
  const name = String(req.body?.displayName || '').slice(0, 60);
  const u = store.upsertUser(req.user.phone, name);
  res.json({ user: publicUser(u) });
});

// ---------------------------------------------------------------------------
// اشتراک: فعال‌سازی با کد (بدون نیاز به درگاه/گوگل)
// ---------------------------------------------------------------------------

app.post('/subscription/redeem', requireAuth, (req, res) => {
  const code = String(req.body?.code || '').trim().toUpperCase();
  if (!code) return res.status(400).json({ error: 'invalid_code' });

  const record = store.getActivationCode(code);
  if (!record) return res.status(404).json({ error: 'code_not_found' });
  if (record.usedByPhone) return res.status(409).json({ error: 'code_already_used' });

  store.useActivationCode(code, req.user.phone);
  const current = req.user.subscriptionExpiryMillis > Date.now()
    ? req.user.subscriptionExpiryMillis
    : Date.now();
  const expiry = current + record.days * 24 * 60 * 60 * 1000;
  const u = store.setSubscription(req.user.phone, expiry);
  res.json({ user: publicUser(u) });
});

// ---------------------------------------------------------------------------
// سوالات و انتقادات
// ---------------------------------------------------------------------------

app.get('/feedback', requireAuth, (req, res) => {
  res.json({ items: store.feedbackForUser(req.user.id) });
});

app.post('/feedback', requireAuth, (req, res) => {
  const message = String(req.body?.message || '').trim();
  if (!message) return res.status(400).json({ error: 'empty_message' });
  const entry = store.addFeedback({
    id: 'f_' + Math.random().toString(36).slice(2, 10),
    userId: req.user.id,
    phone: req.user.phone,
    message: message.slice(0, 2000),
    reply: '',
    submittedAt: Date.now(),
    repliedAt: 0
  });
  res.json({ item: entry });
});

// ---------------------------------------------------------------------------
// ادمین (با هدر x-admin-token)
// ---------------------------------------------------------------------------

app.get('/admin/feedback', requireAdmin, (req, res) => {
  res.json({ items: store.allFeedback() });
});

app.post('/admin/feedback/reply', requireAdmin, (req, res) => {
  const { id, reply } = req.body || {};
  const f = store.replyFeedback(String(id), String(reply || ''));
  if (!f) return res.status(404).json({ error: 'not_found' });
  res.json({ item: f });
});

app.post('/admin/activation-codes', requireAdmin, (req, res) => {
  const count = Math.min(Math.max(Number(req.body?.count) || 1, 1), 100);
  const days = Math.max(Number(req.body?.days) || 30, 1);
  const codes = [];
  for (let i = 0; i < count; i++) {
    const code = genCode();
    store.addActivationCode(code, days);
    codes.push(code);
  }
  res.json({ codes, days });
});

app.post('/admin/subscription', requireAdmin, (req, res) => {
  const phone = normalizePhone(req.body?.phone);
  const days = Math.max(Number(req.body?.days) || 30, 1);
  if (!phone) return res.status(400).json({ error: 'invalid_phone' });
  store.upsertUser(phone);
  const expiry = Date.now() + days * 24 * 60 * 60 * 1000;
  const u = store.setSubscription(phone, expiry);
  res.json({ user: publicUser(u) });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`آپشن‌یار backend روی پورت ${PORT} اجرا شد (پیامک: ${smsProvider})`);
});

export { app };
