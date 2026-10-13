# ARMO Poster — Android App (arm64-v8a)

اپلیکیشن مستقل و آفلاین اندروید برای تولید پوسترهای مسابقه **ARMO NOTES** با کیفیت ۲۴۰۰×۲۴۰۰ پیکسل.

---

## 🌟 ویژگی‌ها (Key Features)

- **مخصوص گوشی‌های مدرن (arm64-v8a Only):** فیلتر معماری NDK فقط روی ۶۴ بیتی جهت بهینه‌سازی و کاهش ۵۰ درصدی حجم APK.
- **کاملاً آفلاین و بدون سرور:** بدون نیاز به اینترنت، Node.js یا رندرر ابری.
- **موتور رندر دقیق ۲۴۰۰×۲۴۰۰:** استفاده از WebView نیتیو اندروید و رندر پشت صحنه (Offscreen) به همراه بررسی وضعیت FITS در ۶ سطح (Fit Ladder).
- **پشتیبانی کامل از ۲۰ تیم لیگ برتر:** انتخاب آسان تیم‌ها، پر شدن خودکار رنگ سازمانی هر تیم و لوگوی رسمی.
- **رابط کاربری دوزبانه (Bilingual):** پشتیبانی کامل از زبان فارسی (راست‌چین RTL) و انگلیسی.
- **مدیریت تاریخچه و گردش کار سریع هفتگی:** امکان ذخیره، باز کردن مجدد، و ساخت پوستر جدید در کمتر از ۲ دقیقه با قابلیت «کپی و ویرایش» (Duplicate).
- **ذخیره در گالری و اشتراک‌گذاری در X:** ذخیره مستقیم در مسیر `Pictures/ARMO NOTES` و اشتراک‌گذاری از طریق منوی سیستمی.

---

## 🛠️ ساختار فنی (Architecture & Tech Stack)

- **سیستم‌عامل و تارگت:** Android 8.0+ (minSdk 26), targetSdk 35
- **معماری پردازنده:** `arm64-v8a`
- **زبان و UI:** Kotlin 2.0+ با Jetpack Compose و Material 3
- **رندرینگ:** AndroidX WebKit با `WebViewAssetLoader`
- **سریال‌سازی:** Kotlinx Serialization JSON
- **ذخیره‌سازی تصاویر:** Android MediaStore API بدون نیاز به دسترسی‌های اضافی

---

## 🚀 نحوه اجرا در Android Studio (Windows)

1. نرم‌افزار **Android Studio** را باز کنید.
2. گزینه **Open** را انتخاب کرده و پوشه پروژه را انتخاب کنید:
   ```text
   C:\Users\HOS3EIN\.gemini\antigravity\scratch\armo-poster-app
   ```
3. منتظر بمانید تا فرآیند **Gradle Sync** انجام شود.
4. گوشی اندرویدی خود (دارای پردازنده ۶۴ بیتی arm64) را با کابل متصل کنید (یا از شبیه‌ساز arm64 استفاده کنید).
5. دکمه **Run (Shift + F10)** را بزنید تا برنامه کامپایل و روی گوشی نصب شود.

---

## 📦 خروجی گرفتن APK نهایی (Build APK)

### ۱. نسخه دیباگ (سریع برای تست روی گوشی)
از منوی ترمینال داخل Android Studio یا خط فرمان:
```bash
gradlew assembleDebug
```
فایل APK در مسیر زیر تولید می‌شود:
`app/build/outputs/apk/debug/app-debug.apk`

### ۲. نسخه Release (امضاشده و بهینه‌سازی با Proguard/R8)
۱. ابتدا کلید اختصاصی خود را تولید کنید:
```bash
keytool -genkey -v -keystore keystore/armo-release.jks -alias armo -keyalg RSA -keysize 2048 -validity 10000
```
۲. فایل `keystore.properties` را در ریشه پروژه بسازید:
```properties
storeFile=keystore/armo-release.jks
storePassword=رمز_عبور_کی‌استور
keyAlias=armo
keyPassword=رمز_عبور_کلید
```
۳. دستور زیر را اجرا کنید:
```bash
gradlew assembleRelease
```
فایل نهایی در مسیر `app/build/outputs/apk/release/app-release.apk` قرار می‌گیرد.
قوانین R8 به گونه‌ای تنظیم شده که اینترفیس جاوااسکریپت WebView حفظ شده و به هیچ وجه حذف نمی‌شود.

---

## 🧪 داده‌های نمونه (Reference Match)
داده‌های مسابقه **Chelsea vs Bournemouth (GW6)** به صورت پیش‌فرض در `SampleData.kt` تعبیه شده و در اولین اجرای برنامه لود می‌شود تا بتوانید فوراً خروجی ۲۴۰۰×۲۴۰۰ را تست و تأیید کنید.
