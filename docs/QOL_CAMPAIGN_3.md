# QoL Campaign 3 — Exhaustive (2026-08-03)

## Goal
Close remaining reliability, privacy, trigger, and assistant gaps so Siri Gemini is usable on GrapheneOS / Pixel without mock data or cloud API keys.

## Status matrix

| # | Item | Priority | Status | Notes |
|---|------|----------|--------|-------|
| 1 | Continuity TLV **0x08** (Hey Siri event) | P0 | ✅ | Cooldown + confidence; no false positives |
| 2 | Native Gemini path (AICore detect + **system Intent fallback**) | P0 | ✅ | No API key; clipboard/web fallback |
| 3 | `BLUETOOTH_SCAN` `neverForLocation` | P0 | ✅ | Drop FINE_LOCATION on API 31+ where possible |
| 4 | Live H2 StateFlow → UI / widget / battery notif | P1 | 🔶 | Partial via gesture service; wire remaining |
| 5 | Gradle wrapper committed | P0 | ❌ | CI still fails until wrapper or Studio generate |
| 6 | Launcher icons (mipmap) | P0 | ❌ | Manifest refs `@mipmap/ic_launcher` — missing assets |
| 7 | Default-assistant onboarding copy + deep link | P1 | 🔶 | Settings intent present; improve UX |
| 8 | Privacy Dashboard screen | P1 | 🔶 | Prefs exist; dedicated honest UI pending |
| 9 | WorkManager restart of gesture FGS | P1 | 🔶 | BootReceiver exists; periodic ensure |
| 10 | Vosk model packaging | P2 | ❌ | Optional offline STT; document size |
| 11 | AICore / AI Edge SDK real generate() | P1 | 🔶 | Detection real; generate still stub until dep |
| 12 | LibrePods-complete AAP L2CAP sequences | P2 | ❌ | Scaffold only; RE-heavy |
| 13 | One-click GitHub Release + APK asset | P1 | 🔶 | Workflow exists; softprops release optional |
| 14 | GrapheneOS-specific README section | P1 | ✅ | This campaign |
| 15 | Verification checklist (device) | P0 | ✅ | See below |

## Non-goals (unchanged)
- Find My network participation
- Root / Xposed / HID head-tracking injection
- Claiming iOS-parity H2 neural engine without AAP

## Verification checklist (device)

1. Install debug APK from Actions artifact.
2. Grant Bluetooth Scan/Connect + Mic (+ Notifications).
3. Start gesture + AAP listener → FGS notification appears.
4. Wear AirPods near phone → Continuity status updates (battery / in-ear) when ads present.
5. Stem / simulate stem → assistant path fires (session or Intent).
6. Talk now / default assistant → STT → Native Gemini router (AICore or system Gemini / clipboard).
7. Noise chips → service accepts Off/ANC/Trans/Adapt.
8. QS tile ANC toggles without crash.
9. Widget refresh does not crash.
10. Airplane mode → offline offline-commands still work; OTA skipped if no-network pref.
11. GrapheneOS: no crash when AICore package missing.

## Pre-build blockers
- Commit Gradle wrapper (`gradlew` + `gradle/wrapper/*`)
- Add adaptive launcher icons under `app/src/main/res/mipmap-*`
- Re-run CI after those two

## Next after Campaign 3 code lands
1. Push wrapper + icons
2. Trigger Actions → download debug APK
3. Run verification checklist on Pixel (GrapheneOS preferred)
4. Only then tag `v0.3.0` for signed release
