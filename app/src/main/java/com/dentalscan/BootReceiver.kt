package com.dentalscan

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dentalscan.utils.ReminderScheduler

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            val hour = prefs.getInt("notification_hour", 9)
            val minute = prefs.getInt("notification_minute", 0)
            val enabled = prefs.getBoolean("notification_enabled", false)
            if (enabled) {
                // Same frequency as before the restart (smart: the last smart interval; manual: the chosen one)
                ReminderScheduler.scheduleNext(
                    context, hour, minute,
                    ReminderScheduler.currentHours(context),
                    afterScan = false
                )
            }
        }
    }
}
