# QoL Campaign 2 — Deep search (viable + experimental)

Sources: LibrePods issues/roadmap, CAPod/MaterialPods/AndroPods feature sets, Android spatial-audio AOSP docs, on-device LLM assistant research (2025–2026).

## Viable next (high confidence, no root)

1. **Firmware version readout** — LibrePods #612 phase 1: detect AirPods firmware string over AAP; show in UI; warn if very old.
2. **Press-and-hold listening-mode customization** — already partial in LibrePods free tier; map long-press exclusively to ANC cycle vs assistant.
3. **Leave-behind reminder** — notify if phone moves away while case left behind (BLE RSSI drop + no audio route). Experimental accuracy.
4. **Case charging sound toggle** — AAP write when packet known.
5. **Android 13+ spatializer status** — read `Spatializer` API (no AirPods head-tracking injection without HID/root).
6. **Offline intent expansion** — alarms, flashlight, DND, battery %, “what’s playing”.
7. **Notification actions** — ANC Off / ANC / Trans directly from persistent battery notification.
8. **Per-bud volume balance** — if AAP exposes it; else system `AudioManager` balance where available.
9. **Connection quality / RSSI graph** — from Continuity scan results (real dBm, no mock).
10. **Strict no-network mode** — kill OTA + any optional AICore path with one switch for GrapheneOS hardcore profiles.

## Experimental (RE / root / OEM dependent)

11. **UARP firmware update from Android** — LibrePods research; high risk; needs deep RE.
12. **Find My network enrollment** — planned upstream; likely root + Apple account surface (privacy conflict with project goals).
13. **Head-tracking HID injection for OS spatial audio** — AOSP supports BT HID head trackers; AirPods path not fully explored; may need root.
14. **On-device small LLM** (Phi / Gemma 2–4B via MediaPipe / ExecuTorch / picoLLM) after Whisper/Vosk STT — viable on flagship RAM; heavy APK.
15. **Live Listen / mic relay** — accessibility; legal/privacy sensitive.
16. **Heart-rate (Pro 3)** — LibrePods notes often need root.
17. **Adaptive Audio automatic switching** based on ambient mic — needs continuous mic + AAP.
18. **Multi-device seamless handoff control** (force take audio) — partial via audio routing APIs; full Apple-like needs AAP.
19. **Wear OS battery tile** — separate module; phone app remains source of truth.
20. **Xposed/LSPosed hooks** for system BT stack quirks on specific OEMs — power users only.

## Explicit non-goals (upstream LibrePods)

- Full stereo→spatial HRTF processing in-app (“beyond project scope”).
- Cloud assistant backends (violates zero-API-key / GrapheneOS stance).

## Hardening rules applied this pass

- No synthetic battery percentages.
- No fake STT transcripts.
- AAP does not report CONNECTED on failure.
- Popups require real data + overlay permission.
- Offline commands only run on non-empty final recognition results.
