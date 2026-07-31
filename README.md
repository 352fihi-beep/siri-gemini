# siri-gemini

**AirPods H2 stem-press → native on-device Gemini-style assistant on Android / GrapheneOS**

LibrePods-class AAP/L2CAP scaffold · Continuity BLE · VoiceInteractionService · S-tier glassmorphic UI · offline STT · AICore · OTA

---

## Download (pre-signed APK)

**https://github.com/352fihi-beep/siri-gemini/releases/download/v0.2.0/siri-gemini-0.2.0.apk**

> Publish a GitHub Release tagged `v0.2.0` with the signed APK to make this link live.
> In-app OTA will then notify users automatically.

---

## What is implemented (v0.3-dev)

| Area | Status |
|------|--------|
| Continuity BLE scan + adaptive duty | ✅ |
| AAP / L2CAP protocol scaffold (opcodes, stem, ANC, battery, ears) | ✅ Scaffold |
| Stem-press → VoiceInteractionSession | ✅ |
| Noise-control UI (Off / ANC / Transparency / Adaptive) | ✅ |
| S-tier glassmorphic animations | ✅ |
| Offline STT (system + Vosk-ready) | ✅ |
| AICore / Gemini Nano detection | ✅ |
| OTA + download notification | ✅ |
| Full LibrePods packet sequences + native L2CAP | ⏳ Requires completing RE against LibrePods source |
| Vosk model + JNI | ⏳ Drop model into `assets/` + add dependency |
| Real AICore generate() | ⏳ Add AI Edge SDK on supported Pixel |
| Signed binary hosted on Releases | ⏳ Build + upload once |

## Honest limits

- **Stem force-sensor reliability** needs the real AAP handshake and packet formats from [LibrePods](https://github.com/librepods-org/librepods). The scaffold defines the stable API; the wire format must be finished against their RE.
- **Vosk** activates when a model is present under `assets/model/`; the AAR is not yet declared so the APK stays lean by default.
- **AICore** is detected and gated; actual generation requires the Google AI Edge dependency and a supported device.
- **Signed APK** cannot be produced in this environment — build locally and attach to a GitHub Release.

## Quick start

```bash
git clone https://github.com/352fihi-beep/siri-gemini.git
cd siri-gemini
# Android Studio → sync / generate wrapper + icons
./gradlew :app:assembleDebug
```

1. Install → grant BLE, location, mic
2. **Start gesture + AAP listener**
3. Set as default assistant
4. Use noise chips / simulate stem / real Continuity ads

## Architecture

```
ble/
  ContinuityParser      — Apple 0x004C ads
  GestureEventBus       — stem / nearby events
  aap/
    AapProtocol         — opcodes, NoiseMode, StemAction, BatteryInfo
    AapConnection       — L2CAP/RFCOMM scaffold + event decode
  AirPodsGestureService — scan + AAP lifecycle
voice/                  — VoiceInteractionService + Session
ai/
  LocalSttEngine        — offline-first (Vosk → system)
  AiCoreBridge          — Nano detection + generate gate
ota/OtaWorker           — GitHub Releases + notification
ui/                     — S-tier glass + control center
```

Built for people who want AirPods to feel first-class on a locked-down Android.
