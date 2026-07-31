# siri-gemini

**AirPods H2 → on-device assistant on Android / GrapheneOS**

No mock data paths · real error handling · 20 QoL features · Continuity + AAP scaffold · widget · QS tile · offline commands

---

## Download

**https://github.com/352fihi-beep/siri-gemini/releases/download/v0.4.0/siri-gemini-0.4.0.apk**

Requires local signed release upload (agent environment cannot produce APKs).

---

## Data integrity

| Path | Behavior |
|------|----------|
| Continuity battery | `null` unless nibble 0–10 parsed — never invented |
| STT | System recognizer only; errors mapped to clear strings; no placeholder phrases |
| AAP | States: DISCONNECTED → CONNECTING → CONNECTED / FAILED with `lastError` |
| Case popup | Skips without overlay permission or without real battery fields |
| Offline commands | Run only on non-empty final transcripts |

## QoL campaigns

- **Campaign 1** — 20 items (popup, notif, ear pause, stem map, battery opt, QS tile, head gestures, conv awareness, rename, hearing aid, adaptive scan, handoff, offline cmds, onboarding, F-Droid lean, spatial indicator, EQ, Max/Beats, Wear deferred, encrypted history).
- **Campaign 2** — see [`docs/QOL_CAMPAIGN_2.md`](docs/QOL_CAMPAIGN_2.md) (firmware readout, leave-behind, Spatializer API, local LLM, UARP, etc.).

## Limits (honest)

- Exact AAP/L2CAP bytes: align with [LibrePods](https://github.com/librepods-org/librepods).
- Vosk/Whisper/AICore: optional deps, not simulated when absent.
- Signed APK: build on your machine.

```bash
git clone https://github.com/352fihi-beep/siri-gemini.git
# Android Studio → assembleDebug / signed release
```
