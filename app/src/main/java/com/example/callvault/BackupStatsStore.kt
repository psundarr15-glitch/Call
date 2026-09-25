package com.example.callvault

import android.content.Context

class BackupStatsStore(ctx: Context) {
    private val p = ctx.getSharedPreferences("backup_stats", Context.MODE_PRIVATE)

    fun lastSentAt(): Long = p.getLong("last_sent_at", 0L)
    fun lastSentCount(): Int = p.getInt("last_sent_count", 0)
    fun totalSent(): Long = p.getLong("total_sent", 0L)
    fun totalBackups(): Long = p.getLong("total_backups", 0L)

    fun recordSent(at: Long, count: Int) {
        p.edit()
            .putLong("last_sent_at", at)
            .putInt("last_sent_count", count)
            .putLong("total_sent", totalSent() + count)
            .putLong("total_backups", totalBackups() + 1)
            .apply()
    }
}
