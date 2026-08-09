package com.siri.gemini.ai

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.siri.gemini.ble.ContinuityParser
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Native Gemini only — no developer API keys.
 * Priority: AICore/Nano when present → system Gemini app Intent → clipboard + web.
 */
object NativeGeminiRouter {

    enum class Mode { ON_DEVICE, SYSTEM_INTENT, CLIPBOARD_FALLBACK, UNAVAILABLE }

    data class RouteResult(
        val mode: Mode,
        val message: String,
        val launchedExternal: Boolean = false
    )

    fun isOnDeviceAvailable(context: Context): Boolean =
        AiCoreBridge.isAvailable(context)

    fun route(
        context: Context,
        utterance: String,
        status: ContinuityParser.AirPodsStatus? = null
    ): RouteResult {
        val prompt = buildPrompt(utterance, status)

        if (isOnDeviceAvailable(context)) {
            var resultMsg = "On-device path selected"
            val ok = java.util.concurrent.atomic.AtomicBoolean(false)
            val latch = CountDownLatch(1)
            AiCoreBridge.generate(
                context,
                prompt,
                onResult = {
                    resultMsg = it
                    ok.set(true)
                    latch.countDown()
                },
                onError = {
                    resultMsg = it
                    latch.countDown()
                }
            )
            // Wait up to 5 seconds for on-device generation
            latch.await(5, TimeUnit.SECONDS)
            return if (ok.get()) {
                RouteResult(Mode.ON_DEVICE, resultMsg)
            } else {
                launchSystemGemini(context, prompt)
            }
        }
        return launchSystemGemini(context, prompt)
    }

    private fun launchSystemGemini(context: Context, prompt: String): RouteResult {
        val packages = listOf(
            "com.google.android.apps.gemini",
            "com.google.android.googlequicksearchbox"
        )
        for (pkg in packages) {
            try {
                val launch = context.packageManager.getLaunchIntentForPackage(pkg)
                if (launch != null) {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        setPackage(pkg)
                        putExtra(Intent.EXTRA_TEXT, prompt)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(send)
                    return RouteResult(
                        Mode.SYSTEM_INTENT,
                        "Opened system Gemini with AirPods context",
                        launchedExternal = true
                    )
                }
            } catch (e: Exception) {
                Log.d(TAG, "Launch $pkg failed: ${e.message}")
            }
        }
        try {
            copyToClipboard(context, prompt)
            val web = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://gemini.google.com/app")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(web)
            return RouteResult(
                Mode.CLIPBOARD_FALLBACK,
                "Gemini app not found. Prompt copied — paste into Gemini.",
                launchedExternal = true
            )
        } catch (_: ActivityNotFoundException) {
            copyToClipboard(context, prompt)
            return RouteResult(
                Mode.CLIPBOARD_FALLBACK,
                "No browser/Gemini. Prompt copied to clipboard."
            )
        }
    }

    private fun copyToClipboard(context: Context, text: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("Siri Gemini → Gemini", text))
        // Clear clipboard after 10 seconds for privacy
        Handler(Looper.getMainLooper()).postDelayed({
            try { cm.setPrimaryClip(ClipData.newPlainText("", "")) } catch (_: Exception) {}
        }, 10_000)
    }

    fun buildPrompt(utterance: String, status: ContinuityParser.AirPodsStatus?): String {
        val hw = status?.toContextString() ?: "AirPods status unavailable"
        return """
            You are assisting via Siri Gemini on Android (privacy-first, GrapheneOS-friendly).
            Live AirPods context (public Continuity only):
            $hw

            User said: $utterance

            Answer using the hardware context when relevant. Do not invent battery or in-ear state.
        """.trimIndent()
    }

    private const val TAG = "NativeGeminiRouter"
}
