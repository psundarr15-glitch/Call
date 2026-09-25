package com.example.callvault

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    override fun doWork(): Result {
        val token  = BuildConfig.TELEGRAM_BOT_TOKEN
        val chatId = BuildConfig.TELEGRAM_CHAT_ID
        if (token.isBlank() || chatId.isBlank()) return Result.failure()

        val store = CallStore(applicationContext)
        val now = System.currentTimeMillis()
        val prefs = applicationContext.getSharedPreferences("backup_window", Context.MODE_PRIVATE)

        // Every backup owns one time window. Nothing outside this window is sent.
        val savedStart = prefs.getLong("last_backup_ts", 0L)
        val start = if (savedStart > 0L) savedStart else java.util.Calendar.getInstance().apply {
            timeInMillis = now
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        val end = now

        // Calls are captured continuously after each call. Select ONLY calls whose
        // call-log timestamp belongs to this backup window.
        val windowCalls = store.readAll()
            .filter { it.date >= start && it.date < end }
            .toMutableList()

        // If a call from this same window has been deleted from the phone before
        // the hourly backup, keep it in the backup and mark it DELETED.
        val systemKeys = CallLogReader(applicationContext).readKeys()
        val marked = windowCalls.map { call ->
            val key = "${call.date}_${call.number}"
            if (!call.deleted && key !in systemKeys) call.copy(deleted = true) else call
        }

        val err = if (marked.isEmpty()) {
            TelegramSender.sendNoBackup(token, chatId, start, end)
        } else {
            TelegramSender.send(marked, token, chatId, start, end)
        }

        return if (err == null) {
            // Advance the window only after Telegram confirms success.
            prefs.edit().putLong("last_backup_ts", end).apply()
            BackupStatsStore(applicationContext).recordSent(end, marked.size)
            NotificationHelper.showResult(applicationContext, marked.size, null)
            Result.success()
        } else {
            NotificationHelper.showResult(applicationContext, 0, err)
            Result.retry()
        }
    }
}
