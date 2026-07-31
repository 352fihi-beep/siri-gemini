package com.siri.gemini.ble

/**
 * Apple Continuity manufacturer-data parser (company ID 0x004C).
 * Returns null unless AirPods-like status fields are actually present.
 * No synthetic / template battery values.
 */
object ContinuityParser {

    const val APPLE_COMPANY_ID = 0x004C
    private const val TYPE_PROXIMITY = 0x07

    data class AirPodsStatus(
        val leftBattery: Int? = null,
        val rightBattery: Int? = null,
        val caseBattery: Int? = null,
        val leftInEar: Boolean? = null,
        val rightInEar: Boolean? = null,
        val lidOpen: Boolean? = null,
        val raw: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is AirPodsStatus) return false
            return raw.contentEquals(other.raw)
        }
        override fun hashCode(): Int = raw.contentHashCode()
    }

    fun parse(manufacturerData: ByteArray?): AirPodsStatus? {
        if (manufacturerData == null || manufacturerData.isEmpty()) return null

        val data = try {
            stripCompanyId(manufacturerData)
        } catch (_: Exception) {
            return null
        }
        if (data.isEmpty()) return null

        var leftBat: Int? = null
        var rightBat: Int? = null
        var caseBat: Int? = null
        var leftInEar: Boolean? = null
        var rightInEar: Boolean? = null
        var lid: Boolean? = null
        var sawAirPodsTlv = false

        var offset = 0
        while (offset + 1 < data.size) {
            val type = data[offset].toInt() and 0xFF
            val len = data[offset + 1].toInt() and 0xFF
            offset += 2
            if (len < 0 || offset + len > data.size) break
            val value = data.copyOfRange(offset, offset + len)
            offset += len

            if (type == TYPE_PROXIMITY || type == 0x07) {
                sawAirPodsTlv = true
                if (value.size >= 3) {
                    val statusByte = value[2].toInt() and 0xFF
                    leftBat = nibblePercent((statusByte shr 4) and 0x0F)
                    rightBat = nibblePercent(statusByte and 0x0F)
                }
                if (value.size >= 5) {
                    val flags = value[4].toInt() and 0xFF
                    leftInEar = (flags and 0x02) != 0
                    rightInEar = (flags and 0x08) != 0
                    lid = (flags and 0x01) != 0
                }
                if (value.size >= 6) {
                    caseBat = nibblePercent(value[5].toInt() and 0x0F)
                }
            }
        }

        if (!sawAirPodsTlv) return null
        // Require at least one concrete field — never invent numbers
        if (leftBat == null && rightBat == null && leftInEar == null && lid == null) return null

        return AirPodsStatus(
            leftBattery = leftBat,
            rightBattery = rightBat,
            caseBattery = caseBat,
            leftInEar = leftInEar,
            rightInEar = rightInEar,
            lidOpen = lid,
            raw = manufacturerData.copyOf()
        )
    }

    private fun stripCompanyId(raw: ByteArray): ByteArray {
        if (raw.size >= 2) {
            val lo = raw[0].toInt() and 0xFF
            val hi = raw[1].toInt() and 0xFF
            if (lo == (APPLE_COMPANY_ID and 0xFF) && hi == ((APPLE_COMPANY_ID shr 8) and 0xFF)) {
                return raw.copyOfRange(2, raw.size)
            }
        }
        return raw
    }

    /** AirPods often encode 0–10 as 0–100%; 15 = unknown — never fabricate. */
    private fun nibblePercent(nibble: Int): Int? {
        if (nibble in 0..10) return nibble * 10
        return null
    }
}
