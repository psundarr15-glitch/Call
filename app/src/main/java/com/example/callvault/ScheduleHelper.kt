package com.example.callvault

import android.content.Context
import androidx.work.*
import java.util.Calendar
import java.util.concurrent.TimeUnit

object ScheduleHelper {

    fun scheduleDailyAt5AM(ctx: Context) {
        val delay = delayToNext5AM()

        val request = PeriodicWorkRequestBuilder<BackupWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        // KEEP — don't replace existing schedule if already set
        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
            "callvault_daily_5am",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun hoursUntil5AM(): Long {
        return delayToNext5AM() / (1000 * 60 * 60)
    }

    private fun delayToNext5AM(): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 5)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // Already past 5 AM today → schedule tomorrow
            if (!after(now)) add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis - now.timeInMillis
    }
}
