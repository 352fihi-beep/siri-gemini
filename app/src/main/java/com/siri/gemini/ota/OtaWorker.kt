package com.siri.gemini.ota

import android.content.Context
import android.util.Log
import androidx.work.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * Lightweight GitHub Releases OTA checker.
 * Runs in the background via WorkManager (doze-aware).
 * Does not auto-install — only notifies / downloads when a newer tag exists.
 */
class OtaWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        return try {
            val latest = fetchLatestRelease()
            if (latest != null) {
                val current = applicationContext.packageManager
                    .getPackageInfo(applicationContext.packageName, 0).versionName ?: "0"
                if (isNewer(latest.tag, current)) {
                    Log.i(TAG, "Update available: ${latest.tag} (current $current)")
                    // Future: show notification + optional DownloadManager
                    setProgress(workDataOf("update_available" to latest.tag))
                }
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
        val assets = json.optJSONArray("assets") ?: return ReleaseInfo(tag, null)
        var apkUrl: String? = null
        for (i in 0 until assets.length()) {
            val a = assets.getJSONObject(i)
            if (a.optString("name").endsWith(".apk")) {
                apkUrl = a.optString("browser_download_url")
                break
            }
        }
        return ReleaseInfo(tag, apkUrl)
    }

    private fun isNewer(remote: String, local: String): Boolean {
        // Simple dotted numeric compare; good enough for 0.1.0 style
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

    data class ReleaseInfo(val tag: String, val apkUrl: String?)

    companion object {
        private const val TAG = "OtaWorker"
        private const val UNIQUE = "siri_gemini_ota"

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
