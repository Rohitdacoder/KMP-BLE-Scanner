package com.example.blekmpapp.ble

data class BleDevice(
    val name: String?,      // Device name (may be null)
    val address: String,    // MAC address (Android) or UUID (iOS)
    val rssi: Int? = null,   // Signal strength (optional)
    val isDemoDevice: Boolean = false
)

enum class ConnectionState {
    Connecting,
    Connected,
    Disconnected,
    Error
}