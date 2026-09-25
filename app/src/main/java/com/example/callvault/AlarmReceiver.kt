package com.example.callvault

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

// Fires at 5 AM via AlarmManager.setAlarmClock
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Enqueue backup via WorkManager (handles network wait + background thread)
        val work = OneTimeWorkRequestBuilder<BackupWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        WorkManager.getInstance(context).enqueue(work)

        // Reschedule for tomorrow — setAlarmClock fires once, not repeating
        ScheduleHelper.scheduleDailyAt5AM(context)
    }
}
