# siri-gemini

**AirPods H2 stem-press → on-device assistant on Android / GrapheneOS**

LibrePods-class AAP scaffold · Continuity BLE · home-screen widget (battery + ANC) · VoiceInteractionService · S-tier UI · offline STT · OTA

---

## Download (signed APK)

**https://github.com/352fihi-beep/siri-gemini/releases/download/v0.3.0/siri-gemini-0.3.0.apk**

> This link goes live after you build, sign, and attach the APK to a GitHub Release tagged `v0.3.0`.
> A real signed binary **cannot** be produced in the remote agent environment (no Android SDK / keystore).

### Ship the signed APK (required local step)

```bash
git clone https://github.com/352fihi-beep/siri-gemini.git && cd siri-gemini

# 1. Keystore (once)
keytool -genkey -v -keystore siri-gemini.keystore -alias siri-gemini \
  -keyalg RSA -keysize 2048 -validity 10000

# 2. Open in Android Studio → let it create Gradle wrapper + icons
# 3. Build → Generate Signed Bundle / APK → APK → release
#    or: ./gradlew :app:assembleRelease   (with signingConfigs in build.gradle.kts)

# 4. Create GitHub Release v0.3.0 and upload siri-gemini-0.3.0.apk
```

After that, the in-app OTA worker will notify users automatically.

---

## Status

| Feature | Status |
|---------|--------|
| Continuity BLE + adaptive scan | ✅ |
| AAP protocol scaffold (stem, ANC, battery, ears) | ✅ Hardened |
| Home-screen widget (battery + ANC) | ✅ |
| Stem → VoiceInteractionSession | ✅ |
| Glassmorphic UI + control center | ✅ |
| Offline STT (system + Vosk-ready) | ✅ |
| AICore detection | ✅ |
| OTA + download notification | ✅ |
| Exact LibrePods wire packets | ⏳ Align with their `airpods_packets` |
| Signed APK on Releases | ⏳ Local build + upload |

## Widget

Add **AirPods Battery & ANC** from the launcher widget picker. Shows L / R / Case levels and one-tap Off / ANC / Transparency. Updates from Continuity ads and AAP events.

## Proprietary packet layer — honest status

The AAP layer now has:
- Stable opcodes and event types
- Framed command builder
- Stem / battery parsers
- Connection + widget refresh path

**It is not a complete byte-compatible LibrePods clone.** Finishing the proprietary handshake requires copying the exact sequences from [LibrePods](https://github.com/librepods-org/librepods) (their RE is the authoritative source). Until then, Continuity + manual/dev stem trigger remain the reliable paths.

## Build (debug)

```bash
./gradlew :app:assembleDebug
```

Grant BLE, location, mic → Start listener → set as default assistant → add widget.
