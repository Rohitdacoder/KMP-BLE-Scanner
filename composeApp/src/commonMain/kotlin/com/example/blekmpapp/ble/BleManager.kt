package com.example.blekmpapp.ble

import androidx.compose.runtime.State
import kotlinx.coroutines.flow.StateFlow

/**
 * This is the common interface for the BLE manager.
 * It defines the contract that platform-specific implementations must follow.
 * The `expect` keyword signals that each platform (`androidMain`, `iosMain`, etc.)
 * will provide an `actual` implementation for this interface.
 */
interface BleManager {
    /**
     * A state flow that emits the current list of discovered BLE devices.
     */
    val scannedDevices: StateFlow<List<BleDevice>>

    /**
     * A state flow that emits the current connection state to a device.
     */
    val connectionState: StateFlow<ConnectionState>

    /**
     * A state flow that emits the current battery level of the connected device.
     */
    val batteryLevel: StateFlow<Int?>

    val isScanning: StateFlow<Boolean>

    /**
     * Starts scanning for nearby BLE devices.
     */
    fun startScan()

    /**
     * Stops the ongoing BLE scan.
     */
    fun stopScan()

    /**
     * Attempts to connect to a specified BLE device.
     */
    fun connect(device: BleDevice)

    /**
     * Disconnects from the currently connected device.
     */
    fun disconnect()

    /**
     * Cleans up resources used by the manager.
     */
    fun close()
}
