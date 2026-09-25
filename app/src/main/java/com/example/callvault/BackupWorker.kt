package com.example.callvault

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class BackupWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    override fun doWork(): Result {
        val token  = BuildConfig.TELEGRAM_BOT_TOKEN
        val chatId = BuildConfig.TELEGRAM_CHAT_ID
        if (token.isBlank() || chatId.isBlank()) return Result.failure()

        val store     = CallStore(applicationContext)
        val lastCheck = store.getLastCheckTime()
        val now       = System.currentTimeMillis()

        val newCalls = store.readSince(lastCheck)

        val err = if (newCalls.isEmpty()) {
            TelegramSender.sendNoBackup(token, chatId)
        } else {
            // Compare local store with current system call log
            // Any call NOT in system log = user deleted it → mark with 🗑 symbol
            val systemKeys = CallLogReader(applicationContext).readKeys()
            val marked = newCalls.map { call ->
                val key = "${call.date}_${call.number}"
                call.copy(deleted = key !in systemKeys)
            }
            TelegramSender.send(marked, token, chatId, lastCheck)
        }

        return if (err == null) {
            store.setLastCheckTime(now)
            if (newCalls.isNotEmpty()) store.clearBefore(now)
            NotificationHelper.showResult(applicationContext, newCalls.size, null)
            Result.success()
        } else {
            NotificationHelper.showResult(applicationContext, 0, err)
            Result.retry()
        }
    }
}
