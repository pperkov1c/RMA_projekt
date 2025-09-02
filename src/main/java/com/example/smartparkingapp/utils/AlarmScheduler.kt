package com.example.smartparkingapp.utils

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.smartparkingapp.ParkingReminderReceiver

object AlarmScheduler {

    @SuppressLint("ScheduleExactAlarm")
    fun scheduleReminder(context: Context, triggerAtMillis: Long, plate: String, zone: String) {
        val intent = Intent(context, ParkingReminderReceiver::class.java).apply {
            putExtra("plate", plate)
            putExtra("zone", zone)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            plate.hashCode(), // različiti ID za svaki alarm
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )
    }
}
