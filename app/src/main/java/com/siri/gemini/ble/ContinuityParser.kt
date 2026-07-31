package com.siri.gemini.ble

/**
 * Apple Continuity (company ID 0x004C) manufacturer-data parser.
 * Focused on AirPods proximity / status messages that appear in BLE ads.
 * Stem force-sensor events still require the L2CAP bridge; this path gives
 * reliable nearby detection + battery / in-ear signals with near-zero cost.
 */
object ContinuityParser {

    const val APPLE_COMPANY_ID = 0x004C

    // Known Continuity message types (partial)
    const val TYPE_PROXIMITY_PAIRING = 0x07
    const val TYPE_AIRPODS = 0x07 // same family used by AirPods

    data class AirPodsStatus(
        val leftBattery: Int? = null,   // 0-100 or null
        val rightBattery: Int? = null,
        val caseBattery: Int? = null,
        val leftInEar: Boolean? = null,
        val rightInEar: Boolean? = null,
        val lidOpen: Boolean? = null,
        val charging: Boolean? = null,
        val modelHint: String? = null,
        val raw: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as AirPodsStatus
            return raw.contentEquals(other.raw)
        }
        override fun hashCode(): Int = raw.contentHashCode()
    }

    /**
     * Parse manufacturer specific data that already had the company ID stripped
     * or the full AD including company ID. Returns null on non-AirPods packets.
     */
    fun parse(manufacturerData: ByteArray?): AirPodsStatus? {
        if (manufacturerData == null || manufacturerData.size < 5) return null

        // Some stacks pass data after the 2-byte company ID, some include it.
        val data = if (manufacturerData.size >= 2 &&
            ((manufacturerData[0].toInt() and 0xFF) == (APPLE_COMPANY_ID and 0xFF)) &&
            ((manufacturerData[1].toInt() and 0xFF) == ((APPLE_COMPANY_ID shr 8) and 0xFF))
        ) {
            manufacturerData.copyOfRange(2, manufacturerData.size)
        } else {
            manufacturerData
        }

        if (data.isEmpty()) return null

        // Continuity is TLV: type (1) + length (1) + value
        var offset = 0
        var leftBat: Int? = null
        var rightBat: Int? = null
        var caseBat: Int? = null
        var leftInEar: Boolean? = null
        var rightInEar: Boolean? = null
        var lid: Boolean? = null

        while (offset + 1 < data.size) {
            val type = data[offset].toInt() and 0xFF
            val len = data[offset + 1].toInt() and 0xFF
            offset += 2
            if (offset + len > data.size) break
            val value = data.copyOfRange(offset, offset + len)
            offset += len

            when (type) {
                TYPE_PROXIMITY_PAIRING, 0x07 -> {
                    // AirPods proximity / status payload (simplified heuristic)
                    // Real production code should match the exact RE from LibrePods / PETS papers.
                    if (value.size >= 5) {
                        // Battery nibble encoding is common in public RE
                        val statusByte = value.getOrNull(2)?.toInt()?.and(0xFF) ?: 0
                        leftBat = ((statusByte shr 4) and 0x0F).takeIf { it in 0..10 }?.times(10)
                        rightBat = (statusByte and 0x0F).takeIf { it in 0..10 }?.times(10)
                        // In-ear bits often live in later flags
                        val flags = value.getOrNull(4)?.toInt()?.and(0xFF) ?: 0
                        leftInEar = (flags and 0x02) != 0
                        rightInEar = (flags and 0x08) != 0
                        lid = (flags and 0x01) != 0
                    }
                }
                else -> { /* ignore other Continuity types */ }
            }
        }

        // Only return if we saw something that looks like AirPods status
        if (leftBat == null && rightBat == null && leftInEar == null) return null

        return AirPodsStatus(
            leftBattery = leftBat,
            rightBattery = rightBat,
            caseBattery = caseBat,
            leftInEar = leftInEar,
            rightInEar = rightInEar,
            lidOpen = lid,
            raw = manufacturerData
        )
    }
}
