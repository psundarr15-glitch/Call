package com.example.callvault

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

object ScheduleHelper {

    fun scheduleDailyAt5AM(ctx: Context) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = getPendingIntent(ctx)

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 5)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (!after(Calendar.getInstance())) add(Calendar.DAY_OF_YEAR, 1)
        }

        // setAlarmClock: fires even in Doze mode, no special permission needed,
        // shows clock icon in status bar — most reliable alarm type
        am.setAlarmClock(
            AlarmManager.AlarmClockInfo(cal.timeInMillis, pi),
            pi
        )
    }

    fun cancel(ctx: Context) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(getPendingIntent(ctx))
    }

    fun hoursUntil5AM(): Long {
        val now    = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 5)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (!after(now)) add(Calendar.DAY_OF_YEAR, 1)
        }
        return (target.timeInMillis - now.timeInMillis) / (1000 * 60 * 60)
    }

    private fun getPendingIntent(ctx: Context) = PendingIntent.getBroadcast(
        ctx, 100, Intent(ctx, AlarmReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}
