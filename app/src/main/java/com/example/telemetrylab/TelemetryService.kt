package com.example.telemetrylab

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.telemetrylab.model.TelemetryFrame
import com.example.telemetrylab.viewmodel.TelemetryViewModel
import kotlinx.coroutines.*
import kotlin.system.measureTimeMillis

class TelemetryService : Service() {

    companion object {
        const val CHANNEL_ID = "telemetry_channel"
        const val NOTIFICATION_ID = 1
    }

    private var serviceJob: Job? = null
    private var frameId = 0

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startGeneratingFrames()
        return START_STICKY
    }

    private fun startGeneratingFrames() {
        serviceJob?.cancel()
        serviceJob = CoroutineScope(Dispatchers.Default).launch {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager

            while (isActive) {
                // Battery-aware check using PowerManager
                val batterySaver = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    powerManager.isPowerSaveMode
                } else false

                val intervalMs = if (batterySaver) 100 else 50
                val load = (TelemetryViewModel.load.coerceIn(1, 5) - if (batterySaver) 1 else 0).coerceAtLeast(1)

                // Measure CPU-heavy computation latency
                val latency = measureTimeMillis {
                    repeat(load) { TelemetryWorker.runComputation(1) }
                }

                // Send telemetry frame to ViewModel via channel
                TelemetryViewModel.channel.trySend(
                    TelemetryFrame(frameId++, latency, batterySaver)
                )

                delay(intervalMs.toLong())
            }
        }
    }

    private fun buildNotification(): Notification {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Telemetry Lab Running")
            .setContentText("Collecting frames...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)

        // FGS behavior for Android 14+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                val field = NotificationCompat.Builder::class.java.getDeclaredField("FOREGROUND_SERVICE_BEHAVIOR_TYPE_DATA_SYNC")
                field.isAccessible = true
                val value = field.getInt(builder)
                builder.foregroundServiceBehavior = value
            } catch (e: Exception) {
                // fallback: ignore
            }
        }

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Telemetry Lab",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceJob?.cancel()
        super.onDestroy()
    }
}
