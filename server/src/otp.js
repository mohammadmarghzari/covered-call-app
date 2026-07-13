import { store } from './store.js';

const CODE_TTL_MS = 2 * 60 * 1000;   // اعتبار کد: ۲ دقیقه
const RESEND_GAP_MS = 60 * 1000;     // فاصله‌ی مجاز بین دو ارسال: ۶۰ ثانیه
const MAX_ATTEMPTS = 5;              // حداکثر تلاش برای هر کد

/** شماره را به شکل استاندارد 09xxxxxxxxx برمی‌گرداند، یا null اگر نامعتبر بود. */
export function normalizePhone(input) {
  if (!input) return null;
  let p = String(input).trim().replace(/[\s-]/g, '');
  // فارسی/عربی به لاتین
  p = p.replace(/[۰-۹]/g, (d) => '۰۱۲۳۴۵۶۷۸۹'.indexOf(d))
       .replace(/[٠-٩]/g, (d) => '٠١٢٣٤٥٦٧٨٩'.indexOf(d));
  if (p.startsWith('+98')) p = '0' + p.slice(3);
  else if (p.startsWith('0098')) p = '0' + p.slice(4);
  else if (p.startsWith('98') && p.length === 12) p = '0' + p.slice(2);
  if (/^9\d{9}$/.test(p)) p = '0' + p;
  return /^09\d{9}$/.test(p) ? p : null;
}

function randomCode() {
  return String(Math.floor(10000 + Math.random() * 90000)); // ۵ رقمی
}

/**
 * یک کد جدید می‌سازد و ذخیره می‌کند. اگر خیلی زود دوباره درخواست شود، خطا می‌دهد.
 * خود ارسال پیامک بیرون از این تابع انجام می‌شود.
 */
export function createOtp(phone) {
  const existing = store.getOtp(phone);
  const now = Date.now();
  if (existing && now - existing.lastSentAt < RESEND_GAP_MS) {
    const wait = Math.ceil((RESEND_GAP_MS - (now - existing.lastSentAt)) / 1000);
    return { error: 'too_soon', retryAfterSec: wait };
  }
  const code = randomCode();
  store.setOtp(phone, { code, expiresAt: now + CODE_TTL_MS, lastSentAt: now, attempts: 0 });
  return { code };
}

/** کد واردشده را بررسی می‌کند. */
export function verifyOtp(phone, code) {
  const rec = store.getOtp(phone);
  if (!rec) return { ok: false, error: 'no_code' };
  if (Date.now() > rec.expiresAt) {
    store.clearOtp(phone);
    return { ok: false, error: 'expired' };
  }
  if (rec.attempts >= MAX_ATTEMPTS) {
    store.clearOtp(phone);
    return { ok: false, error: 'too_many_attempts' };
  }
  if (String(code).trim() !== rec.code) {
    rec.attempts += 1;
    store.setOtp(phone, rec);
    return { ok: false, error: 'wrong_code' };
  }
  store.clearOtp(phone);
  return { ok: true };
}
