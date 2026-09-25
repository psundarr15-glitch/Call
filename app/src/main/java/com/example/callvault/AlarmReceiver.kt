package com.example.callvault

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Backup exactly the elapsed window since the previous successful backup.
        WorkManager.getInstance(context)
            .enqueue(OneTimeWorkRequestBuilder<BackupWorker>().build())

        // Schedule the next HH:00 hourly backup.
        ScheduleHelper.schedule(context)
    }
}
