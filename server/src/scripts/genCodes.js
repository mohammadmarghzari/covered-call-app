// ساخت کد اشتراک از خط فرمان (بدون نیاز به سرور روشن):
//   node src/scripts/genCodes.js <تعداد> <روز>
// مثال: node src/scripts/genCodes.js 5 30   → ۵ کد ۳۰روزه می‌سازد و چاپ می‌کند.

import { store } from '../store.js';
import { genCode } from '../util.js';

const count = Math.min(Math.max(Number(process.argv[2]) || 1, 1), 500);
const days = Math.max(Number(process.argv[3]) || 30, 1);

const codes = [];
for (let i = 0; i < count; i++) {
  const code = genCode();
  store.addActivationCode(code, days);
  codes.push(code);
}

console.log(`\n${count} کد ${days}روزه ساخته شد:\n`);
codes.forEach((c) => console.log('  ' + c));
console.log('\nاین کدها را به کاربرانی که پرداخت کرده‌اند بده تا در اپ وارد کنند.\n');
process.exit(0);
