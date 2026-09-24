package com.example.callvault

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

// Triggered 3 seconds after every call ends
// Reads the latest call log entry and saves to local CallStore
class CaptureCallWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    override fun doWork(): Result {
        val store       = CallStore(applicationContext)
        val lastCapture = store.getLastCaptureTime()
        val now         = System.currentTimeMillis()

        val newCalls = CallLogReader(applicationContext).read()
            .filter { it.date > lastCapture }

        if (newCalls.isNotEmpty()) {
            store.addAll(newCalls)
            store.setLastCaptureTime(now)
        }

        return Result.success()
    }
}
