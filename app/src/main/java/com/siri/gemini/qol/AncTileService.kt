package com.siri.gemini.qol

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.siri.gemini.ble.AirPodsGestureService
import com.siri.gemini.ble.aap.AapProtocol

/**
 * QoL #6 — Quick Settings tile to cycle noise modes.
 */
class AncTileService : TileService() {

    private var mode = AapProtocol.NoiseMode.OFF

    override fun onStartListening() {
        updateTile()
    }

    override fun onClick() {
        mode = when (mode) {
            AapProtocol.NoiseMode.OFF -> AapProtocol.NoiseMode.NOISE_CANCELLATION
            AapProtocol.NoiseMode.NOISE_CANCELLATION -> AapProtocol.NoiseMode.TRANSPARENCY
            AapProtocol.NoiseMode.TRANSPARENCY -> AapProtocol.NoiseMode.ADAPTIVE
            AapProtocol.NoiseMode.ADAPTIVE -> AapProtocol.NoiseMode.OFF
        }
        startService(Intent(this, AirPodsGestureService::class.java).apply {
            action = AirPodsGestureService.ACTION_SET_NOISE
            putExtra(AirPodsGestureService.EXTRA_NOISE_MODE, mode.code)
        })
        updateTile()
    }

    private fun updateTile() {
        qsTile?.apply {
            label = when (mode) {
                AapProtocol.NoiseMode.OFF -> "ANC Off"
                AapProtocol.NoiseMode.NOISE_CANCELLATION -> "ANC On"
                AapProtocol.NoiseMode.TRANSPARENCY -> "Transparency"
                AapProtocol.NoiseMode.ADAPTIVE -> "Adaptive"
            }
            state = if (mode == AapProtocol.NoiseMode.OFF) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE
            updateTile()
        }
    }
}
