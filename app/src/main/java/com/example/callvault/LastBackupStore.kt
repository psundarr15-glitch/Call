package com.example.callvault

import android.content.Context

class LastBackupStore(ctx: Context) {
    private val prefs = ctx.getSharedPreferences("backup_meta", Context.MODE_PRIVATE)

    // Returns 0 if never backed up before (send all calls)
    fun getLastBackupTime(): Long = prefs.getLong("last_backup_ts", 0L)

    fun setLastBackupTime(ts: Long) = prefs.edit().putLong("last_backup_ts", ts).apply()
}
