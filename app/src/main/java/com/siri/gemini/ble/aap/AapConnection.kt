package com.siri.gemini.ble.aap

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
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
 * L2CAP / RFCOMM link for full AAP features.
 * Emits stem events to GestureEventBus and refreshes the home-screen widget.
 */
class AapConnection(
    private val device: BluetoothDevice,
    private val scope: CoroutineScope,
    private val appContext: android.content.Context
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

    private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    @Suppress("MissingPermission")
    fun connect() {
        scope.launch(Dispatchers.IO) {
            try {
                // Production path: device.createInsecureL2capChannel(AapProtocol.PSM_AAP)
                socket = device.createInsecureRfcommSocketToServiceRecord(SPP_UUID)
                socket?.connect()
                _connected.value = true
                Log.i(TAG, "AAP connected ${device.address}")
                startReadLoop()
                write(AapProtocol.buildCommand(AapProtocol.Opcode.HANDSHAKE))
                write(AapProtocol.buildRequestBattery())
            } catch (e: IOException) {
                Log.w(TAG, "AAP connect failed (complete LibrePods framing for production)", e)
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
                    dispatch(buf.copyOf(n))
                } catch (e: IOException) {
                    Log.w(TAG, "AAP read error", e)
                    break
                }
            }
            _connected.value = false
        }
    }

    private fun dispatch(data: ByteArray) {
        if (data.size < 3) return
        // Frame: [0,0, opcode, len, ...payload] (scaffold)
        val opcode = data.getOrNull(2)?.toInt()?.and(0xFF) ?: data[0].toInt().and(0xFF)
        val payloadStart = if (data.size > 4) 4 else 1
        val payload = if (data.size > payloadStart) data.copyOfRange(payloadStart, data.size) else byteArrayOf()

        when (opcode) {
            AapProtocol.Opcode.STEM_PRESS -> {
                val action = AapProtocol.parseStemAction(payload)
                GestureEventBus.tryEmit(GestureEventBus.Event.StemPress(source = "aap_l2cap:$action"))
                Log.i(TAG, "STEM $action")
            }
            AapProtocol.Opcode.BATTERY_STATUS -> {
                val info = AapProtocol.parseBattery(payload)
                _battery.value = info
                AirPodsWidgetProvider.refreshAll(appContext, info, _noiseMode.value)
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
            else -> Log.d(TAG, "AAP opcode 0x${opcode.toString(16)}")
        }
    }

    @Suppress("MissingPermission")
    fun setNoiseMode(mode: AapProtocol.NoiseMode) {
        write(AapProtocol.buildSetNoiseMode(mode))
        _noiseMode.value = mode
        AirPodsWidgetProvider.refreshAll(appContext, _battery.value, mode)
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
