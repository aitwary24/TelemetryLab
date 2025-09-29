package com.example.telemetrylab

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.telemetrylab.ui.theme.TelemetryLabTheme
import com.example.telemetrylab.viewmodel.TelemetryViewModel

class MainActivity : ComponentActivity() {

    // Launcher for POST_NOTIFICATIONS (Android 13+)
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) checkAndStartService()
            else Toast.makeText(this,"Notifications are required.",Toast.LENGTH_LONG).show()
        }

    // Launcher for FOREGROUND_SERVICE_DATA_SYNC (Android 14+)
    private val requestFgsPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) startTelemetryService()
            else Toast.makeText(this,"Foreground service permission denied.",Toast.LENGTH_LONG).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TelemetryLabTheme {
                val vm: TelemetryViewModel = viewModel()
                TelemetryScreen(
                    vm,
                    onStartService = { checkAndStartService() },
                    onStopService = { stopTelemetryService() }
                )
            }
        }
    }

    private fun checkAndStartService() {
        // Android 13+ → Ask POST_NOTIFICATIONS
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }

        // Android 14+ → Ask FOREGROUND_SERVICE_DATA_SYNC
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE_DATA_SYNC)
            != PackageManager.PERMISSION_GRANTED) {
            requestFgsPermission.launch(Manifest.permission.FOREGROUND_SERVICE_DATA_SYNC)
            return
        }

        // Start service if all permissions granted
        startTelemetryService()
    }

    private fun startTelemetryService() {
        val intent = Intent(this, TelemetryService::class.java)
        ContextCompat.startForegroundService(this, intent)
        Toast.makeText(this,"Telemetry Service Started",Toast.LENGTH_SHORT).show()
    }

    private fun stopTelemetryService() {
        val intent = Intent(this, TelemetryService::class.java)
        stopService(intent)
        Toast.makeText(this,"Telemetry Service Stopped",Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun TelemetryScreen(
    vm: TelemetryViewModel,
    onStartService: () -> Unit,
    onStopService: () -> Unit
) {
    val state by vm.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (state.batterySaver) {
            Text("Power-save mode active", color = MaterialTheme.colorScheme.primary)
        }

        Row {
            Button(onClick = {
                if (!state.running) onStartService()
                else onStopService()
                vm.toggle()
            }) {
                Text(if (state.running) "Stop" else "Start")
            }
            Spacer(Modifier.width(16.dp))
            Text("Load: ${state.load}")
        }

        Slider(
            value = state.load.toFloat(),
            onValueChange = { vm.setLoad(it.toInt()) },
            valueRange = 1f..5f
        )

        Spacer(Modifier.height(8.dp))
        Text("Latency: ${state.latencyMs} ms (avg ${state.avgLatencyMs} ms)")
        Text("Jank: ${state.jankPercent}% (${state.jankCount} frames)")

        Spacer(Modifier.height(8.dp))
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(state.counter.size) { i ->
                Text("Frame ${state.counter[i]}")
            }
        }
    }
}
