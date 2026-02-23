package com.example.blekmpapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.blekmpapp.ble.BleDevice
import com.example.blekmpapp.ble.ConnectionState
import com.example.blekmpapp.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailScreen(
    device: BleDevice,
    connectionState: ConnectionState,
    batteryLevel: Int?,
    onBack: () -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("BLE Monitor", fontWeight = FontWeight.Bold)
                        Text(device.name ?: "Unknown", style = MaterialTheme.typography.bodySmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
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
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            ConnectionStatus(connectionState)
            Spacer(Modifier.height(16.dp))
            InfoCard(
                icon = Icons.Default.Bluetooth,
                title = "Device Name",
                value = device.name ?: "Unknown",
                backgroundColor = LightBlue,
                iconColor = AccentBlue
            )
            Spacer(Modifier.height(16.dp))
            InfoCard(
                icon = Icons.Default.Info,
                title = "Device ID",
                value = device.address
            )
            Spacer(Modifier.height(16.dp))
            BatteryCard(connectionState, batteryLevel)
            Spacer(Modifier.weight(1f))

            if (connectionState == ConnectionState.Connected) {
                OutlinedButton(
                    onClick = onDisconnect,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Disconnect Device")
                }
            } else {
                Button(
                    onClick = onConnect,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkBlue)
                ) {
                    Icon(
                        Icons.Default.Bluetooth,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Connect to Device")
                }
            }
        }
    }
}

@Composable
private fun ConnectionStatus(state: ConnectionState) {
    val color = when (state) {
        ConnectionState.Connected -> Green
        ConnectionState.Connecting -> Yellow
        else -> TextSecondary
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (state == ConnectionState.Connected) Green.copy(alpha = 0.1f) else Color.Transparent)
            .padding(vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = state.name,
            color = if (state == ConnectionState.Connected) Green else TextSecondary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun InfoCard(
    icon: ImageVector,
    title: String,
    value: String,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    iconColor: Color = Color.White
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(backgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor)
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Text(
                    value,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun BatteryCard(connectionState: ConnectionState, batteryLevel: Int?) {
    val backgroundColor =
        if (batteryLevel != null) Yellow.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
    val iconColor = if (batteryLevel != null) Yellow else Color.White

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.BatteryChargingFull, contentDescription = null, tint = iconColor)
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Battery Level",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                if (connectionState == ConnectionState.Connected && batteryLevel != null) {
                    Text(
                        "$batteryLevel%",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Yellow
                    )
                } else {
                    Text("Connect to view battery", color = TextSecondary)
                }
            }
        }
        if (connectionState == ConnectionState.Connected && batteryLevel != null) {
            LinearProgressIndicator(
                progress = { batteryLevel / 100f },
                modifier = Modifier.fillMaxWidth().height(8.dp).padding(horizontal = 16.dp),
                color = Yellow,
                trackColor = TextSecondary.copy(alpha = 0.3f)
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}