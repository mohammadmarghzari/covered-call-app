# 🌐 VPN Sales Bot — ربات فروش کانفیگ VPN در تلگرام

یک سیستم کامل، حرفه‌ای و Production-Ready برای فروش خودکار کانفیگ VPN در تلگرام.
کاربر پلن را انتخاب می‌کند، پرداخت می‌کند و رسید می‌فرستد؛ مدیر فقط با **ریپلای**
لینک کانفیگ را ارسال می‌کند و ربات به‌صورت **کاملاً خودکار** کانفیگ، عکس، متن و
دکمه‌های دانلود را برای مشتری می‌فرستد.

---

## ✨ امکانات

**کاربر**
- 🛒 خرید کانفیگ با لیست پلن‌های پویا (بدون تغییر کد قابل افزودن)
- 💳 پرداخت با کارت بانکی (نمایش شماره کارت، نام صاحب کارت، مبلغ + دکمهٔ کپی)
- 🪙 پرداخت با ارز دیجیتال (آدرس + **QR Code** + دکمهٔ کپی برای هر کیف پول)
- 📸 ارسال تصویر رسید
- 📦 «خریدهای من» با نمایش وضعیت و لینک کانفیگ
- ❓ آموزش اتصال و 📞 پشتیبانی
- 🇮🇷 رابط کاربری کاملاً فارسی، مدرن و مبتنی بر Inline Keyboard

**تحویل خودکار**
- رسید کاربر همراه با نام، یوزرنیم، آیدی عددی، پلن، مبلغ، نوع پرداخت و زمان به
  کانال خصوصی مدیر ارسال می‌شود.
- مدیر روی همان پیام **Reply** می‌کند و فقط لینک کانفیگ (`vless://`, `vmess://`, ...)
  را می‌فرستد.
- ربات فوراً برای مشتری ارسال می‌کند: ✅ تأیید پرداخت → لینک کانفیگ → عکس →
  متن دلخواه → دکمه‌های دانلود (اندروید/آیفون/ویندوز/مک).
- برای رد سفارش کافی است مدیر با کلمهٔ «رد» ریپلای کند.

**پنل مدیریت (داخل ربات)**
- ➕ افزودن / ✏️ ویرایش / 🗑 حذف پلن (قیمت، حجم، مدت، توضیحات، فعال/غیرفعال)
- 🪙 مدیریت کیف پول‌های ارز دیجیتال
- 💳 ویرایش شماره کارت و نام صاحب کارت
- ✏️ ویرایش تمام متن‌ها و لینک‌های دانلود
- 🖼 تنظیم عکس تحویل
- 🧾 مشاهده و 🔎 جستجوی سفارش‌ها (بر اساس شمارهٔ سفارش، آیدی یا یوزرنیم)
- 📊 آمار فروش و 👥 آمار کاربران
- 🚫 بلاک/رفع بلاک کاربر
- 📢 ارسال پیام همگانی (هر نوع محتوا)

**امنیت و پایداری**
- 🛡 Anti-Spam و Rate-Limit مبتنی بر Redis
- ✅ Validation ورودی‌ها
- 👥 پشتیبانی از چند مدیر
- 🧾 ثبت لاگ رخدادها (Audit Log)
- 💾 اسکریپت بکاپ خودکار دیتابیس
- 🐳 اجرای کامل با Docker

---

## 🧱 معماری و تکنولوژی

| لایه | تکنولوژی |
|------|----------|
| زبان | Python 3.12 |
| فریم‌ورک ربات | aiogram 3.x (async) |
| دیتابیس | PostgreSQL 16 |
| ORM | SQLAlchemy 2.0 (async) + Alembic |
| کش / FSM / RateLimit | Redis 7 |
| کانتینر | Docker + Docker Compose |
| لاگ | structlog |

ساختار پروژه بر پایهٔ **Clean Architecture** و کاملاً ماژولار است:

```
app/
├── main.py            # نقطهٔ ورود (long-polling)
├── bot.py             # ساخت Bot/Dispatcher، میدل‌ورها، startup
├── config.py          # تنظیمات از محیط (pydantic-settings)
├── logger.py          # لاگ ساخت‌یافته
├── filters.py         # فیلتر IsAdmin
├── database/          # engine، Base و مدل‌های ORM
├── repositories/      # لایهٔ دسترسی به داده
├── services/          # منطق کسب‌وکار (آمار، تنظیمات، کانتینر ریپازیتوری)
├── handlers/          # هندلرهای تلگرام
│   └── admin/         # پنل مدیریت + تحویل خودکار کانفیگ
├── keyboards/         # کیبوردهای Inline و CallbackData
├── middlewares/       # DB session، Throttling، Ban
├── states/            # State‌های FSM
└── utils/             # فرمت‌بندی فارسی، QR، Validation، ثوابت
alembic/               # مهاجرت‌های دیتابیس
scripts/               # install / deploy / backup / systemd
nginx/                 # کانفیگ اختیاری برای حالت webhook
```

