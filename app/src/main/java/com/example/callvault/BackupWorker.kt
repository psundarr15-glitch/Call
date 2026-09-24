package com.example.callvault

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

// Runs daily at 5 AM — reads from LOCAL store (not system call log)
// So even if user deletes call history, backup still has it
class BackupWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    override fun doWork(): Result {
        val token  = BuildConfig.TELEGRAM_BOT_TOKEN
        val chatId = BuildConfig.TELEGRAM_CHAT_ID
        if (token.isBlank() || chatId.isBlank()) return Result.failure()

        val store   = CallStore(applicationContext)
        val entries = store.readAll()

        if (entries.isEmpty()) return Result.success() // nothing to send

        val err = TelegramSender.send(entries, token, chatId, 0L)
        if (err != null) return Result.retry()

        // Clear local store only after successful send
        store.clearAfterBackup()
        return Result.success()
    }
}
