package com.dentalscan

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.dentalscan.utils.ReminderScheduler

class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "dental_reminder_channel"
        const val NOTIFICATION_ID = 1001
    }

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val isUrdu = prefs.getBoolean("urdu_language", false)
        val intervalDays = prefs.getInt("reminder_interval_days", 7)

        val frequencyText = when (intervalDays) {
            1 -> if (isUrdu) "روزانہ" else "daily"
            3 -> if (isUrdu) "ہر 3 دن" else "every 3 days"
            else -> if (isUrdu) "ہفتہ وار" else "weekly"
        }

        val title = if (isUrdu) "DentalScan AI یاددہانی" else "DentalScan AI Reminder"
        val message = if (isUrdu)
            "آپ کا $frequencyText اسکین کا وقت ہے! اپنے دانتوں کی صحت چیک کریں۔"
        else
            "Time for your $frequencyText dental scan! Keep your oral health on track."

        createNotificationChannel(context)

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }

        // Reschedule next reminder automatically
        ReminderScheduler.scheduleDaily(
            context,
            prefs.getInt("notification_hour", 9),
            prefs.getInt("notification_minute", 0),
            intervalDays
        )
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Dental Reminders"
            val descriptionText = "Daily dental scan reminders"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}