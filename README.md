# siri-gemini

**AirPods H2 stem-press → native on-device Gemini-style assistant on Android / GrapheneOS**

BLE Continuity parsing · adaptive scan · VoiceInteractionService · glassmorphic UI · local STT · OTA · **zero cloud API keys**

---

## Status — v0.2.0-qol (all parallel tracks landed)

| Track | Status |
|-------|--------|
| 1. BLE ScanFilter (Apple `0x004C`) + adaptive duty + event bus | ✅ Done |
| 2. Glassmorphic session UI + local STT skeleton | ✅ Done |
| 3. AICore / Gemini Nano feature-flag bridge | ✅ Scaffold (flag off) |
| 4. GitHub Releases OTA via WorkManager | ✅ Done |

## Quick start

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
- Signed release + proper notification for OTA APK download

## Credits

LibrePods RE, Apple Continuity research (PETS / ShmooCon), Android VoiceInteractionService docs.

Built for people who want the AirPods force sensor to feel first-class on a locked-down Android.
