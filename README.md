# siri-gemini

AirPods H2 → on-device / native Gemini assistant · GrapheneOS-friendly · **no cloud API keys** · Campaign 1–3 QoL

## What works today
- Continuity BLE parse **0x07** (status) + **0x08** (Hey Siri events) — no invented batteries
- Gesture FGS + AAP scaffold (LibrePods-class direction)
- Native Gemini router: AICore detect → system Gemini Intent → clipboard
- Offline intents, ANC QS tile, battery notification, leave-behind RSSI, Spatializer, widget
- VoiceInteractionService stack for default-assistant path
- OTA check via WorkManager / GitHub Releases

## Campaign 3 (this push)
See `docs/QOL_CAMPAIGN_3.md` — 0x08 trigger, NativeGeminiRouter, permission direction, verification checklist.

## CI / download
Actions builds **debug APK** on push to `main` (Actions → artifacts).

**Pre-build blockers still open:**
1. Commit Gradle wrapper (`./gradlew` + `gradle/wrapper/`)
2. Add `@mipmap/ic_launcher` adaptive icons

Signed release: set secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` then workflow_dispatch **release**.

## Build locally
```bash
git clone https://github.com/352fihi-beep/siri-gemini.git
# Android Studio once to generate wrapper + icons if missing
./gradlew :app:assembleDebug
```

## GrapheneOS notes
- AICore is usually absent → Intent / clipboard path is expected
- Continuity + hybrid activation still work without GMS
- Prefer `neverForLocation` for BLE scan when not deriving location
