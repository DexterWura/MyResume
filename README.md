# MyResume

Local-first Android resume builder (Kotlin, Jetpack Compose) for Google Play.

## Features

- Create, edit, duplicate, and delete resumes on-device (Room)
- 200+ template presets across six layout classes
- Shared canvas renderer for preview and PDF export
- Free tier: editing, free templates, PDF export
- Google Play Billing unlocks: `premium_templates`, `ai_assistant`
- On-device AI (Gemma 3 1B via LiteRT-LM) behind the AI unlock

## Requirements

- Android Studio Ladybug or newer / JDK 17
- Android SDK 36
- `minSdk` 26, `targetSdk` 36

## Configure

1. Copy values into `local.properties` (already has `sdk.dir` when generated locally):

```
sdk.dir=C:\\Users\\You\\AppData\\Local\\Android\\Sdk
MODEL_URL=https://your-hosted-url/gemma3-1b-it-int4.litertlm
PRIVACY_POLICY_URL=https://your.site/myresume-privacy
```

2. Accept Gemma terms on Hugging Face, download the CPU int4 `.litertlm` from [litert-community/Gemma3-1B-IT](https://huggingface.co/litert-community/Gemma3-1B-IT), host it over HTTPS, and set `MODEL_URL`. Do not put a Hugging Face token in the app.

3. In Play Console, create one-time products:
   - `premium_templates`
   - `ai_assistant`

4. Change `applicationId` / namespace from `com.myresume.app` to a package name you control before the first upload.

## Build

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:bundleRelease
```

Debug builds use a purchase simulator (`DebugBillingGateway`) so unlocks can be tested without Play products.

After a release build that includes native libs, verify 16 KB alignment:

```bash
zipalign -c -P 16 -v 4 app/build/outputs/apk/release/app-release.apk
```

## Play policy notes

- Digital unlocks use Google Play Billing only
- No EcoCash / Paynow / Pesepay / PayPal in this Play build
- Permissions: Internet + network state only
- Photos via system photo picker (when wired); PDFs via Storage Access Framework + FileProvider
- Privacy policy URL required before store listing
- AI disclosure and apply-before-write behavior are built into the AI screen
