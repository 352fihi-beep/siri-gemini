package com.siri.gemini.ble.aap

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import com.siri.gemini.ble.GestureEventBus
import com.siri.gemini.widget.AirPodsWidgetProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException
import java.util.UUID

/**
 * AAP link. Reports real connection failures; does not pretend success.
 * Full L2CAP framing still requires LibrePods packet sequences.
 */
class AapConnection(
    private val device: BluetoothDevice,
    private val scope: CoroutineScope,
    private val appContext: Context
) {
    enum class State { DISCONNECTED, CONNECTING, CONNECTED, FAILED }

    private val _state = MutableStateFlow(State.DISCONNECTED)
    val state: StateFlow<State> = _state.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val _battery = MutableStateFlow<AapProtocol.BatteryInfo?>(null)
    val battery: StateFlow<AapProtocol.BatteryInfo?> = _battery.asStateFlow()

    private val _noiseMode = MutableStateFlow(AapProtocol.NoiseMode.OFF)
    val noiseMode: StateFlow<AapProtocol.NoiseMode> = _noiseMode.asStateFlow()

    private val _ears = MutableStateFlow(AapProtocol.EarDetection(false, false))
    val ears: StateFlow<AapProtocol.EarDetection> = _ears.asStateFlow()

    private var socket: BluetoothSocket? = null
    private var readJob: Job? = null

    private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    @Suppress("MissingPermission")
    fun connect() {
        if (_state.value == State.CONNECTING || _state.value == State.CONNECTED) return
        _state.value = State.CONNECTING
        _lastError.value = null

        scope.launch(Dispatchers.IO) {
            try {
                // Preferred production path when PSM + framing are complete:
                // socket = device.createInsecureL2capChannel(AapProtocol.PSM_AAP)
                val s = device.createInsecureRfcommSocketToServiceRecord(SPP_UUID)
                socket = s
                s.connect()
                if (!s.isConnected) {
                    fail("Socket connected flag false after connect()")
                    return@launch
                }
                _state.value = State.CONNECTED
                Log.i(TAG, "AAP socket open ${device.address}")
                startReadLoop()
                // Handshake writes only after real connect — failures logged, not hidden
                writeOrLog(AapProtocol.buildCommand(AapProtocol.Opcode.HANDSHAKE))
                writeOrLog(AapProtocol.buildRequestBattery())
            } catch (e: SecurityException) {
                fail("Bluetooth permission missing: ${e.message}")
            } catch (e: IOException) {
                fail("AAP connect IO: ${e.message ?: "I/O error"}")
            } catch (e: Exception) {
                fail("AAP connect: ${e.message ?: e.javaClass.simpleName}")
            }
        }
    }

    private fun fail(msg: String) {
        Log.w(TAG, msg)
        _lastError.value = msg
        _state.value = State.FAILED
        closeSocketOnly()
    }

    private fun startReadLoop() {
        readJob = scope.launch(Dispatchers.IO) {
            val input = try {
                socket?.inputStream
            } catch (e: Exception) {
                fail("No input stream: ${e.message}")
                return@launch
            } ?: run {
                fail("Input stream null")
                return@launch
            }

            val buf = ByteArray(512)
            while (isActive && _state.value == State.CONNECTED) {
                try {
                    val n = input.read(buf)
                    if (n <= 0) {
                        fail("Remote closed connection")
                        break
                    }
                    dispatch(buf.copyOf(n))
                } catch (e: IOException) {
                    if (isActive) fail("Read error: ${e.message}")
                    break
                }
            }
        }
    }

    private fun dispatch(data: ByteArray) {
        if (data.size < 1) return
        val opcode = if (data.size >= 3) data[2].toInt() and 0xFF else data[0].toInt() and 0xFF
        val payloadStart = if (data.size > 4) 4 else 1
        val payload = if (data.size > payloadStart) data.copyOfRange(payloadStart, data.size) else byteArrayOf()

        when (opcode) {
            AapProtocol.Opcode.STEM_PRESS -> {
                val action = AapProtocol.parseStemAction(payload)
                GestureEventBus.tryEmit(GestureEventBus.Event.StemPress(source = "aap:$action"))
            }
            AapProtocol.Opcode.BATTERY_STATUS -> {
                val info = AapProtocol.parseBattery(payload)
                // Only publish if at least one real level parsed
                if (info.left != null || info.right != null || info.case != null) {
                    _battery.value = info
                    AirPodsWidgetProvider.refreshAll(appContext, info, _noiseMode.value)
                }
            }
            AapProtocol.Opcode.EAR_DETECTION -> {
                val left = (payload.getOrNull(0)?.toInt()?.and(0x01) ?: 0) != 0
                val right = (payload.getOrNull(0)?.toInt()?.and(0x02) ?: 0) != 0
                _ears.value = AapProtocol.EarDetection(left, right)
            }
            AapProtocol.Opcode.SET_NOISE_CONTROL -> {
                val mode = AapProtocol.NoiseMode.from(payload.getOrNull(0)?.toInt()?.and(0xFF) ?: 0)
                _noiseMode.value = mode
                AirPodsWidgetProvider.refreshAll(appContext, _battery.value, mode)
            }
            else -> Log.d(TAG, "Unhandled opcode 0x${opcode.toString(16)} len=${data.size}")
        }
    }

    @Suppress("MissingPermission")
    fun setNoiseMode(mode: AapProtocol.NoiseMode) {
        if (_state.value != State.CONNECTED) {
            Log.w(TAG, "setNoiseMode ignored — not connected (${_state.value})")
            _lastError.value = "Not connected"
            // Still update local UI preference
            _noiseMode.value = mode
            AirPodsWidgetProvider.refreshAll(appContext, _battery.value, mode)
            return
        }
        writeOrLog(AapProtocol.buildSetNoiseMode(mode))
        _noiseMode.value = mode
        AirPodsWidgetProvider.refreshAll(appContext, _battery.value, mode)
    }

    private fun writeOrLog(data: ByteArray) {
        scope.launch(Dispatchers.IO) {
            try {
                val out = socket?.outputStream ?: throw IOException("No output stream")
                out.write(data)
                out.flush()
            } catch (e: Exception) {
                Log.w(TAG, "Write failed: ${e.message}")
                _lastError.value = "Write failed: ${e.message}"
            }
        }
    }

    private fun closeSocketOnly() {
        readJob?.cancel()
        readJob = null
        try { socket?.close() } catch (_: Exception) {}
        socket = null
    }

    fun close() {
        closeSocketOnly()
        _state.value = State.DISCONNECTED
    }

    companion object {
        private const val TAG = "AapConnection"
    }
}
