package com.example.blekmpapp.ble

import android.content.Context

// This is the actual Android implementation of the factory.
// It requires a Context to create the AndroidBleManager.
actual fun createBleManager(): BleManager {
    return AndroidBleManager(PlatformSDK.applicationContext)
}

// We need a way to get the context to the factory. A simple
// object can hold the application context.
object PlatformSDK {
    lateinit var applicationContext: Context
        private set

    fun init(context: Context) {
        applicationContext = context
    }
}