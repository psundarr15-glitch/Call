package com.example.callvault

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class CaptureCallWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    override fun doWork(): Result {
        // SEPARATE prefs from BackupWorker — they must NOT share the same timestamp
        // Bug was: sharing lastCheckTime caused BackupWorker to miss already-captured calls
        val capturePrefs = applicationContext
            .getSharedPreferences("capture_meta", Context.MODE_PRIVATE)
        val lastCapture = capturePrefs.getLong("last_capture_ts", 0L)
        val now         = System.currentTimeMillis()

        val newCalls = CallLogReader(applicationContext).read()
            .filter { it.date > lastCapture }

        if (newCalls.isNotEmpty()) {
            CallStore(applicationContext).addAll(newCalls)
            capturePrefs.edit().putLong("last_capture_ts", now).apply()
        }

        return Result.success()
    }
}
