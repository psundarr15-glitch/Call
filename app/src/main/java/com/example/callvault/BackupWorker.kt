package com.example.callvault

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.Calendar

class BackupWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    override fun doWork(): Result {
        val token = BuildConfig.TELEGRAM_BOT_TOKEN
        val chatId = BuildConfig.TELEGRAM_CHAT_ID
        if (token.isBlank() || chatId.isBlank()) return Result.failure()

        val store = CallStore(applicationContext)
        val now = System.currentTimeMillis()
        val mode = inputData.getString(KEY_MODE) ?: ScheduleHelper.MODE_REGULAR
        val systemKeys = CallLogReader(applicationContext).readKeys()

        val result = when (mode) {
            ScheduleHelper.MODE_DAILY_FULL -> sendDailyFull(store, token, chatId, now, systemKeys)
            ScheduleHelper.MODE_WEEKLY_FULL -> sendWeeklyFull(store, token, chatId, now, systemKeys)
            else -> sendRegular(store, token, chatId, now, systemKeys)
        }

        if (result.first == null) {
            BackupStatsStore(applicationContext).recordSent(now, result.second)
            NotificationHelper.showResult(applicationContext, result.second, null)
            return Result.success()
        }

        NotificationHelper.showResult(applicationContext, 0, result.first)
        return Result.retry()
    }

    private fun sendRegular(store: CallStore, token: String, chatId: String, now: Long, systemKeys: Set<String>): Pair<String?, Int> {
        val prefs = applicationContext.getSharedPreferences("backup_window", Context.MODE_PRIVATE)
        val cursor = prefs.getLong("incremental_cursor_ts", prefs.getLong("last_backup_ts", 0L))
        val start = if (cursor > 0L) cursor else previousRegularSlot(now)
        val entries = store.readAll().filter { it.date >= start && it.date < now }.map { markDeleted(it, systemKeys) }
        val err = if (entries.isEmpty()) TelegramSender.sendNoBackup(token, chatId, start, now, "4-HOUR")
                  else TelegramSender.send(entries, token, chatId, start, now, "4-HOUR")
        if (err == null) prefs.edit().putLong("incremental_cursor_ts", now).putLong("last_backup_ts", now).apply()
        return err to entries.size
    }

    private fun sendDailyFull(store: CallStore, token: String, chatId: String, now: Long, systemKeys: Set<String>): Pair<String?, Int> {
        val start = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val entries = store.readAll().filter { it.date >= start && it.date <= now }.map { markDeleted(it, systemKeys) }
        val err = TelegramSender.sendFull(entries, token, chatId, start, now, "DAILY FULL")
        return err to entries.size
    }

    private fun sendWeeklyFull(store: CallStore, token: String, chatId: String, now: Long, systemKeys: Set<String>): Pair<String?, Int> {
        val entries = store.readAll().map { markDeleted(it, systemKeys) }
        val err = TelegramSender.sendFull(entries, token, chatId, 0L, now, "WEEKLY FULL")
        return err to entries.size
    }

    private fun markDeleted(call: CallEntry, systemKeys: Set<String>): CallEntry {
        val key = "${call.date}_${call.number}"
        return if (!call.deleted && key !in systemKeys) call.copy(deleted = true) else call
    }

    private fun previousRegularSlot(now: Long): Long {
        val c = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val hour = c.get(Calendar.HOUR_OF_DAY)
        val slot = (hour / 4) * 4
        c.set(Calendar.HOUR_OF_DAY, slot)
        if (c.timeInMillis >= now) c.add(Calendar.HOUR_OF_DAY, -4)
        return c.timeInMillis
    }

    companion object {
        const val KEY_MODE = "backup_mode"
    }
}
