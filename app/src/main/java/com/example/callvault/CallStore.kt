package com.example.callvault

import android.content.Context
import java.io.File

class CallStore(ctx: Context) {
    private val file  = File(ctx.filesDir, "call_store.csv")
    private val prefs = ctx.getSharedPreferences("call_store_meta", Context.MODE_PRIVATE)

    fun addAll(entries: List<CallEntry>) {
        if (entries.isEmpty()) return
        val lines = entries.joinToString("\n") { "${it.number}|${it.type}|${it.date}|${it.duration}" }
        file.appendText(if (file.exists() && file.length() > 0) "\n$lines" else lines)
    }

    fun readSince(ts: Long): List<CallEntry> =
        readAll().filter { it.date > ts }

    fun readAll(): List<CallEntry> {
        if (!file.exists()) return emptyList()
        return file.readLines().filter { it.isNotBlank() }.mapNotNull { line ->
            val p = line.split("|")
            if (p.size == 4) CallEntry(p[0], p[1].toInt(), p[2].toLong(), p[3].toLong()) else null
        }
    }

    // Remove entries already sent (date <= sentUpTo), keep newer ones
    fun clearBefore(sentUpTo: Long) {
        val remaining = readAll().filter { it.date > sentUpTo }
        if (remaining.isEmpty()) file.delete()
        else file.writeText(remaining.joinToString("\n") {
            "${it.number}|${it.type}|${it.date}|${it.duration}"
        })
    }

    fun getLastCheckTime(): Long = prefs.getLong("last_check_ts", 0L)
    fun setLastCheckTime(ts: Long) = prefs.edit().putLong("last_check_ts", ts).apply()
}
