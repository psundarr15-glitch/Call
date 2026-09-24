package com.example.callvault

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class BackupWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    override fun doWork(): Result {
        val token  = BuildConfig.TELEGRAM_BOT_TOKEN
        val chatId = BuildConfig.TELEGRAM_CHAT_ID
        if (token.isBlank() || chatId.isBlank()) return Result.failure()

        val store       = LastBackupStore(applicationContext)
        val lastBackup  = store.getLastBackupTime()
        val backupStart = System.currentTimeMillis()

        // Only fetch calls that happened AFTER the last backup
        val allEntries  = CallLogReader(applicationContext).read()
        val newEntries  = if (lastBackup == 0L) allEntries
                          else allEntries.filter { it.date > lastBackup }

        if (newEntries.isEmpty()) return Result.success() // nothing new, skip

        val err = TelegramSender.send(newEntries, token, chatId, lastBackup)
        if (err != null) return Result.retry()

        // Save timestamp only after successful send
        store.setLastBackupTime(backupStart)
        return Result.success()
    }
}
