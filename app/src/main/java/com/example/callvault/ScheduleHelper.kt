package com.example.callvault

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object ScheduleHelper {
    const val MODE_REGULAR = "REGULAR"
    const val MODE_DAILY_FULL = "DAILY_FULL"
    const val MODE_WEEKLY_FULL = "WEEKLY_FULL"

    // Regular incremental backup: every 4 hours at 00, 04, 08, 12, 16, 20.
    private val REGULAR_HOURS = intArrayOf(0, 4, 8, 12, 16, 20)

    // Weekly full backup days. Change these two values if you want different days.
    private val WEEKLY_FULL_DAYS = setOf(Calendar.TUESDAY, Calendar.FRIDAY)

    fun schedule(ctx: Context) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val event = nextEvent()
        val pi = getPendingIntent(ctx, event.mode)
        am.setAlarmClock(AlarmManager.AlarmClockInfo(event.time, pi), pi)
    }

    fun cancel(ctx: Context) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(getPendingIntent(ctx, MODE_REGULAR))
        am.cancel(getPendingIntent(ctx, MODE_DAILY_FULL))
        am.cancel(getPendingIntent(ctx, MODE_WEEKLY_FULL))
    }

    data class Event(val time: Long, val mode: String)

    fun nextEvent(from: Long = System.currentTimeMillis()): Event {
        val candidates = mutableListOf<Event>()
        candidates += Event(nextRegular(from), MODE_REGULAR)
        candidates += Event(nextDailyFull(from), MODE_DAILY_FULL)
        candidates += Event(nextWeeklyFull(from), MODE_WEEKLY_FULL)
        return candidates.minBy { it.time }
    }

    fun nextSlotLabel(): String {
        val e = nextEvent()
        val time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(e.time))
        return when (e.mode) {
            MODE_DAILY_FULL -> "$time • DAILY FULL"
            MODE_WEEKLY_FULL -> "$time • WEEKLY FULL"
            else -> "$time • 4-HOUR"
        }
    }

    fun minutesUntilNext(): Long =
        ((nextEvent().time - System.currentTimeMillis()).coerceAtLeast(0L)) / 60_000L

    fun modeLabel(mode: String): String = when (mode) {
        MODE_DAILY_FULL -> "DAILY FULL BACKUP"
        MODE_WEEKLY_FULL -> "WEEKLY FULL BACKUP"
        else -> "4-HOUR BACKUP"
    }

    private fun nextRegular(from: Long): Long {
        val base = Calendar.getInstance().apply { timeInMillis = from }
        for (dayOffset in 0..2) {
            val day = Calendar.getInstance().apply {
                timeInMillis = base.timeInMillis
                add(Calendar.DAY_OF_YEAR, dayOffset)
                set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            for (hour in REGULAR_HOURS) {
                day.set(Calendar.HOUR_OF_DAY, hour)
                if (day.timeInMillis > from) return day.timeInMillis
            }
        }
        error("No regular backup slot")
    }

    private fun nextDailyFull(from: Long): Long {
        val c = Calendar.getInstance().apply {
            timeInMillis = from
            set(Calendar.HOUR_OF_DAY, 19); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        if (c.timeInMillis <= from) c.add(Calendar.DAY_OF_YEAR, 1)
        return c.timeInMillis
    }

    private fun nextWeeklyFull(from: Long): Long {
        for (offset in 0..7) {
            val c = Calendar.getInstance().apply {
                timeInMillis = from
                add(Calendar.DAY_OF_YEAR, offset)
                set(Calendar.HOUR_OF_DAY, 17); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            if (c.timeInMillis > from && WEEKLY_FULL_DAYS.contains(c.get(Calendar.DAY_OF_WEEK))) {
                return c.timeInMillis
            }
        }
        error("No weekly full backup slot")
    }

    private fun getPendingIntent(ctx: Context, mode: String) = PendingIntent.getBroadcast(
        ctx,
        mode.hashCode(),
        Intent(ctx, AlarmReceiver::class.java).putExtra(AlarmReceiver.EXTRA_MODE, mode),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}
