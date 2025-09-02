package com.example.smartparkingapp.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.annotation.RequiresPermission
import com.google.firebase.Timestamp

object NotificationScheduler {

    @SuppressLint("ScheduleExactAlarm")
    @RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
    fun scheduleNotification(context: Context, startTime: Long, durationHours: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Postavi trigger vrijeme na 5 minuta nakon početka parkiranja (za test)
        val triggerTime = startTime + (30 * 1000) // 30 sekundi nakon početka

        // Intent za naš BroadcastReceiver koji šalje notifikaciju
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("durationHours", durationHours)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Postavi alarm u AlarmManageru
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            pendingIntent
        )
    }
}
