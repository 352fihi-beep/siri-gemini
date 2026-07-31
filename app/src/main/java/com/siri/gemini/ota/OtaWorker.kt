package com.siri.gemini.ota

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.siri.gemini.R
import com.siri.gemini.SiriGeminiApp
import com.siri.gemini.ui.MainActivity
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * GitHub Releases OTA checker + download notification.
 */
class OtaWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        return try {
            val latest = fetchLatestRelease() ?: return Result.success()
            val current = try {
                applicationContext.packageManager
                    .getPackageInfo(applicationContext.packageName, 0).versionName ?: "0"
            } catch (_: Exception) { "0" }

            if (isNewer(latest.tag, current)) {
                Log.i(TAG, "Update available: ${latest.tag} (current $current)")
                showUpdateNotification(latest)
                setProgress(workDataOf("update_available" to latest.tag))
            }
            Result.success()
        } catch (e: Exception) {
            Log.w(TAG, "OTA check failed", e)
            Result.retry()
        }
    }

    private fun fetchLatestRelease(): ReleaseInfo? {
        val url = URL("https://api.github.com/repos/352fihi-beep/siri-gemini/releases/latest")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 8_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "siri-gemini-ota")
        }
        if (conn.responseCode != 200) return null
        val body = conn.inputStream.bufferedReader().readText()
        val json = JSONObject(body)
        val tag = json.optString("tag_name").removePrefix("v")
        val bodyText = json.optString("body")
        val assets = json.optJSONArray("assets") ?: return ReleaseInfo(tag, null, bodyText)
        var apkUrl: String? = null
        for (i in 0 until assets.length()) {
            val a = assets.getJSONObject(i)
            if (a.optString("name").endsWith(".apk", ignoreCase = true)) {
                apkUrl = a.optString("browser_download_url")
                break
            }
        }
        return ReleaseInfo(tag, apkUrl, bodyText)
    }

    private fun showUpdateNotification(info: ReleaseInfo) {
        val nm = applicationContext.getSystemService(NotificationManager::class.java)

        val openIntent = if (info.apkUrl != null) {
            Intent(Intent.ACTION_VIEW, Uri.parse(info.apkUrl))
        } else {
            Intent(applicationContext, MainActivity::class.java)
        }
        val pi = PendingIntent.getActivity(
            applicationContext, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(applicationContext, SiriGeminiApp.CHANNEL_OTA)
            .setContentTitle("Siri Gemini update available")
            .setContentText("Version ${info.tag} is ready")
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "Version ${info.tag} is ready.\n${info.notes.take(200)}"
            ))
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        nm.notify(OTA_NOTIFICATION_ID, notif)
    }

    private fun isNewer(remote: String, local: String): Boolean {
        fun parse(v: String) = v.split(".").map { it.toIntOrNull() ?: 0 }
        val r = parse(remote)
        val l = parse(local)
        for (i in 0 until maxOf(r.size, l.size)) {
            val rv = r.getOrElse(i) { 0 }
            val lv = l.getOrElse(i) { 0 }
            if (rv != lv) return rv > lv
        }
        return false
    }

    data class ReleaseInfo(val tag: String, val apkUrl: String?, val notes: String = "")

    companion object {
        private const val TAG = "OtaWorker"
        private const val UNIQUE = "siri_gemini_ota"
        private const val OTA_NOTIFICATION_ID = 99

        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<OtaWorker>(12, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
