# siri-gemini

AirPods H2 → on-device assistant · GrapheneOS-friendly · no mock data · Campaign 1+2 QoL

## CI / download

GitHub Actions builds **debug APK** on every push to `main` (Actions → artifacts).

Signed release: set repo secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` and run workflow_dispatch **release** job.

Manual: `https://github.com/352fihi-beep/siri-gemini/releases` after you upload.

## Campaign 2 (implemented)

Firmware field · leave-behind RSSI · Spatializer API · expanded offline intents · notification ANC actions · RSSI quality · no-network mode · stem long-press mapping · balance / case-charge prefs · local LLM bridge (no fake output).

See `docs/QOL_CAMPAIGN_2.md`.

## Build locally

```bash
git clone https://github.com/352fihi-beep/siri-gemini.git
# Android Studio once to generate wrapper + icons
./gradlew :app:assembleDebug
```
