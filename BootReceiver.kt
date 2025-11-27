package com.hospital.alert

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Restart the alert service
            val serviceIntent = Intent(context, AlertListenerService::class.java)
            ContextCompat.startForegroundService(context, serviceIntent)
        }
    }
}
