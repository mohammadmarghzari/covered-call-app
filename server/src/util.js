// توابع کمکی مشترک (بدون هیچ side-effect ای مثل روشن‌کردن سرور).

/** یک کد اشتراک ۱۰ کاراکتری بدون حروف/اعداد گمراه‌کننده (0/O/1/I) می‌سازد. */
export function genCode() {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
  let s = '';
  for (let i = 0; i < 10; i++) s += chars[Math.floor(Math.random() * chars.length)];
  return s;
}
