package com.example.blekmpapp

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.blekmpapp.ble.PlatformSDK

class MainActivity : ComponentActivity() {

    // 1. Modern way to handle permission requests.
    // This launcher will request permissions and handle the result in a callback.
    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            // You can check here if all permissions were granted
            if (permissions.values.all { it }) {
                // Permissions granted. The app can now function correctly.
                // You could trigger an event here if needed, but for now, the user can just tap "Scan".
            } else {
                // Permissions denied. You should show a message to the user.
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Initialize the PlatformSDK so the ViewModel factory can get the context.
        PlatformSDK.init(applicationContext)

        // 2. Ask for permissions as soon as the app starts.
        askBluetoothPermission()

        setContent {
            App()
        }
    }


    private fun askBluetoothPermission() {
        // THE FIX: Use a mutable list and add permissions based on Android version.
        // This is the most robust way to handle BLE permissions.
        val permissionsToRequest = mutableListOf<String>()

        // For Android 12 (API 31) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            // For older Android versions
            permissionsToRequest.add(Manifest.permission.BLUETOOTH)
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_ADMIN)
        }

        // Location permissions are required for scanning regardless of version on many devices
        permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)

        requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}