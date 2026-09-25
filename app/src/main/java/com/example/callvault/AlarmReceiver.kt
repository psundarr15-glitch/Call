package com.example.callvault

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Run backup check (sends calls or "no new calls" message)
        WorkManager.getInstance(context)
            .enqueue(OneTimeWorkRequestBuilder<BackupWorker>().build())
        // Schedule the next 4-hour slot
        ScheduleHelper.schedule(context)
    }
}
