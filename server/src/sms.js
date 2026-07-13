// ارسال کد تأیید. پیش‌فرض: کاوه‌نگار. اگر کلید تنظیم نشده باشد، حالت console
// فعال می‌شود که کد را فقط در لاگ سرور چاپ می‌کند (برای تست بدون هزینه‌ی پیامک).

const PROVIDER = process.env.SMS_PROVIDER || (process.env.KAVENEGAR_API_KEY ? 'kavenegar' : 'console');

/**
 * کد را برای شماره می‌فرستد.
 * @returns {Promise<{ok: boolean, error?: string}>}
 */
export async function sendOtpSms(phone, code) {
  if (PROVIDER === 'console') {
    console.log(`[SMS:console] کد ورود برای ${phone} => ${code}`);
    return { ok: true };
  }
  if (PROVIDER === 'kavenegar') {
    return sendViaKavenegar(phone, code);
  }
  if (PROVIDER === 'smsir') {
    return sendViaSmsIr(phone, code);
  }
  return { ok: false, error: 'unknown_provider' };
}

// کاوه‌نگار: روش verify/lookup (مخصوص کد یک‌بارمصرف؛ نیازمند تأیید یک «الگو/template»).
async function sendViaKavenegar(phone, code) {
  const key = process.env.KAVENEGAR_API_KEY;
  const template = process.env.KAVENEGAR_TEMPLATE; // نام الگوی تأییدشده در پنل کاوه‌نگار
  if (!key || !template) return { ok: false, error: 'kavenegar_not_configured' };

  const url =
    `https://api.kavenegar.com/v1/${encodeURIComponent(key)}/verify/lookup.json` +
    `?receptor=${encodeURIComponent(phone)}&token=${encodeURIComponent(code)}` +
    `&template=${encodeURIComponent(template)}`;

  try {
    const res = await fetch(url);
    const data = await res.json().catch(() => ({}));
    const status = data?.return?.status;
    if (status === 200) return { ok: true };
    return { ok: false, error: `kavenegar_${status || res.status}` };
  } catch (e) {
    return { ok: false, error: 'kavenegar_network' };
  }
}

// SMS.ir: روش ارسال کد تأیید (Verify). پارامترها بسته به پنل ممکن است کمی فرق کنند.
async function sendViaSmsIr(phone, code) {
  const key = process.env.SMSIR_API_KEY;
  const templateId = process.env.SMSIR_TEMPLATE_ID;
  const paramName = process.env.SMSIR_PARAM_NAME || 'CODE';
  if (!key || !templateId) return { ok: false, error: 'smsir_not_configured' };

  try {
    const res = await fetch('https://api.sms.ir/v1/send/verify', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'application/json',
        'x-api-key': key
      },
      body: JSON.stringify({
        mobile: phone,
        templateId: Number(templateId),
        parameters: [{ name: paramName, value: String(code) }]
      })
    });
    const data = await res.json().catch(() => ({}));
    if (res.ok && (data?.status === 1 || data?.status === 200)) return { ok: true };
    return { ok: false, error: `smsir_${data?.status || res.status}` };
  } catch {
    return { ok: false, error: 'smsir_network' };
  }
}

export const smsProvider = PROVIDER;
