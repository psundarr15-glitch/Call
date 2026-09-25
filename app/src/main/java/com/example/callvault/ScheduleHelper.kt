package com.example.callvault

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

object ScheduleHelper {

    // Run once every hour, at HH:00.
    fun schedule(ctx: Context) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = getPendingIntent(ctx)
        val cal = nextHour()
        am.setAlarmClock(AlarmManager.AlarmClockInfo(cal.timeInMillis, pi), pi)
    }

    fun cancel(ctx: Context) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(getPendingIntent(ctx))
    }

    fun minutesUntilNext(): Long =
        (nextHour().timeInMillis - System.currentTimeMillis()) / 60_000

    fun nextSlotLabel(): String {
        val cal = nextHour()
        val h = cal.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
        return "$h:00"
    }

    private fun nextHour(): Calendar {
        val now = Calendar.getInstance()
        return Calendar.getInstance().apply {
            timeInMillis = now.timeInMillis
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.HOUR_OF_DAY, 1)
        }
    }

    private fun getPendingIntent(ctx: Context) = PendingIntent.getBroadcast(
        ctx, 100, Intent(ctx, AlarmReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}
