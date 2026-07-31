package com.siri.gemini.qol

import android.util.Log
import com.siri.gemini.prefs.UserPrefs

/**
 * Campaign 2 #1 / experimental UARP awareness.
 * Stores and displays firmware strings only when received from AAP — never invented.
 */
object FirmwareInfo {
    private const val TAG = "FirmwareInfo"

    fun onFirmwarePacket(prefs: UserPrefs, payload: ByteArray) {
        if (payload.isEmpty()) return
        val text = try {
            // Best-effort ASCII; real format from LibrePods when integrated
            val s = payload.toString(Charsets.US_ASCII).trim { it < ' ' || it > '~' }
            s.takeIf { it.length in 3..32 }
        } catch (e: Exception) {
            Log.w(TAG, "fw parse", e)
            null
        } ?: return
        prefs.lastFirmware = text
        Log.i(TAG, "Firmware reported: $text")
    }

    fun display(prefs: UserPrefs): String {
        val fw = prefs.lastFirmware
        return if (fw.isBlank()) "Firmware: unknown (awaiting AAP)" else "Firmware: $fw"
    }
}
