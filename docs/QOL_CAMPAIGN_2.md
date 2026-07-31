# QoL Campaign 2 — status after parallel batch

| # | Item | Status |
|---|------|--------|
| 1 | Firmware readout | ✅ `FirmwareInfo` — stores only real AAP strings |
| 2 | Long-press listening mode | ✅ via `stemLong` pref (default `anc_cycle`) |
| 3 | Leave-behind reminder | ✅ `LeaveBehindMonitor` real RSSI streak |
| 4 | Case charging sound toggle | ✅ pref `caseChargeSound` (AAP write when framed) |
| 5 | Spatializer status | ✅ `SpatializerStatus` Android 13+ API |
| 6 | Offline intent expansion | ✅ timers, alarms, media, ANC, DND, mute |
| 7 | Notification ANC actions | ✅ Off / ANC / Trans on battery notif |
| 8 | Per-bud balance pref | ✅ `budBalance` 0–100 |
| 9 | RSSI quality | ✅ `RssiTracker` real samples only |
| 10 | No-network mode | ✅ skips OTA when enabled |
| 11 | UARP awareness | 🔶 scaffold via firmware field |
| 12 | Find My | ❌ privacy non-goal |
| 13 | HID head-tracking inject | ❌ needs root/RE |
| 14 | Local LLM bridge | ✅ `LocalLlmBridge` no mock answers |
| 15–20 | Live Listen / HRM / Wear / Xposed | documented non-goals or deferred |

CI: `.github/workflows/android.yml` builds debug APK on push; optional signed release via secrets.
