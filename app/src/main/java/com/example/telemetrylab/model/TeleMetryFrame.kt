package com.example.telemetrylab.model

data class TelemetryFrame(
    val id: Int,
    val latency: Long,
    val batterySaver: Boolean = false
)
