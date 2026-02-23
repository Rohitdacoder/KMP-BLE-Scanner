package com.example.blekmpapp.ble

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.cinterop.addressOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.CoreBluetooth.CBAdvertisementDataLocalNameKey
import platform.CoreBluetooth.CBCentralManager
import platform.CoreBluetooth.CBCentralManagerDelegateProtocol
import platform.CoreBluetooth.CBCharacteristic
import platform.CoreBluetooth.CBManagerStatePoweredOff
import platform.CoreBluetooth.CBManagerStatePoweredOn
import platform.CoreBluetooth.CBPeripheral
import platform.CoreBluetooth.CBPeripheralDelegateProtocol
import platform.CoreBluetooth.CBService
import platform.CoreBluetooth.CBUUID
import platform.Foundation.NSError
import platform.Foundation.NSNumber
import platform.darwin.NSObject // ADD THIS IMPORT
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData

// The IOSBleManager is a delegate for the CBCentralManager, meaning it receives events from it.
@OptIn(ExperimentalForeignApi::class)
class IOSBleManager(private val centralManager: CBCentralManager) : NSObject(),
    CBCentralManagerDelegateProtocol, CBPeripheralDelegateProtocol, BleManager {

    // --- Mappings and private state ---
    private val discoveredPeripherals = mutableMapOf<String, CBPeripheral>()
    private var connectedPeripheral: CBPeripheral? = null

    // --- State Flows to communicate with the ViewModel ---
    private val _scannedDevices = MutableStateFlow<List<BleDevice>>(emptyList())
    override val scannedDevices: StateFlow<List<BleDevice>> = _scannedDevices.asStateFlow()

    private val _connectionState = MutableStateFlow(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _batteryLevel = MutableStateFlow<Int?>(null)
    override val batteryLevel: StateFlow<Int?> = _batteryLevel.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    override val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    // --- Standard BLE UUIDs ---
    private val batteryServiceUUID = CBUUID.UUIDWithString("180F")
    private val batteryLevelCharacteristicUUID = CBUUID.UUIDWithString("2A19")

    init {
        // Set this class as the delegate to receive BLE events.
        centralManager.delegate = this
    }

    // --- BleManager Interface Implementation ---

    override fun startScan() {
        if (centralManager.state == CBManagerStatePoweredOn) {
            _isScanning.value = true
            _scannedDevices.value = emptyList()
            discoveredPeripherals.clear()
            // Start scanning for peripherals. nil means scan for all devices.
            centralManager.scanForPeripheralsWithServices(null, null)
        } else {
            // TODO: Handle Bluetooth being off (e.g., show an error message to the user)
        }
    }

    override fun stopScan() {
        if (_isScanning.value) {
            _isScanning.value = false
            centralManager.stopScan()
        }
    }

    override fun connect(device: BleDevice) {
        stopScan()
        val peripheral = discoveredPeripherals[device.address]
        if (peripheral != null) {
            _connectionState.value = ConnectionState.Connecting
            centralManager.connectPeripheral(peripheral, null)
        } else {
            // Device not found in the scanned list, handle error
            _connectionState.value = ConnectionState.Error
        }
    }

    override fun disconnect() {
        connectedPeripheral?.let {
            centralManager.cancelPeripheralConnection(it)
        }
    }

    override fun close() {
        disconnect()
    }


    // --- CBCentralManagerDelegateProtocol Callbacks ---

    override fun centralManagerDidUpdateState(central: CBCentralManager) {
        when (central.state) {
            CBManagerStatePoweredOn -> { /* Ready to scan */
            }

            CBManagerStatePoweredOff -> { /* Handle BLE being off */
            }

            else -> {}
        }
    }

    override fun centralManager(
        central: CBCentralManager,
        didDiscoverPeripheral: CBPeripheral,
        advertisementData: Map<Any?, *>,
        RSSI: NSNumber
    ) {
        val name = didDiscoverPeripheral.name
            ?: (advertisementData[CBAdvertisementDataLocalNameKey] as? String)
        val address = didDiscoverPeripheral.identifier.UUIDString

        // Store the peripheral object for later connection
        discoveredPeripherals[address] = didDiscoverPeripheral

        val newDevice = BleDevice(
            name = name,
            address = address,
            rssi = RSSI.intValue,
            isDemoDevice = false
        )

        _scannedDevices.value = (_scannedDevices.value + newDevice).distinctBy { it.address }
    }

    override fun centralManager(central: CBCentralManager, didConnectPeripheral: CBPeripheral) {
        _connectionState.value = ConnectionState.Connected
        connectedPeripheral = didConnectPeripheral
        didConnectPeripheral.delegate = this
        // After connecting, discover the specific battery service
        didConnectPeripheral.discoverServices(listOf(batteryServiceUUID))
    }

    // THE FIX: This is the correct signature. It matches what you had, but having a clean file ensures no hidden characters or IDE issues.
    @ObjCSignatureOverride
    override fun centralManager(
        central: CBCentralManager,
        didFailToConnectPeripheral: CBPeripheral,
        error: NSError?
    ) {
        _connectionState.value = ConnectionState.Error
        connectedPeripheral = null
    }

    @ObjCSignatureOverride
    override fun centralManager(
        central: CBCentralManager,
        didDisconnectPeripheral: CBPeripheral,
        error: NSError?
    ) {
        _connectionState.value = ConnectionState.Disconnected
        _batteryLevel.value = null
        connectedPeripheral = null
    }


    // --- CBPeripheralDelegateProtocol Callbacks ---

    override fun peripheral(peripheral: CBPeripheral, didDiscoverServices: NSError?) {
        if (didDiscoverServices != null) {
            // Handle error
            return
        }
        // Safely cast the services list
        val services = peripheral.services as? List<CBService>
        services?.firstOrNull { it.UUID == batteryServiceUUID }?.let { service ->
            // Found the battery service, now discover its characteristics
            peripheral.discoverCharacteristics(
                listOf(batteryLevelCharacteristicUUID),
                forService = service
            )
        }
    }

    override fun peripheral(
        peripheral: CBPeripheral,
        didDiscoverCharacteristicsForService: CBService,
        error: NSError?
    ) {
        if (error != null) {
            // Handle error
            return
        }
        // Safely cast the characteristics list
        val characteristics =
            didDiscoverCharacteristicsForService.characteristics as? List<CBCharacteristic>
        characteristics?.firstOrNull { it.UUID == batteryLevelCharacteristicUUID }
            ?.let { characteristic ->
                // Found the battery level characteristic, read its value
                peripheral.readValueForCharacteristic(characteristic)
            }
    }

    override fun peripheral(
        peripheral: CBPeripheral,
        didUpdateValueForCharacteristic: CBCharacteristic,
        error: NSError?
    ) {
        if (error != null) {
            // Handle error
            return
        }
        // Correctly check UUID and parse the value
        if (didUpdateValueForCharacteristic.UUID == batteryLevelCharacteristicUUID) {
            val value = didUpdateValueForCharacteristic.value
            if (value != null) {
                // The battery level is a single byte (UInt8).
                val bytes = value.toByteArray()
                val batteryLevelValue = bytes.firstOrNull()?.toInt()
                _batteryLevel.value = batteryLevelValue
                println("Battery level received: $batteryLevelValue%")
            }
        }
    }
}

// Helper function to convert NSData to ByteArray
@OptIn(ExperimentalForeignApi::class)
fun NSData.toByteArray(): ByteArray = ByteArray(this.length.toInt()).apply {
    usePinned {
        platform.posix.memcpy(it.addressOf(0), this@toByteArray.bytes, this@toByteArray.length)
    }
}