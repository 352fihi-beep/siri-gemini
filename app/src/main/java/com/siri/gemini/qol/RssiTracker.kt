package com.siri.gemini.qol

import java.util.concurrent.ConcurrentLinkedDeque

/**
 * Campaign 2 #9 — real RSSI samples only (no synthetic graph data).
 */
object RssiTracker {
    private const val MAX = 30
    private val samples = ConcurrentLinkedDeque<Int>()

    fun add(rssi: Int) {
        if (rssi == 0 || rssi == Integer.MIN_VALUE) return
        samples.addLast(rssi)
        while (samples.size > MAX) samples.pollFirst()
    }

    fun latest(): Int? = samples.lastOrNull()

    fun average(): Int? {
        val list = samples.toList()
        if (list.isEmpty()) return null
        return list.sum() / list.size
    }

    fun snapshot(): List<Int> = samples.toList()

    fun qualityLabel(): String {
        val avg = average() ?: return "Signal: —"
        return when {
            avg >= -60 -> "Signal: excellent ($avg dBm)"
            avg >= -75 -> "Signal: good ($avg dBm)"
            avg >= -85 -> "Signal: fair ($avg dBm)"
            else -> "Signal: weak ($avg dBm)"
        }
    }
}
