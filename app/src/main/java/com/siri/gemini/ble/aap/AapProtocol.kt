package com.siri.gemini.ble.aap

/**
 * Apple AirPods Protocol (AAP) / L2CAP layer scaffold.
 *
 * Modeled on the reverse-engineering work in LibrePods
 * (https://github.com/librepods-org/librepods).
 *
 * Real stem force-sensor, ANC, and head-gesture events travel over
 * a proprietary L2CAP channel after the AirPods are convinced they
 * are talking to an Apple device. This file defines the opcode surface,
 * packet shapes, and event types so the rest of the app can be wired
 * against a stable API while the full RE implementation is completed.
 *
 * Implementation status:
 * - Packet formats & opcodes: scaffolded from public RE notes
 * - Actual L2CAP socket + handshake: requires native Bluetooth access
 *   and the full packet sequences from LibrePods source (airpods_packets.h etc.)
 * - On stock AOSP/GrapheneOS some L2CAP features still need the recent
 *   Bluetooth patches or a Magisk module (see LibrePods Android notes).
 */
object AapProtocol {

    // Common L2CAP PSM used by Apple accessories (publicly observed)
    const val PSM_AAP = 0x1001 // placeholder — confirm against current RE

    // High-level opcodes (names aligned with LibrePods terminology)
    object Opcode {
        const val HANDSHAKE = 0x00
        const val SET_NOISE_CONTROL = 0x09
        const val REQUEST_BATTERY = 0x04
        const val BATTERY_STATUS = 0x04
        const val EAR_DETECTION = 0x06
        const val STEM_PRESS = 0x0B          // force-sensor / stem events
        const val HEAD_GESTURE = 0x0C
        const val CONVERSATIONAL_AWARENESS = 0x0D
        const val ANC_LEVEL = 0x0E
        const val CUSTOM_EQ = 0x12
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
        SINGLE_PRESS,
        DOUBLE_PRESS,
        TRIPLE_PRESS,
        LONG_PRESS,
        SWIPE_UP,
        SWIPE_DOWN
    }

    data class BatteryInfo(
        val left: Int?,          // 0-100
        val right: Int?,
        val case: Int?,
        val leftCharging: Boolean = false,
        val rightCharging: Boolean = false,
        val caseCharging: Boolean = false
    )

    data class EarDetection(
        val leftInEar: Boolean,
        val rightInEar: Boolean
    )

    sealed class AapEvent {
        data class Stem(val action: StemAction, val bud: String = "either") : AapEvent()
        data class Battery(val info: BatteryInfo) : AapEvent()
        data class Ears(val detection: EarDetection) : AapEvent()
        data class Noise(val mode: NoiseMode) : AapEvent()
        data class HeadGesture(val nod: Boolean) : AapEvent() // true = nod, false = shake
        data class ConversationalAwareness(val active: Boolean) : AapEvent()
        data class Raw(val opcode: Int, val payload: ByteArray) : AapEvent()
    }

    /** Build a minimal command packet (opcode + payload). Real packets need checksums / headers from RE. */
    fun buildCommand(opcode: Int, payload: ByteArray = byteArrayOf()): ByteArray {
        // Placeholder framing — replace with exact LibrePods format
        return byteArrayOf(opcode.toByte()) + payload
    }

    fun buildSetNoiseMode(mode: NoiseMode): ByteArray =
        buildCommand(Opcode.SET_NOISE_CONTROL, byteArrayOf(mode.code.toByte()))

    fun buildRequestBattery(): ByteArray =
        buildCommand(Opcode.REQUEST_BATTERY)
}
