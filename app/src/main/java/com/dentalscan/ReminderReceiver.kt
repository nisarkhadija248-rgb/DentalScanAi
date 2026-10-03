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
import androidx.core.app.TaskStackBuilder
import com.dentalscan.data.ScanEntity
import com.dentalscan.utils.ConditionStyle
import com.dentalscan.utils.HealthScore
import com.dentalscan.utils.LanguageManager
import com.dentalscan.utils.ReminderScheduler
import com.dentalscan.utils.RiskAssessment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "dental_reminder_channel"
        const val NOTIFICATION_ID = 1001
    }

    override fun onReceive(context: Context, intent: Intent) {
        // The last scan comes from the database, so finish the work off the main thread
        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                showReminderAndScheduleNext(appContext)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun showReminderAndScheduleNext(context: Context) {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

        val scans: List<ScanEntity> = try {
            HealthScore.scansFor(context)
        } catch (e: Exception) {
            emptyList()
        }
        val lastScan = scans.maxByOrNull { it.timestamp }
        val risk = RiskAssessment.assess(scans)

        // Text in the app's language (strings.xml / values-ur)
        val localized = LanguageManager.setLocale(context)
        val messageRes = when {
            lastScan == null -> R.string.reminder_msg_first
            risk?.level == RiskAssessment.Level.CRITICAL -> R.string.reminder_msg_critical
            ConditionStyle.isHealthy(lastScan.condition) -> R.string.reminder_msg_healthy
            lastScan.severity == "High" -> R.string.reminder_msg_high
            lastScan.severity == "Medium" -> R.string.reminder_msg_medium
            else -> R.string.reminder_msg_low
        }
        val title = localized.getString(R.string.reminder_title)
        val message = localized.getString(messageRes)

        createNotificationChannel(context)

        // Tapping opens the camera, with the home screen behind it so Back goes home
        val tapIntent = TaskStackBuilder.create(context)
            .addNextIntent(Intent(context, MainActivity::class.java))
            .addNextIntent(Intent(context, CameraActivity::class.java))
            .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(tapIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }

        // Next reminder: smart mode works the interval out again from the latest data
        val hours = if (ReminderScheduler.mode(context) == ReminderScheduler.MODE_SMART) {
            ReminderScheduler.smartHours(lastScan, risk).also { ReminderScheduler.saveSmartHours(context, it) }
        } else {
            prefs.getInt("reminder_interval_days", 7) * 24
        }
        ReminderScheduler.scheduleNext(
            context,
            prefs.getInt("notification_hour", 9),
            prefs.getInt("notification_minute", 0),
            hours,
            afterScan = false
        )
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Dental Reminders"
            val descriptionText = "Dental scan reminders"
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
