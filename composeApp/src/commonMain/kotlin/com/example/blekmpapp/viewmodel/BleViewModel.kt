package com.example.blekmpapp.viewmodel

import com.example.blekmpapp.ble.BleDevice
import com.example.blekmpapp.ble.BleManager
import com.example.blekmpapp.ble.ConnectionState
import com.example.blekmpapp.ble.createBleManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BleViewModel {

    // --- Real BLE Manager ---
    private val bleManager: BleManager = createBleManager()
    private val viewModelScope = CoroutineScope(Dispatchers.Default)

    // --- Demo Mode State ---
    private val _isDemoMode = MutableStateFlow(false)
    val isDemoMode: StateFlow<Boolean> = _isDemoMode.asStateFlow()

    // --- State for Demo Mode ONLY ---
    private val _demoDevices = MutableStateFlow<List<BleDevice>>(emptyList())
    private val _demoConnectionState = MutableStateFlow(ConnectionState.Disconnected)
    private val _demoBatteryLevel = MutableStateFlow<Int?>(null)
    private val _demoIsScanning = MutableStateFlow(false)

    // --- Navigation State ---
    private val _selectedDevice = MutableStateFlow<BleDevice?>(null)
    val selectedDevice: StateFlow<BleDevice?> = _selectedDevice.asStateFlow()

    // --- THE FIX: Publicly exposed state now chooses its source based on demo mode ---
    val devices: StateFlow<List<BleDevice>>
        get() = if (_isDemoMode.value) _demoDevices else bleManager.scannedDevices

    val connectionState: StateFlow<ConnectionState>
        get() = if (_isDemoMode.value) _demoConnectionState else bleManager.connectionState

    val batteryLevel: StateFlow<Int?>
        get() = if (_isDemoMode.value) _demoBatteryLevel else bleManager.batteryLevel

    val isScanning: StateFlow<Boolean>
        get() = if (_isDemoMode.value) _demoIsScanning else bleManager.isScanning


    fun selectDevice(device: BleDevice?) {
        if (device == null) {
            disconnect() // Call the disconnect function to handle logic
        }
        _selectedDevice.value = device
        // Reset connection state only for the demo flows
        _demoConnectionState.value = ConnectionState.Disconnected
        _demoBatteryLevel.value = null
    }

    fun toggleDemoMode() {
        _isDemoMode.value = !_isDemoMode.value
        // Stop any real BLE operations when switching to demo mode
        if (_isDemoMode.value) {
            bleManager.stopScan()
            bleManager.disconnect()
        }
        _selectedDevice.value = null // Go back to the main screen
        // Reset demo state
        _demoDevices.value = emptyList()
        _demoConnectionState.value = ConnectionState.Disconnected
        _demoBatteryLevel.value = null
        _demoIsScanning.value = false
    }

    fun startScan() {
        _selectedDevice.value = null // Ensure we are on the scan screen
        if (_isDemoMode.value) {
            // --- DEMO SCAN ---
            _demoIsScanning.value = true
            _demoDevices.value = emptyList()
            viewModelScope.launch {
                delay(1500)
                _demoDevices.value = listOf(
                    BleDevice("Fitness Tracker Pro", "AA:BB:CC:DD:EE:01", -50, true),
                    BleDevice("Smart Watch X1", "AA:BB:CC:DD:EE:02", -65, true),
                    BleDevice("Heart Rate Monitor", "AA:BB:CC:DD:EE:03", -75, true)
                )
                _demoIsScanning.value = false
            }
        } else {
            // --- REAL SCAN ---
            // The isScanning state will be updated automatically by the bleManager
            bleManager.startScan()
        }
    }

    fun connect() {
        _selectedDevice.value?.let { device ->
            if (device.isDemoDevice) {
                // --- DEMO CONNECT ---
                _demoConnectionState.value = ConnectionState.Connecting
                viewModelScope.launch {
                    delay(2000)
                    _demoConnectionState.value = ConnectionState.Connected
                    delay(500)
                    _demoBatteryLevel.value = 32
                }
            } else {
                // --- REAL CONNECT ---
                bleManager.connect(device)
            }
        }
    }

    fun disconnect() {
        if (_isDemoMode.value) {
            _demoConnectionState.value = ConnectionState.Disconnected
            _demoBatteryLevel.value = null
        } else {
            bleManager.disconnect()
        }
    }
}