package com.example.callvault

import androidx.work.Worker
import androidx.work.WorkerParameters

/**
 * Runs the backup requested by AlarmReceiver.
 *
 * REGULAR     = incremental 4-hour backup
 * DAILY_FULL  = complete saved call archive
 * WEEKLY_FULL = complete saved call archive
 */
class BackupWorker(ctx: android.content.Context, params: WorkerParameters) : Worker(ctx, params) {

    override fun doWork(): Result {
        val token = BuildConfig.TELEGRAM_BOT_TOKEN
        val chatId = BuildConfig.TELEGRAM_CHAT_ID
        if (token.isBlank() || chatId.isBlank()) return Result.failure()

        val store = CallStore(applicationContext)
        val now = System.currentTimeMillis()
        val lastSent = store.getLastCheckTime()
        val mode = inputData.getString(KEY_MODE) ?: ScheduleHelper.MODE_REGULAR

        val (err, sentCount) = when (mode) {
            ScheduleHelper.MODE_DAILY_FULL -> {
                val calls = store.readAll()
                TelegramSender.sendFull(
                    calls, token, chatId, lastSent, now,
                    ScheduleHelper.modeLabel(mode)
                ) to calls.size
            }

            ScheduleHelper.MODE_WEEKLY_FULL -> {
                val calls = store.readAll()
                TelegramSender.sendFull(
                    calls, token, chatId, lastSent, now,
                    ScheduleHelper.modeLabel(mode)
                ) to calls.size
            }

            else -> {
                val calls = store.readAll().filter { it.date > lastSent && it.date <= now }
                val error = if (calls.isEmpty()) {
                    TelegramSender.sendNoBackup(
                        token, chatId, lastSent, now,
                        ScheduleHelper.modeLabel(ScheduleHelper.MODE_REGULAR)
                    )
                } else {
                    TelegramSender.send(
                        calls, token, chatId, lastSent, now,
                        ScheduleHelper.modeLabel(ScheduleHelper.MODE_REGULAR)
                    )
                }
                error to calls.size
            }
        }

        return if (err == null) {
            store.setLastCheckTime(now)
            NotificationHelper.showResult(applicationContext, sentCount, null)
            Result.success()
        } else {
            NotificationHelper.showResult(applicationContext, 0, err)
            Result.retry()
        }
    }

    companion object {
        const val KEY_MODE = "backup_mode"
    }
}
