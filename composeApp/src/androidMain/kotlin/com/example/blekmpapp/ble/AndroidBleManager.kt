package com.example.blekmpapp.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

// INCORRECT IMPORT REMOVED - This was the build error.
@SuppressLint("MissingPermission")
class AndroidBleManager(private val context: Context) : BleManager {

    // --- Companion Object, Properties, and Flows (No changes here) ---
    companion object {
        val BATTERY_SERVICE_UUID: UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
        val BATTERY_LEVEL_CHARACTERISTIC_UUID: UUID =
            UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")
        private const val SCAN_PERIOD: Long = 10000
    }

    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    private val scanner by lazy { bluetoothAdapter?.bluetoothLeScanner }
    private var gatt: BluetoothGatt? = null
    private val _scannedDevices = MutableStateFlow<List<BleDevice>>(emptyList())
    override val scannedDevices: StateFlow<List<BleDevice>> = _scannedDevices.asStateFlow()
    private val _connectionState = MutableStateFlow(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    private val _batteryLevel = MutableStateFlow<Int?>(null)
    override val batteryLevel: StateFlow<Int?> = _batteryLevel.asStateFlow()

    private var connectingDeviceAddress: String? = null

    private val _isScanning = MutableStateFlow(false)
    override val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val handler = Handler(Looper.getMainLooper())

    // --- GATT Callback (No changes needed here) ---
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            val deviceAddress = gatt?.device?.address
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    // This is now simpler. We only get here if bonding is already complete.
                    Log.d(
                        "BleManager",
                        "GATT Connection successful to $deviceAddress. Discovering services."
                    )
                    _connectionState.value = ConnectionState.Connected
                    gatt?.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.d("BleManager", "Disconnected from $deviceAddress")
                    close() // Disconnect and clean up resources
                }
            } else {
                Log.e("BleManager", "GATT Connection Error for $deviceAddress. Status: $status")
                close() // Connection failed, clean up
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("BleManager", "Services discovered for ${gatt?.device?.address}")
                val batteryService = gatt?.getService(BATTERY_SERVICE_UUID)
                val batteryLevelCharacteristic =
                    batteryService?.getCharacteristic(BATTERY_LEVEL_CHARACTERISTIC_UUID)
                if (batteryLevelCharacteristic != null) {
                    Log.d("BleManager", "Found Battery Level characteristic. Reading value...")
                    gatt.readCharacteristic(batteryLevelCharacteristic)
                } else {
                    Log.w("BleManager", "Battery Level characteristic not found.")
                }
            } else {
                Log.w("BleManager", "Service discovery failed with status: $status")
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS && characteristic.uuid == BATTERY_LEVEL_CHARACTERISTIC_UUID) {
                val batteryLevelRead = value[0].toInt()
                Log.d("BleManager", "Battery level received: $batteryLevelRead%")
                _batteryLevel.value = batteryLevelRead
            }
        }
    }


    // --- Scan Callback (This is correct) ---
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (result.device.address == null) return
            val deviceName = result.scanRecord?.deviceName ?: result.device.name
            val bleDevice =
                BleDevice(name = deviceName, address = result.device.address, rssi = result.rssi)
            _scannedDevices.update { existingDevices ->
                val mutableDevices = existingDevices.toMutableList()
                val existingDeviceIndex =
                    mutableDevices.indexOfFirst { it.address == bleDevice.address }
                if (existingDeviceIndex != -1) {
                    mutableDevices[existingDeviceIndex] = bleDevice
                } else {
                    mutableDevices.add(bleDevice)
                }
                mutableDevices.sortedByDescending { it.rssi }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("BleManager", "BLE Scan Failed with error code: $errorCode")
        }
    }


    // --- Bonding BroadcastReceiver (Updated to initiate connection) ---
    private val bondStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != BluetoothDevice.ACTION_BOND_STATE_CHANGED) return

            val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            val bondState =
                intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR)

            // We only care about the device we are trying to bond with
            if(device?.address != connectingDeviceAddress)
            {
                return
            }

            when (bondState) {
                BluetoothDevice.BOND_BONDED -> {
                    Log.d(
                        "BleManager",
                        "Bonding successful with ${device?.address}. Now connecting GATT."
                    )
                    // **CRITICAL CHANGE**: Connect to GATT *after* bonding is complete.
                    gatt = device?.connectGatt(
                        context,
                        false,
                        gattCallback,
                        BluetoothDevice.TRANSPORT_LE
                    )
                }

                BluetoothDevice.BOND_NONE -> {
                    Log.w("BleManager", "Bonding failed or was cancelled for ${device?.address}.")
                    close() // Clean up if bonding fails
                }

                BluetoothDevice.BOND_BONDING -> {
                    Log.d("BleManager", "Bonding in progress with ${device?.address}...")
                }
            }
        }
    }


    // --- Connection Logic (Updated to handle bonding flow) ---
    override fun connect(device: BleDevice) {
        stopScan()
        Log.i("BleManager", "Attempting to connect to ${device.address}")
        _connectionState.value = ConnectionState.Connecting
        connectingDeviceAddress = device.address
        val remoteDevice = bluetoothAdapter?.getRemoteDevice(device.address)
        if (remoteDevice == null) {
            Log.e("BleManager", "Device not found for address: ${device.address}")
            close()
            return
        }

        // Check bond state first
        when (remoteDevice.bondState) {
            BluetoothDevice.BOND_BONDED -> {
                Log.d("BleManager", "Device is already bonded. Connecting GATT directly.")
                gatt = remoteDevice.connectGatt(
                    context,
                    false,
                    gattCallback,
                    BluetoothDevice.TRANSPORT_LE
                )
            }

            BluetoothDevice.BOND_NONE -> {
                Log.d("BleManager", "Device is not bonded. Initiating bonding process.")
                // Register receiver to listen for bond success
                context.registerReceiver(
                    bondStateReceiver,
                    IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
                )
                // Start the bonding process. The BroadcastReceiver will handle the next step.
                remoteDevice.createBond()
            }

            BluetoothDevice.BOND_BONDING -> {
                Log.w("BleManager", "Device is already in the process of bonding.")
                // We don't need to do anything, the existing process will continue.
                // It's good to register our receiver just in case we initiated the original process.
                context.registerReceiver(
                    bondStateReceiver,
                    IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
                )
            }
        }
    }

    // --- Other functions ---
    override fun startScan() {
        Log.d("BleManager", "Start Scan button clicked.")
        if (bluetoothAdapter?.isEnabled == false) {
            Log.e("BleManager", "Bluetooth is disabled.")
            return
        }
        if (scanner == null) {
            Log.e("BleManager", "BluetoothLeScanner is not available.")
            return
        }
        handler.postDelayed({
            if (_isScanning.value) {
                Log.i("BleManager", "Scan timed out. Stopping scan.")
                stopScan()
            }
        }, SCAN_PERIOD)

        _scannedDevices.value = emptyList()
        val scanSettings =
            ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        Log.i("BleManager", "Starting BLE scan...")
        _isScanning.value = true
        scanner?.startScan(null, scanSettings, scanCallback)
    }

    override fun stopScan() { /* ... unchanged ... */
        if (!_isScanning.value) return
        Log.i("BleManager", "Stopping BLE scan.")
        _isScanning.value = false
        scanner?.stopScan(scanCallback)
        handler.removeCallbacksAndMessages(null)
    }

    override fun disconnect() { /* ... unchanged ... */
        Log.i("BleManager", "Disconnecting from GATT server.")
        gatt?.disconnect()
    }

    override fun close() {
        // This function is now the central cleanup point
        try {
            context.unregisterReceiver(bondStateReceiver)
        } catch (e: Exception) {
            // Ignore if receiver was not registered
        }
        connectingDeviceAddress = null
        gatt?.close()
        gatt = null
        if (_connectionState.value != ConnectionState.Disconnected) {
            _connectionState.value = ConnectionState.Disconnected
        }
        _batteryLevel.value = null
        Log.d("BleManager", "Resources cleaned up.")
    }
}