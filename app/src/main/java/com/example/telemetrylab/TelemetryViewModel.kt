package com.example.telemetrylab.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.telemetrylab.model.TelemetryFrame
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

data class TelemetryState(
    val running: Boolean = false,
    val load: Int = 1,
    val latencyMs: Long = 0,
    val avgLatencyMs: Long = 0,
    val jankPercent: Float = 0f,
    val jankCount: Int = 0,
    val counter: List<Int> = emptyList(),
    val batterySaver: Boolean = false
)

class TelemetryViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(TelemetryState())
    val uiState: StateFlow<TelemetryState> = _uiState.asStateFlow()

    companion object {
        val channel = Channel<TelemetryFrame>(Channel.UNLIMITED)
        var load: Int = 1
    }

    //  Track last 30s jank
    private val jankWindow = mutableListOf<Long>()
    private val maxFrames = 600 // 20Hz * 30s

    init {
        viewModelScope.launch(Dispatchers.Default) {
            channel.consumeAsFlow().collect { frame ->
                val current = _uiState.value
                val newList = (current.counter + frame.id).takeLast(50)

                // Track jank for last 30s
                jankWindow.add(frame.latency)
                if (jankWindow.size > maxFrames) jankWindow.removeAt(0)

                val avgLatency = if (current.avgLatencyMs == 0L) frame.latency
                else (current.avgLatencyMs + frame.latency) / 2

                val jankCount = jankWindow.count { it > 50 }
                val jankPercent = if (jankWindow.isNotEmpty())
                    (jankCount.toFloat() / jankWindow.size.toFloat()) * 100f else 0f

                _uiState.value = current.copy(
                    latencyMs = frame.latency,
                    avgLatencyMs = avgLatency,
                    counter = newList,
                    jankCount = jankCount,
                    jankPercent = jankPercent,
                    batterySaver = frame.batterySaver
                )
            }
        }
    }

    fun toggle() {
        _uiState.update { it.copy(running = !it.running) }
    }

    fun setLoad(value: Int) {
        _uiState.update { it.copy(load = value) }
        load = value
    }
}
