package com.example.blekmpapp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.example.blekmpapp.theme.BLEAppTheme
import com.example.blekmpapp.ui.BleScreen
import com.example.blekmpapp.ui.DeviceDetailScreen
import com.example.blekmpapp.viewmodel.BleViewModel

@Composable
fun rememberViewModel(): BleViewModel = remember { BleViewModel() }

@Composable
fun App() {
    val viewModel = rememberViewModel()
    val selectedDevice by viewModel.selectedDevice.collectAsState()

    BLEAppTheme {
        if (selectedDevice == null) {
            // Show the main list screen
            val devices by viewModel.devices.collectAsState()
            val isScanning by viewModel.isScanning.collectAsState()
            val isDemoMode by viewModel.isDemoMode.collectAsState()

            BleScreen(
                devices = devices,
                isScanning = isScanning,
                isDemoMode = isDemoMode,
                onScanClick = { viewModel.startScan() },
                onDeviceClick = { device -> viewModel.selectDevice(device) },
                onToggleDemoMode = { viewModel.toggleDemoMode() },
            )
        } else {
            // Show the detail screen
            val connectionState by viewModel.connectionState.collectAsState()
            val batteryLevel by viewModel.batteryLevel.collectAsState()

            DeviceDetailScreen(
                device = selectedDevice!!,
                connectionState = connectionState,
                batteryLevel = batteryLevel,
                onBack = { viewModel.selectDevice(null) },
                onConnect = { viewModel.connect() },
                onDisconnect = { viewModel.disconnect() }
            )
        }
    }
}