package com.example.callvault

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Only one backup schedule is supported:
 * a FULL backup every 4 hours.
 */
object ScheduleHelper {
    const val MODE_REGULAR = "REGULAR"

    // Full backup slots: 00:00, 04:00, 08:00, 12:00, 16:00, 20:00.
    private val BACKUP_HOURS = intArrayOf(0, 4, 8, 12, 16, 20)

    data class Event(val time: Long, val mode: String = MODE_REGULAR)

    fun schedule(ctx: Context) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val event = nextEvent()
        val pi = getPendingIntent(ctx)
        am.setAlarmClock(AlarmManager.AlarmClockInfo(event.time, pi), pi)
    }

    fun cancel(ctx: Context) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(getPendingIntent(ctx))
    }

    fun nextEvent(from: Long = System.currentTimeMillis()): Event =
        Event(nextBackupSlot(from))

    fun nextSlotLabel(): String {
        val e = nextEvent()
        val time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(e.time))
        return "$time • 4-HOUR FULL BACKUP"
    }

    fun minutesUntilNext(): Long =
        ((nextEvent().time - System.currentTimeMillis()).coerceAtLeast(0L)) / 60_000L

    fun modeLabel(mode: String = MODE_REGULAR): String = "4-HOUR FULL BACKUP"

    private fun nextBackupSlot(from: Long): Long {
        val base = java.util.Calendar.getInstance().apply { timeInMillis = from }
        for (dayOffset in 0..2) {
            val day = java.util.Calendar.getInstance().apply {
                timeInMillis = base.timeInMillis
                add(java.util.Calendar.DAY_OF_YEAR, dayOffset)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            for (hour in BACKUP_HOURS) {
                day.set(java.util.Calendar.HOUR_OF_DAY, hour)
                if (day.timeInMillis > from) return day.timeInMillis
            }
        }
        error("No 4-hour backup slot")
    }

    private fun getPendingIntent(ctx: Context) = PendingIntent.getBroadcast(
        ctx,
        MODE_REGULAR.hashCode(),
        Intent(ctx, AlarmReceiver::class.java).putExtra(AlarmReceiver.EXTRA_MODE, MODE_REGULAR),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}
