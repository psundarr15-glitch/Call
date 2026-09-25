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

        val allStored = store.readAll()
        val newCalls  = allStored.filter { it.date > lastCheck }

        // Check the CURRENT phone call log against every previously stored call.
        // This catches calls that were backed up earlier and deleted later.
        val systemKeys = CallLogReader(applicationContext).readKeys()

        val deletedKeys = allStored
            .filter { !it.deleted && it.date <= lastCheck }
            .filter { "${it.date}_${it.number}" !in systemKeys }
            .map { "${it.date}_${it.number}" }
            .toSet()

        if (deletedKeys.isNotEmpty()) {
            store.markDeleted(deletedKeys)
        }

        val deletedCalls = allStored
            .filter { "${it.date}_${it.number}" in deletedKeys }
            .map { it.copy(deleted = true) }

        val markedNewCalls = newCalls.map { call ->
            val key = "${call.date}_${call.number}"
            call.copy(deleted = key !in systemKeys)
        }

        // Send new calls plus newly detected deleted calls.
        val toSend = markedNewCalls + deletedCalls

        val err = if (toSend.isEmpty()) {
            TelegramSender.sendNoBackup(token, chatId)
        } else {
            TelegramSender.send(toSend, token, chatId, lastCheck)
        }

        return if (err == null) {
            store.setLastCheckTime(now)
            // IMPORTANT: do not delete old records from local storage.
            // They are needed to detect a later phone-log deletion.
            NotificationHelper.showResult(applicationContext, toSend.size, null)
            Result.success()
        } else {
            NotificationHelper.showResult(applicationContext, 0, err)
            Result.retry()
        }
    }
}
