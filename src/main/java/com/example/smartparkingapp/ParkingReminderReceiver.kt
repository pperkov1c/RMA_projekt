package com.example.smartparkingapp

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ParkingReminderReceiver : BroadcastReceiver() {
    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        val plate = intent.getStringExtra("plate") ?: "vozilo"
        val zone = intent.getStringExtra("zone") ?: "zona"

        NotificationHelper.showNotification(
            context,
            "Parkiranje uskoro završava",
            "Vaše parkiranje za $plate u $zone završava za 5 minuta."
        )
    }
}
