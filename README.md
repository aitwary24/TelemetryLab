# Telemetry Lab

A smooth, battery-aware, compute-intensive Android app built with **Jetpack Compose**, **Foreground Service**, and **JankStats**.  
The app continuously generates telemetry frames, performs heavy CPU computations off the main thread, and provides real-time performance insights.

---

## Features

- **Start / Stop toggle** to control background telemetry generation.
- **Slider (1–5)** to adjust compute load dynamically.
- **Real-time dashboard** showing:
    - Current frame latency (ms)
    - Moving average latency
    - Jank % over the last 30s
    - Jank frame count
- **Continuously scrolling list / animated counter** to visualize UI activity.
- **Battery saver mode support**
    - Frame interval reduced from 20Hz (50ms) → 10Hz (100ms).
    - Load reduced automatically.
- **Foreground Service (FGS)** ensures background compute continues reliably.
- **Notification** shown while service is active.
- **Runtime permission handling**:
    - `POST_NOTIFICATIONS` (Android 13+)
    - `FOREGROUND_SERVICE_DATA_SYNC` (Android 14+)

---

## How it Works

1. When **Start** is pressed:
    - A foreground service (`TelemetryService`) starts.
    - It begins producing frames at 20Hz (or 10Hz in battery saver mode).
    - Each frame performs a **256×256 convolution computation** off the main thread.
    - Frame latency is measured and sent via a channel to the `TelemetryViewModel`.

2. The **ViewModel**:
    - Collects frames and updates state.
    - Tracks moving average latency.
    - Integrates **JankStats** to calculate jank percentage and count.

3. The **UI (Jetpack Compose)**:
    - Displays telemetry in real-time.
    - Provides a slider to change load.
    - Shows a toggle for Start/Stop.

---

## Permissions

- **Android 13+** → Requires `POST_NOTIFICATIONS`.
- **Android 14+** → Requires `FOREGROUND_SERVICE_DATA_SYNC`.
- The app requests permissions at runtime if not already granted.

---

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose (Material3)
- **Architecture**: MVVM (`TelemetryViewModel`)
- **Background Execution**: Foreground Service + Coroutines
- **Performance Monitoring**: JankStats
- **Benchmarking**: Macrobenchmark + Baseline Profiles (placeholders provided)

---

## Testing

- Verify Start/Stop toggle.
- Adjust load slider → latency increases with higher load.
- Put device in **battery saver mode** → frame rate halves and load decreases.
- Check that notifications appear (and permission request shown if needed).
- Observe Jank % and frame count update in the UI.

---

## Future Improvements

- Persist telemetry history.
- Add charts for latency/jank trends.
- Upload telemetry to server via WorkManager.

---
