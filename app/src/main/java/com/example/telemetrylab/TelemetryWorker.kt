package com.example.telemetrylab

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

object TelemetryWorker {

    suspend fun runComputation(load: Int) {
        withContext(Dispatchers.Default) {
            val size = 256
            val matrix = Array(size) { FloatArray(size) { Random.nextFloat() } }
            val kernel = arrayOf(
                floatArrayOf(0f, 1f, 0f),
                floatArrayOf(1f, -4f, 1f),
                floatArrayOf(0f, 1f, 0f)
            )

            repeat(load) {
                val result = Array(size) { FloatArray(size) }
                for (i in 1 until size - 1) {
                    for (j in 1 until size - 1) {
                        var sum = 0f
                        for (ki in -1..1) {
                            for (kj in -1..1) {
                                sum += matrix[i + ki][j + kj] * kernel[ki + 1][kj + 1]
                            }
                        }
                        result[i][j] = sum
                    }
                }
            }
        }
    }
}
