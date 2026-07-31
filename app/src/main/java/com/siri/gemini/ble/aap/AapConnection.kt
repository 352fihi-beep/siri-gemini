package com.siri.gemini.ble.aap

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import com.siri.gemini.ble.GestureEventBus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException
import java.util.UUID

/**
 * Manages the L2CAP / RFCOMM-style link to AirPods for full AAP features.
 *
 * Current state: connection + read loop scaffold.
 * Full handshake + packet parsing must be completed against LibrePods source
 * (native L2CAP sockets are preferred; classic RFCOMM UUID is a fallback probe).
 */
class AapConnection(
    private val device: BluetoothDevice,
    private val scope: CoroutineScope
) {
    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private val _battery = MutableStateFlow<AapProtocol.BatteryInfo?>(null)
    val battery: StateFlow<AapProtocol.BatteryInfo?> = _battery.asStateFlow()

    private val _noiseMode = MutableStateFlow(AapProtocol.NoiseMode.OFF)
    val noiseMode: StateFlow<AapProtocol.NoiseMode> = _noiseMode.asStateFlow()

    private val _ears = MutableStateFlow(AapProtocol.EarDetection(false, false))
    val ears: StateFlow<AapProtocol.EarDetection> = _ears.asStateFlow()

    private var socket: BluetoothSocket? = null
    private var readJob: Job? = null

    // Classic SPP UUID used as a probe; real AAP uses L2CAP PSM
    private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    @Suppress("MissingPermission")
    fun connect() {
        scope.launch(Dispatchers.IO) {
            try {
                // Preferred path (when system allows): L2CAP
                // socket = device.createInsecureL2capChannel(AapProtocol.PSM_AAP)
                // Fallback probe for development:
                socket = device.createInsecureRfcommSocketToServiceRecord(SPP_UUID)
                socket?.connect()
                _connected.value = true
                Log.i(TAG, "AAP socket connected to ${device.address}")
                startReadLoop()
                // Send handshake + initial battery request (once framing is exact)
                // write(AapProtocol.buildCommand(AapProtocol.Opcode.HANDSHAKE))
                // write(AapProtocol.buildRequestBattery())
            } catch (e: IOException) {
                Log.w(TAG, "AAP connect failed (expected until full RE + permissions)", e)
                _connected.value = false
                close()
            }
        }
    }

    private fun startReadLoop() {
        readJob = scope.launch(Dispatchers.IO) {
            val input = socket?.inputStream ?: return@launch
            val buf = ByteArray(512)
            while (isActive && _connected.value) {
                try {
                    val n = input.read(buf)
                    if (n <= 0) break
                    handlePacket(buf.copyOf(n))
                } catch (e: IOException) {
                    Log.w(TAG, "AAP read error", e)
                    break
                }
            }
            _connected.value = false
        }
    }

    private fun handlePacket(data: ByteArray) {
        if (data.isEmpty()) return
        val opcode = data[0].toInt() and 0xFF
        val payload = if (data.size > 1) data.copyOfRange(1, data.size) else byteArrayOf()

        when (opcode) {
            AapProtocol.Opcode.STEM_PRESS -> {
                // Map payload bytes to StemAction once exact format is known
                val action = AapProtocol.StemAction.SINGLE_PRESS
                GestureEventBus.tryEmit(GestureEventBus.Event.StemPress(source = "aap_l2cap"))
                Log.i(TAG, "STEM event: $action")
            }
            AapProtocol.Opcode.BATTERY_STATUS -> {
                // Parse real battery nibbles from LibrePods format
                val info = AapProtocol.BatteryInfo(
                    left = payload.getOrNull(0)?.toInt()?.and(0xFF),
                    right = payload.getOrNull(1)?.toInt()?.and(0xFF),
                    case = payload.getOrNull(2)?.toInt()?.and(0xFF)
                )
                _battery.value = info
            }
            AapProtocol.Opcode.EAR_DETECTION -> {
                val left = (payload.getOrNull(0)?.toInt()?.and(0x01) ?: 0) != 0
                val right = (payload.getOrNull(0)?.toInt()?.and(0x02) ?: 0) != 0
                _ears.value = AapProtocol.EarDetection(left, right)
            }
            AapProtocol.Opcode.SET_NOISE_CONTROL -> {
                val mode = AapProtocol.NoiseMode.from(payload.getOrNull(0)?.toInt()?.and(0xFF) ?: 0)
                _noiseMode.value = mode
            }
            else -> Log.d(TAG, "Unhandled AAP opcode 0x${opcode.toString(16)}")
        }
    }

    @Suppress("MissingPermission")
    fun setNoiseMode(mode: AapProtocol.NoiseMode) {
        write(AapProtocol.buildSetNoiseMode(mode))
        _noiseMode.value = mode
    }

    private fun write(data: ByteArray) {
        scope.launch(Dispatchers.IO) {
            try {
                socket?.outputStream?.write(data)
                socket?.outputStream?.flush()
            } catch (e: IOException) {
                Log.w(TAG, "AAP write failed", e)
            }
        }
    }

    fun close() {
        readJob?.cancel()
        try { socket?.close() } catch (_: Exception) {}
        socket = null
        _connected.value = false
    }

    companion object {
        private const val TAG = "AapConnection"
    }
}