---

## 🚀 نصب سریع روی Ubuntu 24.04

### ۱) دریافت کد و ورود به پوشه
```bash
git clone <REPO_URL> vpn-sales-bot
cd vpn-sales-bot
```

### ۲) نصب خودکار (Docker + راه‌اندازی)
```bash
sudo bash scripts/install.sh
```
اسکریپت، Docker را نصب می‌کند و در صورت نبود فایل `.env`، آن را از روی نمونه
می‌سازد و از شما می‌خواهد مقادیر را ویرایش کنید.

### ۳) تنظیم متغیرها
```bash
cp .env.example .env   # اگر هنوز ساخته نشده
nano .env              # مقادیر واقعی را وارد کنید
```
حداقل مقادیر لازم: `BOT_TOKEN`، `ADMIN_IDS`، `ADMIN_CHANNEL_ID`،
`POSTGRES_PASSWORD` (و در نتیجه `DATABASE_URL`).

### ۴) اجرا
```bash
sudo bash scripts/install.sh     # بار دوم: build و اجرا می‌کند
# یا به‌صورت دستی:
docker compose up -d --build
docker compose logs -f bot
```

مهاجرت‌های دیتابیس به‌صورت خودکار توسط سرویس `migrate` اجرا می‌شوند و تنظیمات
پیش‌فرض در اولین اجرا Seed می‌شوند.

---

## 🤖 آماده‌سازی در تلگرام (مهم)

1. یک ربات از **@BotFather** بسازید و `BOT_TOKEN` را در `.env` قرار دهید.
2. آیدی عددی مدیر(ها) را از **@userinfobot** بگیرید و در `ADMIN_IDS` بگذارید
   (چند مدیر با کاما جدا می‌شوند).
3. یک **کانال خصوصی** بسازید، **ربات را در آن Admin کنید** و آیدی کانال را
   (به شکل `-100XXXXXXXXXX`) در `ADMIN_CHANNEL_ID` قرار دهید.
   - ساده‌ترین راه گرفتن آیدی: یک پیام از کانال را به @userinfobot فوروارد کنید،
     یا به‌طور موقت @username_to_id_bot را ادمین کنید.
4. `/start` را به ربات بزنید؛ برای ورود به پنل مدیریت `/admin` را بزنید.
5. از داخل پنل، پلن‌ها، کیف پول‌ها، شماره کارت، متن‌ها، لینک‌های دانلود و عکس
   تحویل را تنظیم کنید.

> نکته: ربات به‌صورت پیش‌فرض روی **Long-Polling** کار می‌کند؛ نیازی به دامنه،
> IP ثابت یا SSL نیست. کانفیگ Nginx فقط برای حالت اختیاری Webhook ارائه شده است.

---

## 🔧 دستورات پرکاربرد (Makefile)

```bash
make up        # ساخت و اجرای کامل
make logs      # مشاهدهٔ لاگ
make down      # توقف
make restart   # ری‌استارت ربات
make migrate   # اعمال مهاجرت‌ها
make backup    # بکاپ دیتابیس
```

---

## 💾 بکاپ خودکار

اسکریپت `scripts/backup.sh` از دیتابیس خروجی `.sql.gz` می‌گیرد و بکاپ‌های قدیمی
را پاک می‌کند. برای اجرای روزانه در ساعت ۳ بامداد به crontab اضافه کنید:
```cron
0 3 * * * /opt/vpn-sales-bot/scripts/backup.sh >> /var/log/vpnbot-backup.log 2>&1
```

---

## ⚙️ اجرا به‌عنوان سرویس systemd (اختیاری)

اگر می‌خواهید به‌جای اتکا به `restart: unless-stopped` داکر، استارت خودکار در
بوت سیستم را با systemd مدیریت کنید:
```bash
sudo cp scripts/vpnbot.service /etc/systemd/system/vpnbot.service
# مسیر WorkingDirectory را در فایل مطابق محل نصب ویرایش کنید
sudo systemctl daemon-reload
sudo systemctl enable --now vpnbot
```

---

## 🔐 SSL و Webhook (اختیاری)

حالت پیش‌فرض Polling است و به SSL نیاز ندارد. برای استفاده از Webhook:
```bash
sudo apt install -y certbot python3-certbot-nginx
sudo certbot --nginx -d bot.example.com
# سپس از nginx/vpnbot.conf.example به‌عنوان الگو استفاده کنید
```

---

## 🧪 توسعه

```bash
python3.12 -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
# اجرای مهاجرت‌ها
alembic upgrade head
# اجرای ربات
python -m app.main
# ساخت مهاجرت جدید پس از تغییر مدل‌ها
alembic revision --autogenerate -m "your message"
```

---

## 📜 مجوز

این پروژه برای استفادهٔ شخصی/تجاری شما آماده شده است.
