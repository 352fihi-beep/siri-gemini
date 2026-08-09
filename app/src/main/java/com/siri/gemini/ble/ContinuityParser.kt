package com.siri.gemini.ble

/**
 * Apple Continuity manufacturer-data parser (company ID 0x004C).
 * - 0x07 Proximity / AirPods status (public fields only)
 * - 0x08 Hey Siri event (presence + optional confidence)
 * Never invents battery or in-ear values.
 */
object ContinuityParser {

    const val APPLE_COMPANY_ID = 0x004C
    private const val TYPE_PROXIMITY = 0x07
    private const val TYPE_HEY_SIRI = 0x08

    data class AirPodsStatus(
        val leftBattery: Int? = null,
        val rightBattery: Int? = null,
        val caseBattery: Int? = null,
        val leftInEar: Boolean? = null,
        val rightInEar: Boolean? = null,
        val lidOpen: Boolean? = null,
        val raw: ByteArray
    ) {
        fun toContextString(): String {
            val l = leftBattery?.let { "L=${it}%" } ?: "L=?"
            val r = rightBattery?.let { "R=${it}%" } ?: "R=?"
            val c = caseBattery?.let { "Case=${it}%" } ?: "Case=?"
            val ear = "inEar[L=${leftInEar ?: "?"},R=${rightInEar ?: "?"}]"
            val lid = "lidOpen=${lidOpen ?: "?"}"
            return "AirPods $l $r $c $ear $lid"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is AirPodsStatus) return false
            return raw.contentEquals(other.raw)
        }
        override fun hashCode(): Int = raw.contentHashCode()
    }

    data class HeySiriEvent(
        val confidence: Int? = null,
        val snr: Int? = null,
        val raw: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is HeySiriEvent) return false
            return raw.contentEquals(other.raw)
        }
        override fun hashCode(): Int = raw.contentHashCode()
    }

    data class ParseResult(
        val status: AirPodsStatus? = null,
        val heySiri: HeySiriEvent? = null
    )

    fun parse(manufacturerData: ByteArray?): ParseResult? {
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
        var sawProximity = false
        var heySiri: HeySiriEvent? = null

        var offset = 0
        while (offset + 1 < data.size) {
            val type = data[offset].toInt() and 0xFF
            val len = data[offset + 1].toInt() and 0xFF
            offset += 2
            if (offset + len > data.size) break
            val value = data.copyOfRange(offset, offset + len)
            offset += len

            when (type) {
                TYPE_PROXIMITY -> {
                    sawProximity = true
                    if (value.size >= 4) {
                        val batByte = value.getOrNull(3)?.toInt()?.and(0xFF)
                            ?: value.getOrNull(2)?.toInt()?.and(0xFF)
                        if (batByte != null) {
                            leftBat = nibblePercent((batByte shr 4) and 0x0F) ?: leftBat
                            rightBat = nibblePercent(batByte and 0x0F) ?: rightBat
                        }
                    }
                    if (value.size >= 5) {
                        val flags = value[4].toInt() and 0xFF
                        leftInEar = (flags and 0x02) != 0
                        rightInEar = (flags and 0x08) != 0
                        lid = (flags and 0x01) != 0
                    }
                    if (value.size >= 6) {
                        caseBat = nibblePercent(value[5].toInt() and 0x0F) ?: caseBat
                    }
                }
                TYPE_HEY_SIRI -> {
                    if (len >= 1) {
                        val conf = value.getOrNull(0)?.toInt()?.and(0xFF)
                        val snr = value.getOrNull(1)?.toInt()?.and(0xFF)
                        heySiri = HeySiriEvent(
                            confidence = conf,
                            snr = snr,
                            raw = manufacturerData.copyOf()
                        )
                    }
                }
            }
        }

        val status = if (sawProximity &&
            (leftBat != null || rightBat != null || leftInEar != null || lid != null)
        ) {
            AirPodsStatus(
                leftBattery = leftBat,
                rightBattery = rightBat,
                caseBattery = caseBat,
                leftInEar = leftInEar,
                rightInEar = rightInEar,
                lidOpen = lid,
                raw = manufacturerData.copyOf()
            )
        } else null

        if (status == null && heySiri == null) return null
        return ParseResult(status = status, heySiri = heySiri)
    }

    fun parseStatus(manufacturerData: ByteArray?): AirPodsStatus? =
        parse(manufacturerData)?.status

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

    private fun nibblePercent(nibble: Int): Int? {
        if (nibble in 0..10) return nibble * 10
        return null
    }
}
