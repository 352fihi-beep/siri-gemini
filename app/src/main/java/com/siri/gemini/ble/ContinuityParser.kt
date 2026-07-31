package com.siri.gemini.ble

/**
 * Lightweight Apple Continuity (0x004C) manufacturer data parser.
 * Focused on AirPods proximity / status messages.
 * Stem force-sensor events require the higher-level L2CAP bridge (see AirPodsGestureService).
 */
object ContinuityParser {

    const val APPLE_COMPANY_ID = 0x004C

    data class AirPodsStatus(
        val model: String? = null,
        val leftBattery: Int? = null,
        val rightBattery: Int? = null,
        val caseBattery: Int? = null,
        val leftInEar: Boolean? = null,
        val rightInEar: Boolean? = null,
        val lidOpen: Boolean? = null,
        val raw: ByteArray
    )

    fun parse(manufacturerData: ByteArray): AirPodsStatus? {
        if (manufacturerData.size < 4) return null
        // First two bytes after company ID are usually type + length for Continuity TLV
        // Real production parser will expand the known AirPods message types (0x07 proximity etc.)
        // This is the optimized skeleton — zero allocation hot path where possible.
        return AirPodsStatus(raw = manufacturerData)
    }
}
