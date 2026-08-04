# Device verification (Campaign 3)

1. Install debug APK from Actions.
2. Permissions: Bluetooth Scan/Connect, Microphone, Notifications.
3. Start gesture + AAP listener → FGS notification.
4. AirPods nearby → status fields update only when Continuity ads present.
5. Simulate stem / real stem → NativeGeminiRouter path (Intent or AICore).
6. Default assistant → VoiceInteraction session starts.
7. Noise chips + QS ANC tile — no crash.
8. Airplane mode → offline commands; OTA skipped if no-network pref.
9. GrapheneOS without AICore → graceful message, no crash.

Pass all before tagging v0.3.0.
