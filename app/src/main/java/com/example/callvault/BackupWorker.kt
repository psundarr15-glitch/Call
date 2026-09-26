package com.example.callvault

import androidx.work.Worker
import androidx.work.WorkerParameters

/**
 * Sends one COMPLETE call archive every 4 hours.
 * A full backup always contains every call currently stored by the app,
 * including records marked as deleted.
 */
class BackupWorker(ctx: android.content.Context, params: WorkerParameters) : Worker(ctx, params) {

    override fun doWork(): Result {
        val token = BuildConfig.TELEGRAM_BOT_TOKEN
        val chatId = BuildConfig.TELEGRAM_CHAT_ID
        if (token.isBlank() || chatId.isBlank()) return Result.failure()

        val store = CallStore(applicationContext)
        val calls = store.readAll()
        val now = System.currentTimeMillis()

        // Full backup: send ALL stored calls, not only calls from the last 4 hours.
        val error = TelegramSender.sendFull(
            entries = calls,
            token = token,
            chatId = chatId,
            start = 0L,
            end = now,
            schedule = ScheduleHelper.modeLabel()
        )

        return if (error == null) {
            store.setLastCheckTime(now)
            NotificationHelper.showResult(applicationContext, calls.size, null)
            Result.success()
        } else {
            NotificationHelper.showResult(applicationContext, 0, error)
            Result.retry()
        }
    }

    companion object {
        const val KEY_MODE = "backup_mode"
    }
}
