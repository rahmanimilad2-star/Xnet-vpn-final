# تغییرات این نسخه (اپ اندروید)
- `ui/Screens.kt` (نسخه شما با زبان انگلیسی/حالت تاریک):
  - `HapticButton` پارامتر `modifier` گرفت و به `Button` داخلی پاس می‌شود (خطای 255).
  - `when (vpn.phase)` در `HomeScreen` برای همه حالت‌ها `Pair<String, Color>` برمی‌گرداند (خطاهای 396 و 410).
  - `@OptIn(ExperimentalMaterial3Api::class)` روی `AccountScreen` برای `FilterChip`.
  - صفحه سرورها: برچسب «متصل / Connected» کنار سروری که الان به آن وصل هستید.
- `ui/MainActivity.kt`: پارامترهای جدید `AppRoot` (زبان، حالت تاریک) با ذخیره در SharedPreferences؛ جهت صفحه در انگلیسی LTR و در فارسی RTL.
- `data/Repository.kt`: رفع خطای syntax در `private set`.
- `data/ApiClient.kt`: رفع ارجاع به تابع companion و پیش‌فرض کد خطا.
- Gradle: AGP 8.7.3، Kotlin 2.0.21 + پلاگین Compose، Gradle wrapper 8.14.3، compileSdk/targetSdk 34، Build Tools 34.0.0، سازگار با Java 21.
- Manifest: حذف `extractNativeLibs` (با `useLegacyPackaging` جایگزین شده بود).
- CI: `.github/workflows/android.yml` برای ساخت خودکار APK.
منطق VPN، API و backend تغییری نکرده است.

## به‌روزرسانی: ویبره و اطلاعات توسعه‌دهنده
- ویبره کوتاه و ظریف (۱۵ms، شدت کم) برای: تب‌های پایین، انتخاب سرور، ورود، نمایش/مخفی کردن رمز، به‌روزرسانی، خروج، حالت تاریک و زبان.
- دکمه اتصال/قطع VPN ویبره کمی محسوس‌تر (۳۵ms) دارد. فاصله حداقل ۸۰ms بین دو ویبره، تا با ضربه‌های پشت سر هم لرزش ممتد ایجاد نشود.
- اگر گوشی ویبره نداشته باشد یا خطا بدهد، بی‌صدا نادیده گرفته می‌شود. مجوز `VIBRATE` به Manifest اضافه شد (قبلاً نبود و ویبره عملاً کار نمی‌کرد).
- نام توسعه‌دهنده در کارت «توسعه‌دهنده» صفحه حساب: **Milad Rahmani**، همراه با نام و نسخه اپ. (`developer_name` در strings.xml هم اضافه شد.)
