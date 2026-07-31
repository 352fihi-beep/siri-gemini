package com.siri.gemini.qol

import android.content.Context
import android.telecom.TelecomManager
import android.util.Log
import android.widget.Toast

/**
 * QoL #7 — head gesture actions (nod = accept, shake = dismiss).
 * Wired once AAP head-gesture packets are live.
 */
class HeadGestureHandler(private val context: Context) {

    fun onNod() {
        Log.i(TAG, "Nod → accept call / confirm")
        try {
            val tm = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            if (context.checkSelfPermission(android.Manifest.permission.ANSWER_PHONE_CALLS)
                == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                tm.acceptRingingCall()
            }
        } catch (e: Exception) {
            Log.w(TAG, "accept call failed", e)
        }
    }

    fun onShake() {
        Log.i(TAG, "Shake → dismiss")
        try {
            val tm = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            if (context.checkSelfPermission(android.Manifest.permission.ANSWER_PHONE_CALLS)
                == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                // endCall requires different permission on newer APIs; best-effort
                @Suppress("DEPRECATION")
                tm.endCall()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Dismiss", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val TAG = "HeadGesture"
    }
}
