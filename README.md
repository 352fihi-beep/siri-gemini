# siri-gemini

**AirPods H2 stem-press → native on-device Gemini-style assistant on Android / GrapheneOS**

BLE Continuity parsing · L2CAP gesture bridge · VoiceInteractionService · glassmorphic UI · OTA · **zero cloud API keys**

---

## Vision

Press the force sensor on an AirPods (H2 / Pro 2 / 3 / 4) stem and instantly get a private, on-device voice assistant experience that feels like a native system assistant. No Google account required for the core path, no API keys, GrapheneOS-first.

## Architecture (performance-first)

| Layer | Tech | Optimization notes |
|-------|------|--------------------|
| Gesture detection | Apple Continuity BLE ads + LibrePods-compatible L2CAP | ScanFilter on `0x004C`, adaptive duty cycle, ConnectedDevice FGS |
| Assistant surface | `VoiceInteractionService` + `VoiceInteractionSession` + `RecognitionService` | System-kept-alive, minimal wake locks |
| UI | Jetpack Compose + glassmorphism | RenderEffect blur (API 31+) with solid fallback, no heavy assets |
| Intelligence | AICore / Gemini Nano (when present) → quantized local STT/TTS | Zero network for core path |
| Updates | GitHub Releases + WorkManager | Doze-aware, differential |
| Build | R8 full mode, baseline profiles, minSdk 31 | Low-RAM friendly |

## Hardware & OS constraints addressed

- **GrapheneOS** — no hard dependency on Play Services / AICore. Core gesture → assistant path works with AOSP Bluetooth.
- **Low RAM** — aggressive shrinking, no ML by default, Compose only.
- **Battery** — BLE only aggressive when AirPods are detected nearby; otherwise low-duty or stopped.
- **Stem press reliability** — pure ad parsing is insufficient; the L2CAP bridge (inspired by [LibrePods](https://github.com/librepods-org/librepods)) is the viable path.

## Quick start (developer)

```bash
git clone https://github.com/352fihi-beep/siri-gemini.git
cd siri-gemini
./gradlew :app:assembleDebug
```

Requires Android Studio Ladybug+ / AGP 8.7+, JDK 17+.

## Status

Scaffold is production-oriented. Next concrete milestones:

1. Wire Continuity parser + basic stem-press event emission
2. Complete VoiceInteractionSession with glassmorphic overlay
3. Local STT fallback (Vosk / Whisper.cpp quantized)
4. Optional AICore binding
5. First signed release + OTA pipeline

## Credits & prior art

- LibrePods team (kavishdevar et al.) for the hard protocol RE
- Apple Continuity reverse-engineering papers (PETS, ShmooCon)
- Android VoiceInteractionService documentation

---

Built for people who want the AirPods force sensor to feel first-class on a locked-down Android.
