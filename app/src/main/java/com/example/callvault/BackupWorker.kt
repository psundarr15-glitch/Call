package com.example.callvault

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class BackupWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        val token  = BuildConfig.TELEGRAM_BOT_TOKEN
        val chatId = BuildConfig.TELEGRAM_CHAT_ID
        if (token.isBlank() || chatId.isBlank()) return Result.failure()
        val entries = CallLogReader(applicationContext).read()
        if (entries.isEmpty()) return Result.success()
        val err = TelegramSender.send(entries, token, chatId)
        return if (err == null) Result.success() else Result.retry()
    }
}
