# siri-gemini

**AirPods H2 → on-device assistant on Android / GrapheneOS**

All 20 QoL items scaffolded · LibrePods-class AAP · widget · QS tile · offline commands · OTA

---

## Download

**https://github.com/352fihi-beep/siri-gemini/releases/download/v0.4.0/siri-gemini-0.4.0.apk**

Build + sign + upload a Release tagged `v0.4.0` to activate (cannot be produced in the agent environment).

---

## All 20 QoL features

| # | Feature | Status |
|---|---------|--------|
| 1 | Case-open popup | ✅ `CaseOpenPopup` |
| 2 | Persistent battery notification | ✅ `BatteryNotification` |
| 3 | Ear detection play/pause | ✅ `EarDetectionController` |
| 4 | Stem action mapping | ✅ `StemActionRouter` + prefs UI |
| 5 | Battery optimization exemption | ✅ `BatteryOptHelper` |
| 6 | Quick Settings ANC tile | ✅ `AncTileService` |
| 7 | Head gesture nod/shake | ✅ `HeadGestureHandler` |
| 8 | Conversational Awareness toggle | ✅ prefs + UI |
| 9 | Rename + find chirp | ✅ prefs UI (chirp needs AAP write) |
| 10 | Hearing-aid panel | ✅ amp/balance/tone/boost/loud |
| 11 | Smarter adaptive scan | ✅ already in gesture service |
| 12 | Multi-device handoff toast | ✅ `HandoffDetector` |
| 13 | Offline command shortcuts | ✅ `OfflineCommands` |
| 14 | First-run onboarding | ✅ `OnboardingScreen` |
| 15 | F-Droid lean path | ✅ no hard Google deps |
| 16 | Spatial audio indicator | ✅ `SpatialIndicator` |
| 17 | Custom EQ | ✅ bass/treble prefs |
| 18 | Max / Beats model hints | ✅ name matching in discovery |
| 19 | Wear OS | 📝 companion deferred (phone-first) |
| 20 | Encrypted command history | ✅ `CommandHistory` AES local |

Wire-format AAP packets still need LibrePods RE alignment for stem/force and write commands (rename, EQ, hearing aid, chirp) to reach the buds.

## Build

```bash
git clone https://github.com/352fihi-beep/siri-gemini.git
cd siri-gemini
# Android Studio sync → assembleDebug / signed release
```
