package com.siri.gemini.ai

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * QoL #20 — local encrypted command history (AES, key derived from device-only salt).
 * No cloud. Optional; disabled by default until user opts in.
 */
class CommandHistory(context: Context) {

    private val sp = context.getSharedPreferences("cmd_hist", Context.MODE_PRIVATE)
    private val key: SecretKeySpec

    init {
        val salt = sp.getString("salt", null) ?: run {
            val s = Base64.encodeToString(
                MessageDigest.getInstance("SHA-256")
                    .digest((context.packageName + System.currentTimeMillis()).toByteArray()),
                Base64.NO_WRAP
            ).take(16)
            sp.edit().putString("salt", s).apply()
            s
        }
        val raw = MessageDigest.getInstance("SHA-256").digest(salt.toByteArray()).copyOf(16)
        key = SecretKeySpec(raw, "AES")
    }

    var enabled: Boolean
        get() = sp.getBoolean("enabled", false)
        set(v) = sp.edit().putBoolean("enabled", v).apply()

    fun add(text: String) {
        if (!enabled || text.isBlank()) return
        val enc = encrypt(text)
        val list = sp.getStringSet("items", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        list.add("${System.currentTimeMillis()}|$enc")
        // Keep last 50
        val trimmed = list.sorted().takeLast(50).toSet()
        sp.edit().putStringSet("items", trimmed).apply()
    }

    fun list(): List<String> {
        if (!enabled) return emptyList()
        return sp.getStringSet("items", emptySet())
            ?.mapNotNull {
                val parts = it.split("|", limit = 2)
                if (parts.size == 2) decrypt(parts[1]) else null
            }?.reversed() ?: emptyList()
    }

    private fun encrypt(plain: String): String {
        val c = Cipher.getInstance("AES")
        c.init(Cipher.ENCRYPT_MODE, key)
        return Base64.encodeToString(c.doFinal(plain.toByteArray()), Base64.NO_WRAP)
    }

    private fun decrypt(enc: String): String {
        val c = Cipher.getInstance("AES")
        c.init(Cipher.DECRYPT_MODE, key)
        return String(c.doFinal(Base64.decode(enc, Base64.NO_WRAP)))
    }
}
