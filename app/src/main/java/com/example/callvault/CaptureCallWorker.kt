package com.example.callvault

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class CaptureCallWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    override fun doWork(): Result {
        val store       = CallStore(applicationContext)
        val lastCapture = store.getLastCheckTime()   // Fix: was getLastCaptureTime
        val now         = System.currentTimeMillis()

        val newCalls = CallLogReader(applicationContext).read()
            .filter { it.date > lastCapture }

        if (newCalls.isNotEmpty()) {
            store.addAll(newCalls)
            store.setLastCheckTime(now)              // Fix: was setLastCaptureTime
        }

        return Result.success()
    }
}
