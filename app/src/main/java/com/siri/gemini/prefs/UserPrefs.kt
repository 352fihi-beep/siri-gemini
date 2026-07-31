package com.siri.gemini.prefs

import android.content.Context

class UserPrefs(context: Context) {
    private val sp = context.getSharedPreferences("siri_gemini_prefs", Context.MODE_PRIVATE)

    var onboardingDone: Boolean
        get() = sp.getBoolean("onboarding_done", false)
        set(v) = sp.edit().putBoolean("onboarding_done", v).apply()

    var stemSingle: String
        get() = sp.getString("stem_single", "assistant") ?: "assistant"
        set(v) = sp.edit().putString("stem_single", v).apply()
    var stemDouble: String
        get() = sp.getString("stem_double", "play_pause") ?: "play_pause"
        set(v) = sp.edit().putString("stem_double", v).apply()
    var stemLong: String
        get() = sp.getString("stem_long", "anc_cycle") ?: "anc_cycle"
        set(v) = sp.edit().putString("stem_long", v).apply()

    var conversationalAwareness: Boolean
        get() = sp.getBoolean("conv_aware", true)
        set(v) = sp.edit().putBoolean("conv_aware", v).apply()
    var earAutoPause: Boolean
        get() = sp.getBoolean("ear_auto_pause", true)
        set(v) = sp.edit().putBoolean("ear_auto_pause", v).apply()
    var airpodsName: String
        get() = sp.getString("airpods_name", "AirPods") ?: "AirPods"
        set(v) = sp.edit().putString("airpods_name", v).apply()

    var eqBass: Int
        get() = sp.getInt("eq_bass", 0)
        set(v) = sp.edit().putInt("eq_bass", v).apply()
    var eqTreble: Int
        get() = sp.getInt("eq_treble", 0)
        set(v) = sp.edit().putInt("eq_treble", v).apply()

    var haAmplification: Int
        get() = sp.getInt("ha_amp", 50)
        set(v) = sp.edit().putInt("ha_amp", v).apply()
    var haBalance: Int
        get() = sp.getInt("ha_balance", 50)
        set(v) = sp.edit().putInt("ha_balance", v).apply()
    var haTone: Int
        get() = sp.getInt("ha_tone", 50)
        set(v) = sp.edit().putInt("ha_tone", v).apply()
    var haConversationBoost: Boolean
        get() = sp.getBoolean("ha_conv_boost", false)
        set(v) = sp.edit().putBoolean("ha_conv_boost", v).apply()
    var haLoudReduction: Boolean
        get() = sp.getBoolean("ha_loud_red", true)
        set(v) = sp.edit().putBoolean("ha_loud_red", v).apply()

    // Campaign 2
    var caseChargeSound: Boolean
        get() = sp.getBoolean("case_charge_sound", true)
        set(v) = sp.edit().putBoolean("case_charge_sound", v).apply()
    var noNetworkMode: Boolean
        get() = sp.getBoolean("no_network", false)
        set(v) = sp.edit().putBoolean("no_network", v).apply()
    var leaveBehindEnabled: Boolean
        get() = sp.getBoolean("leave_behind", false)
        set(v) = sp.edit().putBoolean("leave_behind", v).apply()
    var budBalance: Int  // 0 = full left, 100 = full right, 50 = center
        get() = sp.getInt("bud_balance", 50)
        set(v) = sp.edit().putInt("bud_balance", v.coerceIn(0, 100)).apply()
    var adaptiveAudio: Boolean
        get() = sp.getBoolean("adaptive_audio", false)
        set(v) = sp.edit().putBoolean("adaptive_audio", v).apply()
    var lastFirmware: String
        get() = sp.getString("fw", "") ?: ""
        set(v) = sp.edit().putString("fw", v).apply()

    companion object {
        val STEM_ACTIONS = listOf(
            "assistant", "play_pause", "next", "prev", "anc_cycle",
            "volume_up", "volume_down", "none"
        )
    }
}
