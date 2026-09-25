package com.example.callvault

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val store = AlarmTimeStore(context)

        // Show "alarm ringing" notification with sound + vibration
        NotificationHelper.showAlarm(context, store.label())

        // Run backup in WorkManager (background thread, waits for network)
        WorkManager.getInstance(context)
            .enqueue(OneTimeWorkRequestBuilder<BackupWorker>().build())

        // Reschedule for same time tomorrow
        ScheduleHelper.schedule(context)
    }
}
