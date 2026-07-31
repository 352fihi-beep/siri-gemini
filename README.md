# siri-gemini

**AirPods H2 stem-press → native on-device Gemini-style assistant on Android / GrapheneOS**

BLE Continuity parsing · adaptive scan · VoiceInteractionService · glassmorphic UI · local STT · OTA · **zero cloud API keys**

---

## Download (pre-signed APK)

Once the first release is published the direct link will be:

**https://github.com/352fihi-beep/siri-gemini/releases/download/v0.2.0/siri-gemini-0.2.0.apk**

> Check the [Releases page](https://github.com/352fihi-beep/siri-gemini/releases) for the latest signed APK.
> The in-app OTA worker already points at this repository and will detect new tags automatically.

### How to publish the signed APK (one-time)

```bash
# 1. Create a keystore (only once)
keytool -genkey -v -keystore siri-gemini.keystore -alias siri-gemini \
  -keyalg RSA -keysize 2048 -validity 10000

# 2. Build the release APK (Android Studio or CLI)
./gradlew :app:assembleRelease

# 3. Sign + align (or use Android Studio "Generate Signed APK")
# 4. Create a GitHub Release tagged v0.2.0 and attach the APK
#    named siri-gemini-0.2.0.apk
```

After that the download URL above becomes live and the OTA checker will start offering updates.

---

## Status — v0.2.0-qol (all parallel tracks landed)

| Track | Status |
|-------|--------|
| 1. BLE ScanFilter (Apple `0x004C`) + adaptive duty + event bus | ✅ Done |
| 2. Glassmorphic session UI + local STT skeleton | ✅ Done |
| 3. AICore / Gemini Nano feature-flag bridge | ✅ Scaffold (flag off) |
| 4. GitHub Releases OTA via WorkManager | ✅ Done |

## Quick start (debug)

```bash
git clone https://github.com/352fihi-beep/siri-gemini.git
cd siri-gemini
# Open in Android Studio → let it generate wrapper + icons if missing
./gradlew :app:assembleDebug
```

1. Install the APK
2. Grant BLE + location + mic
3. Tap **Start gesture listener**
4. Set **Siri Gemini** as default assistant in system settings
5. Use **Simulate stem press** (dev) or wait for real Continuity ads / future L2CAP stem events

## Architecture highlights

- **AirPodsGestureService** — filtered BLE scan, adaptive LOW_POWER ↔ LOW_LATENCY, emits on `GestureEventBus`
- **SiriGeminiVoiceInteractionService** — listens for `StemPress`, calls `showSession`
- **LocalSttEngine** — system `SpeechRecognizer` with `EXTRA_PREFER_OFFLINE`
- **AiCoreBridge** — feature-flagged no-op until device + deps are ready
- **OtaWorker** — 12 h periodic GitHub Releases check, battery-aware

## Next (still open)

- Full L2CAP / AAP stem-press (LibrePods protocol) for reliable force-sensor events
- Vosk / Whisper.cpp quantized as stronger offline STT
- Real AICore binding when targeting supported Pixels
- First signed release uploaded to GitHub Releases

## Credits

LibrePods RE, Apple Continuity research (PETS / ShmooCon), Android VoiceInteractionService docs.

Built for people who want the AirPods force sensor to feel first-class on a locked-down Android.
