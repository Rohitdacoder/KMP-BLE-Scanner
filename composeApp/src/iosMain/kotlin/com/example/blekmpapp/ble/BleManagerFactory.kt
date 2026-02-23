package com.example.blekmpapp.ble

import platform.CoreBluetooth.CBCentralManager
import platform.darwin.dispatch_get_main_queue

// This is the actual iOS implementation of the factory.
actual fun createBleManager(): BleManager {
    // On iOS, the CBCentralManager is the core component for all BLE client operations.
    // We create it here and pass it to our IOSBleManager.
    val centralManager = CBCentralManager(null, dispatch_get_main_queue())
    return IOSBleManager(centralManager)
}