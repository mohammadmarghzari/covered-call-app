# بک‌اند آپشن‌یار

سرویس سبک Node.js برای **ورود با شماره موبایل (OTP)**، **اشتراک** و **سوالات و انتقادات** —
جایگزین Firebase تا اپ در ایران **بدون VPN** کار کند. هیچ وابستگی به سرویس‌های گوگل ندارد.

## اجرای محلی

```bash
cd server
cp .env.example .env      # مقادیر را پر کن
npm install
npm start
```

بدون تنظیم پنل پیامک، حالت `console` فعال است و کد ورود فقط در لاگ سرور چاپ می‌شود (برای تست).

## دیپلوی

### لیارا (Liara)
1. در `liara.ir` یک اپ از نوع **Docker** بساز (چون `Dockerfile` داریم) یا از نوع Node.
2. یک **دیسک (Disk)** بساز و روی مسیر `/app/data` وصلش کن تا داده پاک نشود.
3. متغیرهای محیطی را ست کن: `JWT_SECRET`، `ADMIN_TOKEN`، و کلیدهای پیامک.
4. با `liara deploy` بالا بیاور. آدرس اپ (مثل `https://xxx.liara.run`) همان `BACKEND_URL` است.

### هر VPS ایرانی (با Docker)
> برای دیپلوی روی لیارا نیازی به Docker نیست؛ فایل داکر به اسم `Dockerfile.vps` است تا لیارا از بیلدپکِ Node استفاده کند. برای VPS با `-f` استفاده کن:
```bash
docker build -f server/Dockerfile.vps -t coveredcall-backend ./server
docker run -d -p 3000:3000 \
  -e JWT_SECRET=... -e ADMIN_TOKEN=... \
  -e SMS_PROVIDER=kavenegar -e KAVENEGAR_API_KEY=... -e KAVENEGAR_TEMPLATE=... \
  -v $PWD/data:/app/data --name coveredcall coveredcall-backend
```

سپس آدرس عمومی سرور (`http://<ip>:3000` یا دامنه با HTTPS) را در اپ اندروید به‌عنوان `BACKEND_URL` بگذار.
> توصیه: جلوی سرور یک reverse proxy با HTTPS (مثل Caddy/Nginx) بگذار.

## پنل پیامک (کاوه‌نگار)

1. در پنل کاوه‌نگار یک **الگو (Template)** برای کد تأیید بساز و تأیید بگیر (مثلاً نامش `verify`).
2. این متغیرها را ست کن:
   ```
   SMS_PROVIDER=kavenegar
   KAVENEGAR_API_KEY=<کلید>
   KAVENEGAR_TEMPLATE=verify
   ```
برای SMS.ir به‌جای این‌ها `SMSIR_API_KEY` و `SMSIR_TEMPLATE_ID` را ست کن.

## مدیریت (ادمین)

همه‌ی endpointهای زیر هدر `x-admin-token: <ADMIN_TOKEN>` می‌خواهند.

**ساخت کد اشتراک** (این کدها را به کاربرانِ پرداخت‌کرده بده تا در اپ وارد کنند):
```bash
curl -X POST https://<backend>/admin/activation-codes \
  -H 'x-admin-token: ...' -H 'Content-Type: application/json' \
  -d '{"count":5,"days":30}'
```
یا آفلاین از خط فرمان: `npm run gen-codes 5 30`

**فعال‌سازی مستقیم اشتراک یک شماره:**
```bash
curl -X POST https://<backend>/admin/subscription \
  -H 'x-admin-token: ...' -H 'Content-Type: application/json' \
  -d '{"phone":"09121234567","days":30}'
```

**دیدن و پاسخ به سوالات کاربران:**
```bash
curl https://<backend>/admin/feedback -H 'x-admin-token: ...'
curl -X POST https://<backend>/admin/feedback/reply \
  -H 'x-admin-token: ...' -H 'Content-Type: application/json' \
  -d '{"id":"f_xxx","reply":"سلام، ممنون از بازخوردت"}'
```

## فهرست endpointها

| متد | مسیر | توضیح |
|-----|------|-------|
| GET | `/health` | بررسی سلامت سرور |
| POST | `/auth/request-otp` | `{phone}` → ارسال کد |
| POST | `/auth/verify-otp` | `{phone, code}` → `{token, user}` |
| GET | `/me` | پروفایل کاربر (نیازمند توکن) |
| POST | `/me` | `{displayName}` بروزرسانی نام |
| POST | `/subscription/redeem` | `{code}` فعال‌سازی اشتراک |
| GET | `/feedback` | پیام‌های کاربر |
| POST | `/feedback` | `{message}` ارسال پیام |

## نکته‌ی ماندگاری داده

نسخه‌ی فعلی داده را در `data/db.json` نگه می‌دارد (ساده و بدون وابستگی). برای بار بالا،
لایه‌ی `src/store.js` را می‌توان به PostgreSQL/SQLite ارتقا داد بدون تغییر بقیه‌ی کد.
