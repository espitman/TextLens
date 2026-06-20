# TextLens — سند پیاده‌سازی برای Codex

## هدف محصول
TextLens یک اپ macOS است که کاربر با یک میانبر کیبورد یا آیکن منوبار، بخشی از صفحه را انتخاب می‌کند، اپ از همان ناحیه اسکرین‌شات می‌گیرد، متن داخل تصویر را با OCR استخراج می‌کند، سپس متن را با AI به فارسی ترجمه می‌کند و نتیجه را در یک پنجره کوچک روی صفحه نمایش می‌دهد.

این نسخه فقط حالت **Partial Translate** دارد. حالت‌های comic، full-screen، overlay جایگزین متن، و ترجمه کل صفحه فعلاً پیاده‌سازی نشوند.

---

## پلتفرم و تکنولوژی پیشنهادی

- زبان: Swift
- UI: SwiftUI + AppKit
- نوع اپ: macOS Menu Bar App
- OCR: Apple Vision Framework
- Screenshot: ScreenCaptureKit یا CGDisplayCreateImage برای MVP
- ترجمه AI: OpenAI-compatible API client با URL و API Key قابل تنظیم
- ذخیره تنظیمات: UserDefaults یا Keychain برای API Key
- حداقل macOS پیشنهادی: macOS 13+

---

## تجربه کاربری MVP

### جریان اصلی

1. کاربر اپ را اجرا می‌کند.
2. آیکن TextLens در منوبار نمایش داده می‌شود.
3. کاربر روی گزینه `Translate Area` کلیک می‌کند یا میانبر `Command + Shift + 0` را می‌زند.
4. کل صفحه کمی تاریک می‌شود و cursor به حالت انتخاب ناحیه می‌رود.
5. کاربر با drag یک مستطیل روی صفحه می‌کشد.
6. اپ از همان ناحیه screenshot می‌گیرد.
7. تصویر crop شده به Vision OCR داده می‌شود.
8. متن استخراج‌شده به API ترجمه ارسال می‌شود.
9. ترجمه فارسی در یک popup کوچک نزدیک محل انتخاب‌شده نمایش داده می‌شود.
10. کاربر با Escape یا کلیک بیرون popup آن را می‌بندد.

### حالت‌های خطا

- اگر permission اسکرین رکوردینگ داده نشده بود، صفحه راهنما باز شود.
- اگر متنی پیدا نشد، پیام `No text found in selected area` نمایش داده شود.
- اگر API Key تنظیم نشده بود، پنجره Settings باز شود.
- اگر ترجمه خطا داد، متن خطا ساده و قابل فهم نمایش داده شود.

---

## قابلیت‌های نسخه اول

### ضروری

- Menu bar app
- Global hotkey: `⌘⇧0`
- Selection overlay برای کشیدن مستطیل
- گرفتن screenshot از ناحیه انتخاب‌شده
- OCR با Vision
- ارسال متن به AI برای ترجمه فارسی
- نمایش popup نتیجه
- Settings ساده برای API Key، Base URL و Model
- Copy button برای کپی ترجمه

### غیرضروری در نسخه اول

- Comic mode
- Full screen translate
- ترجمه خودکار کل صفحه
- جایگزینی متن روی تصویر
- OCR چندستونه پیشرفته
- تاریخچه ترجمه‌ها
- اکانت کاربری
- پرداخت درون‌برنامه‌ای

---

## ساختار پیشنهادی پروژه

```text
TextLens/
  TextLensApp.swift
  AppDelegate.swift

  Core/
    ScreenshotService.swift
    OCRService.swift
    TranslationService.swift
    PermissionService.swift
    HotKeyService.swift

  UI/
    MenuBarController.swift
    SelectionOverlayWindow.swift
    SelectionOverlayView.swift
    TranslationPopupWindow.swift
    TranslationPopupView.swift
    SettingsView.swift

  Models/
    TranslationSettings.swift
    TranslationResult.swift
    OCRResult.swift

  Utils/
    CGRect+Helpers.swift
    ErrorPresenter.swift
```

---

## جزئیات فنی هر بخش

### 1. Menu Bar App

اپ نباید پنجره اصلی پیش‌فرض داشته باشد. بعد از اجرا فقط در منوبار نمایش داده شود.

منوی پیشنهادی:

```text
TextLens
- Translate Area
- Settings
- Quit
```

---

### 2. Global Hotkey

میانبر پیش‌فرض:

```text
Command + Shift + 0
```

برای MVP می‌توان از Carbon Event HotKey API یا پکیج سبک مانند KeyboardShortcuts استفاده کرد. اگر پکیج خارجی استفاده شد، دلیل آن در README ذکر شود.

---

### 3. Selection Overlay

وقتی کاربر ترجمه ناحیه را فعال می‌کند:

- یک borderless NSWindow روی همه صفحه‌ها باز شود.
- background نیمه‌شفاف تیره باشد.
- کاربر بتواند با mouse drag یک rectangle انتخاب کند.
- rectangle انتخاب‌شده border واضح داشته باشد.
- Escape انتخاب را cancel کند.
- بعد از mouse up، مختصات rectangle برگردانده شود و overlay بسته شود.

نکته مهم: پشتیبانی از چند مانیتور مطلوب است، ولی اگر برای MVP سخت شد، حداقل روی primary display درست کار کند و TODO برای multi-display گذاشته شود.

---

