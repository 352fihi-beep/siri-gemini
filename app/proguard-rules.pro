# siri-gemini — aggressive but safe R8 rules
-keepclassmembers class * extends android.service.voice.VoiceInteractionService {
    public <init>(...);
}
-keep class * extends android.service.voice.VoiceInteractionSession { *; }
-keep class * extends android.speech.RecognitionService { *; }

# BLE / Continuity parser
-keepclassmembers class com.siri.gemini.ble.** { *; }

# Keep Parcelables / data classes used across processes
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Compose
-dontwarn androidx.compose.**
