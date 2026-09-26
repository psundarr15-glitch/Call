package com.example.callvault

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val mode = intent.getStringExtra(EXTRA_MODE) ?: ScheduleHelper.MODE_REGULAR
        val request = OneTimeWorkRequestBuilder<BackupWorker>()
            .setInputData(workDataOf(BackupWorker.KEY_MODE to mode))
            .build()
        WorkManager.getInstance(context).enqueue(request)
        ScheduleHelper.schedule(context)
    }

    companion object {
        const val EXTRA_MODE = "backup_mode"
    }
}