### 4. Screenshot

برای MVP ساده‌ترین پیاده‌سازی قابل قبول:

- گرفتن تصویر صفحه با CGDisplayCreateImage
- crop کردن تصویر بر اساس rectangle انتخاب‌شده

اگر ScreenCaptureKit استفاده شد، بهتر است permission handling تمیزتر نوشته شود.

خروجی این سرویس باید `CGImage` یا `NSImage` از ناحیه انتخاب‌شده باشد.

Signature پیشنهادی:

```swift
final class ScreenshotService {
    func capture(rect: CGRect, displayID: CGDirectDisplayID) throws -> CGImage
}
```

---

### 5. OCR با Vision

از `VNRecognizeTextRequest` استفاده شود.

تنظیمات پیشنهادی:

```swift
request.recognitionLevel = .accurate
request.usesLanguageCorrection = true
request.recognitionLanguages = ["en-US"]
```

برای MVP تمرکز روی ترجمه انگلیسی به فارسی است. در Settings بعداً می‌توان زبان مبدأ را اضافه کرد.

خروجی OCR باید text خام باشد، با line breakهای منطقی.

Signature پیشنهادی:

```swift
final class OCRService {
    func recognizeText(from image: CGImage) async throws -> String
}
```

---

### 6. Translation Service

ترجمه با API سازگار با OpenAI انجام شود.

Settings:

- API Key
- Base URL، پیش‌فرض: `https://api.openai.com/v1`
- Model، پیش‌فرض قابل تغییر، مثلاً `gpt-4o-mini`
- Target language، پیش‌فرض: Persian

Prompt پیشنهادی:

```text
Translate the following text to Persian.
Keep the meaning accurate and natural.
Do not add explanations.
If the text contains UI labels, keep the translation concise.

Text:
{{OCR_TEXT}}
```

Signature پیشنهادی:

```swift
protocol TranslationServiceProtocol {
    func translateToPersian(_ text: String) async throws -> String
}
```

خروجی باید فقط ترجمه باشد، نه توضیح اضافه.

---

### 7. Translation Popup

Popup باید:

- نزدیک ناحیه انتخاب‌شده باز شود.
- متن ترجمه را نمایش دهد.
- دکمه Copy داشته باشد.
- دکمه Close داشته باشد.
- با Escape بسته شود.
- ظاهر سبک و macOS-native داشته باشد.

برای MVP نیازی به pin کردن popup نیست.

---

## Permissionها

### Screen Recording Permission

macOS برای خواندن تصویر صفحه نیاز به Screen Recording permission دارد.

اپ باید:

1. در اولین اجرا یا اولین تلاش برای capture، permission را بررسی کند.
2. اگر permission نبود، پیام توضیحی نشان دهد.
3. کاربر را به System Settings ببرد.

متن پیشنهادی:

```text
TextLens needs Screen Recording permission to read text from the selected area of your screen.
```

---

## امنیت و حریم خصوصی

- API Key در حالت بهتر داخل Keychain ذخیره شود.
- برای MVP اگر UserDefaults استفاده شد، TODO برای Keychain گذاشته شود.
- تصویر screenshot نباید روی دیسک ذخیره شود.
- متن OCR و ترجمه فقط در حافظه نگهداری شود.
- هیچ telemetry یا analytics اضافه نشود.

---

## Acceptance Criteria

نسخه MVP زمانی قابل قبول است که:

1. اپ در منوبار اجرا شود.
2. با `⌘⇧0` بتوان selection overlay را باز کرد.
3. کاربر بتواند یک ناحیه از صفحه را انتخاب کند.
4. اپ از همان ناحیه تصویر بگیرد.
5. OCR متن انگلیسی ساده را درست استخراج کند.
6. متن استخراج‌شده به API ارسال شود.
7. ترجمه فارسی در popup نمایش داده شود.
8. دکمه Copy کار کند.
9. نبودن permission یا API Key باعث crash نشود.
10. README شامل روش اجرا، تنظیم API Key و محدودیت‌های نسخه اول باشد.

---

## README مورد انتظار

در README پروژه این موارد نوشته شود:

- TextLens چیست؟
- نیازمندی‌ها
- روش build و اجرا
- روش دادن Screen Recording permission
- روش تنظیم API Key
- shortcut پیش‌فرض
- محدودیت‌های MVP
- TODOهای نسخه بعد

---

## TODOهای نسخه بعد

- پشتیبانی بهتر از چند مانیتور
- انتخاب زبان مبدأ و مقصد
- تشخیص خودکار زبان
- تاریخچه ترجمه‌ها
- تنظیم shortcut از داخل Settings
- استفاده قطعی از Keychain برای API Key
- نمایش متن OCR اصلی کنار ترجمه
- Retry برای خطاهای شبکه
- پشتیبانی از Gemini / DeepL به عنوان provider جداگانه

---

## دستور پیشنهادی برای Codex

از این سند به عنوان spec استفاده کن و یک macOS menu bar app به نام TextLens بساز. تمرکز فقط روی MVP باشد: انتخاب ناحیه صفحه، OCR، ترجمه AI به فارسی و نمایش popup. از Swift، SwiftUI/AppKit، Vision و یک OpenAI-compatible translation client استفاده کن. حالت‌های comic و full-screen را پیاده‌سازی نکن. کد باید تمیز، ماژولار و قابل توسعه باشد و README کامل داشته باشد.
