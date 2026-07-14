"""Setting keys and their default values.

All of these are editable at runtime from the in-bot admin panel and are
stored in the `settings` table. Defaults are seeded on first run.
"""
from __future__ import annotations


class SettingKey:
    WELCOME_TEXT = "welcome_text"
    SUPPORT_USERNAME = "support_username"          # e.g. @support
    TUTORIAL_TEXT = "tutorial_text"

    CARD_NUMBER = "card_number"
    CARD_HOLDER = "card_holder"

    DELIVERY_TEXT = "delivery_text"                # sent with the config
    DELIVERY_PHOTO_FILE_ID = "delivery_photo_file_id"

    DOWNLOAD_ANDROID = "download_android"
    DOWNLOAD_IPHONE = "download_iphone"
    DOWNLOAD_WINDOWS = "download_windows"
    DOWNLOAD_MAC = "download_mac"


DEFAULT_SETTINGS: dict[str, str] = {
    SettingKey.WELCOME_TEXT: (
        "🌐 به فروشگاه اینترنتی ما خوش آمدید!\n\n"
        "با استفاده از دکمه‌های زیر می‌توانید به‌سادگی کانفیگ پرسرعت و پرقدرت "
        "تهیه کنید. 🚀\n\n"
        "لطفاً یکی از گزینه‌ها را انتخاب کنید:"
    ),
    SettingKey.SUPPORT_USERNAME: "@support",
    SettingKey.TUTORIAL_TEXT: (
        "❓ <b>آموزش اتصال</b>\n\n"
        "۱️⃣ ابتدا برنامهٔ مناسب سیستم‌عامل خود را از دکمه‌های دانلود نصب کنید.\n"
        "۲️⃣ لینک کانفیگی که پس از خرید دریافت می‌کنید را کپی کنید.\n"
        "۳️⃣ در برنامه، گزینهٔ افزودن از کلیپ‌بورد (Import from Clipboard) را بزنید.\n"
        "۴️⃣ روی دکمهٔ اتصال (Connect) بزنید و از اینترنت آزاد لذت ببرید! ✅\n\n"
        "در صورت بروز هر مشکلی با پشتیبانی در ارتباط باشید."
    ),
    SettingKey.CARD_NUMBER: "6037-9911-1111-1111",
    SettingKey.CARD_HOLDER: "نام صاحب کارت",
    SettingKey.DELIVERY_TEXT: (
        "🎉 خرید شما با موفقیت تکمیل شد!\n\n"
        "کانفیگ اختصاصی شما در پیام بالا ارسال شد. برای اتصال، برنامهٔ مناسب "
        "دستگاه خود را از دکمه‌های زیر دانلود و کانفیگ را وارد کنید.\n\n"
        "🙏 از خرید شما سپاسگزاریم."
    ),
    SettingKey.DELIVERY_PHOTO_FILE_ID: "",
    SettingKey.DOWNLOAD_ANDROID: "https://play.google.com/store/apps/details?id=com.v2raytun.android",
    SettingKey.DOWNLOAD_IPHONE: "https://apps.apple.com/us/app/streisand/id6450534064",
    SettingKey.DOWNLOAD_WINDOWS: "https://github.com/2dust/v2rayN/releases/latest",
    SettingKey.DOWNLOAD_MAC: "https://apps.apple.com/us/app/streisand/id6450534064",
}


# Editable-text metadata for the admin "edit texts" panel:
# key -> human readable Persian label. Order matters: the admin keyboard
# addresses each entry by its index (see EDITABLE_TEXT_KEYS).
EDITABLE_TEXTS: dict[str, str] = {
    SettingKey.WELCOME_TEXT: "متن خوش‌آمدگویی",
    SettingKey.TUTORIAL_TEXT: "متن آموزش اتصال",
    SettingKey.DELIVERY_TEXT: "متن تحویل کانفیگ",
    SettingKey.SUPPORT_USERNAME: "آیدی پشتیبانی",
    SettingKey.DOWNLOAD_ANDROID: "لینک دانلود اندروید",
    SettingKey.DOWNLOAD_IPHONE: "لینک دانلود آیفون",
    SettingKey.DOWNLOAD_WINDOWS: "لینک دانلود ویندوز",
    SettingKey.DOWNLOAD_MAC: "لینک دانلود مک",
}

# Stable, index-addressable list of the keys above.
EDITABLE_TEXT_KEYS: list[str] = list(EDITABLE_TEXTS.keys())
