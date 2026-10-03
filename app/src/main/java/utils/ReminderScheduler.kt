package com.dentalscan.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.StringRes
import com.dentalscan.R
import com.dentalscan.ReminderReceiver
import com.dentalscan.data.ScanEntity
import java.util.Calendar

/**
 * Schedules the dental scan reminder.
 *
 * Two modes (pref "reminder_mode"):
 *  - manual: the user's own choice (daily / every 3 days / weekly), pref "reminder_interval_days"
 *  - smart : the interval follows the last scan result, see [smartHours]
 *
 * Intervals are in hours so "twice a day" (12 h) fits next to the longer ones.
 */
object ReminderScheduler {

    const val MODE_SMART = "smart"
    const val MODE_MANUAL = "manual"

    private const val PREFS = "settings"
    private const val KEY_MODE = "reminder_mode"
    private const val KEY_MANUAL_DAYS = "reminder_interval_days"
    private const val KEY_SMART_HOURS = "smart_interval_hours"
    private const val HOUR_MS = 3_600_000L

    // ---------- Mode and intervals ----------

    /**
     * Smart for new users. Someone who already picked a manual frequency before this option
     * existed keeps manual until they choose Smart themselves.
     */
    fun mode(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY_MODE, null)
            ?: if (prefs.contains(KEY_MANUAL_DAYS)) MODE_MANUAL else MODE_SMART
    }

    /**
     * Hours between reminders in smart mode:
     *  no scans yet          every 3 days  (encourage the first scan)
     *  Critical risk         twice a day
     *  last scan Healthy     every 2 weeks
     *  last scan Low         weekly
     *  last scan Medium      every 3 days
     *  last scan High        daily
     */
    fun smartHours(lastScan: ScanEntity?, risk: RiskAssessment.Result?): Int = when {
        lastScan == null -> 72
        risk?.level == RiskAssessment.Level.CRITICAL -> 12
        ConditionStyle.isHealthy(lastScan.condition) -> 336
        lastScan.severity == "High" -> 24
        lastScan.severity == "Medium" -> 72
        else -> 168
    }

    fun smartHours(scans: List<ScanEntity>): Int =
        smartHours(scans.maxByOrNull { it.timestamp }, RiskAssessment.assess(scans))

    /** Last smart interval that was scheduled (used after a reboot, when no database query is wanted). */
    fun savedSmartHours(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_SMART_HOURS, 72)

    fun saveSmartHours(context: Context, hours: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(KEY_SMART_HOURS, hours).apply()
    }

    @StringRes
    fun labelRes(hours: Int): Int = when (hours) {
        12 -> R.string.freq_twice_daily
        24 -> R.string.freq_daily
        72 -> R.string.freq_every_3_days
        336 -> R.string.freq_every_2_weeks
        else -> R.string.freq_weekly
    }

    /** Hours to use right now for the saved mode, from the saved values only. */
    fun currentHours(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return if (mode(context) == MODE_SMART) savedSmartHours(context)
        else prefs.getInt(KEY_MANUAL_DAYS, 7) * 24
    }

    // ---------- Scheduling ----------

    /** Original entry point, kept for the manual frequencies (interval in days). */
    fun scheduleDaily(context: Context, hour: Int, minute: Int, intervalDays: Int = 7) {
        scheduleNext(context, hour, minute, intervalDays * 24, afterScan = false)
    }

    /**
     * Sets the next reminder at hour:minute.
     * @param afterScan true right after a scan is saved: wait a full interval instead of reminding today.
     */
    fun scheduleNext(context: Context, hour: Int, minute: Int, intervalHours: Int, afterScan: Boolean) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAt = nextTrigger(hour, minute, intervalHours, afterScan, System.currentTimeMillis())

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                }
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
            else -> {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        }
    }

    private fun nextTrigger(hour: Int, minute: Int, intervalHours: Int, afterScan: Boolean, now: Long): Long {
        val base = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val interval = intervalHours.coerceAtLeast(1) * HOUR_MS
        // A time within the next minute counts as already due, so a reminder never repeats at once
        val due = now + 60_000L

        return if (intervalHours < 24) {
            // Several a day (for example 09:00 and 21:00): the first one still to come
            var t = base
            while (t <= due) t += interval
            t
        } else if (afterScan || base <= due) {
            base + interval
        } else {
            base // today's time is still ahead: first reminder today, as before
        }
    }

    /**
     * Called every time a scan is saved. In smart mode the frequency is worked out again from
     * the new scan result and the next reminder is moved accordingly. Manual mode is left alone.
     */
    fun updateAfterScan(context: Context, scans: List<ScanEntity>) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (mode(context) != MODE_SMART) return
        val hours = smartHours(scans)
        saveSmartHours(context, hours)
        if (!prefs.getBoolean("notification_enabled", false)) return
        scheduleNext(
            context,
            prefs.getInt("notification_hour", 9),
            prefs.getInt("notification_minute", 0),
            hours,
            afterScan = true
        )
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun isScheduled(context: Context): Boolean {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        return pendingIntent != null
    }
}
