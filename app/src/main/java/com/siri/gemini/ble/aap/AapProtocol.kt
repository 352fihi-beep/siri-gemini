package com.siri.gemini.ble.aap

/**
 * Apple AirPods Protocol (AAP) layer — hardened scaffold.
 *
 * Grounded in public RE (LibrePods, Continuity papers).
 * Exact multi-byte headers, checksums, and stem payload maps must still be
 * aligned with the current LibrePods `airpods_packets` definitions before
 * production use. This file gives the rest of the app a stable contract.
 */
object AapProtocol {

    const val PSM_AAP = 0x1001

    object Opcode {
        const val HANDSHAKE = 0x00
        const val SET_NOISE_CONTROL = 0x09
        const val BATTERY_STATUS = 0x04
        const val EAR_DETECTION = 0x06
        const val STEM_PRESS = 0x0B
        const val HEAD_GESTURE = 0x0C
        const val CONVERSATIONAL_AWARENESS = 0x0D
        const val CUSTOM_EQ = 0x12
        const val NOTIFICATION = 0x0F
    }

    enum class NoiseMode(val code: Int) {
        OFF(0x01),
        NOISE_CANCELLATION(0x02),
        TRANSPARENCY(0x03),
        ADAPTIVE(0x04);
        companion object {
            fun from(code: Int) = entries.find { it.code == code } ?: OFF
        }
    }

    enum class StemAction {
        SINGLE_PRESS, DOUBLE_PRESS, TRIPLE_PRESS, LONG_PRESS, SWIPE_UP, SWIPE_DOWN
    }

    data class BatteryInfo(
        val left: Int? = null,
        val right: Int? = null,
        val case: Int? = null,
        val leftCharging: Boolean = false,
        val rightCharging: Boolean = false,
        val caseCharging: Boolean = false
    )

    data class EarDetection(val leftInEar: Boolean, val rightInEar: Boolean)

    sealed class AapEvent {
        data class Stem(val action: StemAction, val bud: String = "either") : AapEvent()
        data class Battery(val info: BatteryInfo) : AapEvent()
        data class Ears(val detection: EarDetection) : AapEvent()
        data class Noise(val mode: NoiseMode) : AapEvent()
        data class HeadGesture(val nod: Boolean) : AapEvent()
        data class Raw(val opcode: Int, val payload: ByteArray) : AapEvent()
    }

    /** Minimal command frame. Replace with LibrePods exact header when integrating. */
    fun buildCommand(opcode: Int, payload: ByteArray = byteArrayOf()): ByteArray {
        val len = payload.size
        return byteArrayOf(
            0x00, 0x00,                 // reserved / version placeholder
            opcode.toByte(),
            (len and 0xFF).toByte()
        ) + payload
    }

    fun buildSetNoiseMode(mode: NoiseMode): ByteArray =
        buildCommand(Opcode.SET_NOISE_CONTROL, byteArrayOf(mode.code.toByte()))

    fun buildRequestBattery(): ByteArray =
        buildCommand(Opcode.BATTERY_STATUS)

    fun parseStemAction(payload: ByteArray): StemAction {
        val code = payload.getOrNull(0)?.toInt()?.and(0xFF) ?: 0
        return when (code) {
            1 -> StemAction.SINGLE_PRESS
            2 -> StemAction.DOUBLE_PRESS
            3 -> StemAction.TRIPLE_PRESS
            4 -> StemAction.LONG_PRESS
            5 -> StemAction.SWIPE_UP
            6 -> StemAction.SWIPE_DOWN
            else -> StemAction.SINGLE_PRESS
        }
    }

    fun parseBattery(payload: ByteArray): BatteryInfo {
        // Nibble-style encoding commonly seen in public RE; refine with LibrePods
        fun level(b: Byte?): Int? {
            val v = b?.toInt()?.and(0x0F) ?: return null
            return if (v in 0..10) v * 10 else null
        }
        fun charging(b: Byte?): Boolean =
            ((b?.toInt()?.and(0xF0) ?: 0) shr 4) and 0x01 != 0

        return BatteryInfo(
            left = level(payload.getOrNull(0)),
            right = level(payload.getOrNull(1)),
            case = level(payload.getOrNull(2)),
            leftCharging = charging(payload.getOrNull(0)),
            rightCharging = charging(payload.getOrNull(1)),
            caseCharging = charging(payload.getOrNull(2))
        )
    }
}
