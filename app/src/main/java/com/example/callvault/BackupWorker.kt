package com.example.callvault

import androidx.work.Worker
import androidx.work.WorkerParameters

/**
 * Rolling full backup:
 * - A backup is due 4 hours after the previous successful backup.
 * - It is NOT tied to fixed clock slots.
 * - First run starts from the last stored checkpoint; if none exists,
 *   it backs up the available calls and establishes the checkpoint.
 */
class BackupWorker(ctx: android.content.Context, params: WorkerParameters) : Worker(ctx, params) {

    override fun doWork(): Result {
        val token = BuildConfig.TELEGRAM_BOT_TOKEN
        val chatId = BuildConfig.TELEGRAM_CHAT_ID
        if (token.isBlank() || chatId.isBlank()) return Result.failure()

        val store = CallStore(applicationContext)
        val now = System.currentTimeMillis()
        val lastSent = store.getLastCheckTime()

        // Only calls since the previous successful backup.
        val calls = store.readAll().filter { it.date > lastSent && it.date <= now }

        val err = if (calls.isEmpty()) {
            TelegramSender.sendNoBackup(token, chatId)
        } else {
            TelegramSender.send(calls, token, chatId, lastSent)
        }

        return if (err == null) {
            // Move the checkpoint only after a successful Telegram send.
            store.setLastCheckTime(now)
            NotificationHelper.showResult(applicationContext, calls.size, null)
            Result.success()
        } else {
            NotificationHelper.showResult(applicationContext, 0, err)
            Result.retry()
        }
    }
}
