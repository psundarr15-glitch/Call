package com.example.callvault

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

object ScheduleHelper {

    // Fixed 4-hour slots: 00:00, 04:00, 08:00, 12:00, 16:00, 20:00
    private val SLOTS = listOf(0, 4, 8, 12, 16, 20)

    fun schedule(ctx: Context) {
        val am  = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi  = getPendingIntent(ctx)
        val cal = nextSlot()
        am.setAlarmClock(AlarmManager.AlarmClockInfo(cal.timeInMillis, pi), pi)
    }

    fun cancel(ctx: Context) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(getPendingIntent(ctx))
    }

    // Returns how many minutes until next slot fires
    fun minutesUntilNext(): Long {
        val now = System.currentTimeMillis()
        return (nextSlot().timeInMillis - now) / 60_000
    }

    fun nextSlotLabel(): String {
        val cal = nextSlot()
        val h = cal.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
        val m = cal.get(Calendar.MINUTE).toString().padStart(2, '0')
        return "$h:$m"
    }

    private fun nextSlot(): Calendar {
        val now  = Calendar.getInstance()
        val hour = now.get(Calendar.HOUR_OF_DAY)
        // Find the next slot hour after current hour
        val nextHour = SLOTS.firstOrNull { it > hour } ?: (SLOTS.first() + 24)
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, nextHour % 24)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (nextHour >= 24) add(Calendar.DAY_OF_YEAR, 1)
        }
    }

    private fun getPendingIntent(ctx: Context) = PendingIntent.getBroadcast(
        ctx, 100, Intent(ctx, AlarmReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}
