package com.example.blekmpapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.blekmpapp.ble.BleDevice
import com.example.blekmpapp.theme.AccentBlue
import com.example.blekmpapp.theme.DarkBlue
import com.example.blekmpapp.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BleScreen(
    devices: List<BleDevice>,
    isScanning: Boolean,
    isDemoMode: Boolean,
    onScanClick: () -> Unit,
    onDeviceClick: (BleDevice) -> Unit,
    onToggleDemoMode: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("BLE Monitor", fontWeight = FontWeight.Bold)
                        Text("Available Devices", style = MaterialTheme.typography.bodySmall)
                    }
                },
                actions = {
                    IconButton(onClick = onToggleDemoMode) {
                        Icon(
                            Icons.Default.Lightbulb,
                            contentDescription = "Toggle Demo Mode",
                            tint = if (isDemoMode) Color.Yellow else Color.White
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(AccentBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Bluetooth,
                            contentDescription = "Bluetooth",
                            tint = Color.White
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            if (devices.isEmpty()) {
                Surface(color = AccentBlue, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "💡 Tap \"Scan\" to find nearby BLE devices",
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onScanClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DarkBlue),
                enabled = !isScanning
            ) {
                Icon(
                    Icons.Default.Bluetooth,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(if (isScanning) "Scanning for devices..." else "Scan for BLE Devices")
            }
            Spacer(Modifier.height(16.dp))

            if (isScanning) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (devices.isEmpty()) {
                NoDevicesFound()
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(devices) { device ->
                        DeviceRow(device = device, onClick = { onDeviceClick(device) })
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceRow(device: BleDevice, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(AccentBlue.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Bluetooth, contentDescription = null, tint = AccentBlue)
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(device.name ?: "Unknown Device", fontWeight = FontWeight.Bold)
            Text(device.address, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Icon(
            Icons.Default.ArrowForwardIos,
            contentDescription = "Details",
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun NoDevicesFound() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(AccentBlue.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Bluetooth,
                contentDescription = null,
                tint = AccentBlue,
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "No Devices Found",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Tap the button above to start scanning for nearby\nBluetooth devices",
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}