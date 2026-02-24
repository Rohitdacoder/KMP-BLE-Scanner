package com.example.blekmpapp.util

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class EventLoggerService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            val nodeInfo = event.source
            val clickedText = nodeInfo?.text ?: nodeInfo?.contentDescription ?: "Unknown Element"
            Log.i("EventLoggerService", "UI Element Clicked: '$clickedText'")
            nodeInfo?.recycle()
        }
    }

    override fun onInterrupt() {
        // This method is called when the service is interrupted.
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i("EventLoggerService", "Accessibility Service has been connected.")
    }
}