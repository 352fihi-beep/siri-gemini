package com.siri.gemini.ble

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Lightweight in-process event bus for stem-press / AirPods events.
 * Keeps the BLE service decoupled from the VoiceInteractionService.
 */
object GestureEventBus {

    sealed class Event {
        data class StemPress(val source: String = "airpods_h2") : Event()
        data class AirPodsNearby(val status: ContinuityParser.AirPodsStatus) : Event()
        object AirPodsLost : Event()
    }

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 8)
    val events: SharedFlow<Event> = _events.asSharedFlow()

    suspend fun emit(event: Event) {
        _events.emit(event)
    }

    fun tryEmit(event: Event): Boolean = _events.tryEmit(event)
}
