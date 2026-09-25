package com.example.callvault

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class BackupWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        val token  = BuildConfig.TELEGRAM_BOT_TOKEN
        val chatId = BuildConfig.TELEGRAM_CHAT_ID
        if (token.isBlank() || chatId.isBlank()) {
            NotificationHelper.showResult(applicationContext, 0, "Telegram not configured")
            return Result.failure()
        }
        val store   = CallStore(applicationContext)
        val entries = store.readAll()
        if (entries.isEmpty()) {
            NotificationHelper.showResult(applicationContext, 0, null)
            return Result.success()
        }
        val err = TelegramSender.send(entries, token, chatId, 0L)
        return if (err == null) {
            NotificationHelper.showResult(applicationContext, entries.size, null)
            store.clearAfterBackup()
            Result.success()
        } else {
            NotificationHelper.showResult(applicationContext, 0, err)
            Result.retry()
        }
    }
}
