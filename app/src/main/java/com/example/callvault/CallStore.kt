package com.example.callvault

import android.content.Context
import java.io.File

class CallStore(ctx: Context) {
    private val file  = File(ctx.filesDir, "call_store.csv")
    private val prefs = ctx.getSharedPreferences("call_store_meta", Context.MODE_PRIVATE)

    fun addAll(entries: List<CallEntry>) {
        if (entries.isEmpty()) return

        val existingKeys = readAll().map { key(it) }.toHashSet()
        val newLines = entries
            .filter { key(it) !in existingKeys }
            .joinToString("\n") {
                "${it.number}|${it.type}|${it.date}|${it.duration}|${if (it.deleted) 1 else 0}"
            }

        if (newLines.isNotEmpty()) {
            file.appendText(if (file.exists() && file.length() > 0) "\n$newLines" else newLines)
        }
    }

    fun readSince(ts: Long): List<CallEntry> =
        readAll().filter { it.date > ts }

    fun readAll(): List<CallEntry> {
        if (!file.exists()) return emptyList()

        return file.readLines().filter { it.isNotBlank() }.mapNotNull { line ->
            val p = line.split("|")
            try {
                when (p.size) {
                    4 -> CallEntry(p[0], p[1].toInt(), p[2].toLong(), p[3].toLong(), false)
                    5 -> CallEntry(p[0], p[1].toInt(), p[2].toLong(), p[3].toLong(), p[4] == "1")
                    else -> null
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    fun markDeleted(keys: Set<String>) {
        if (keys.isEmpty() || !file.exists()) return

        val updated = readAll().map { e ->
            if (key(e) in keys) e.copy(deleted = true) else e
        }

        file.writeText(updated.joinToString("\n") {
            "${it.number}|${it.type}|${it.date}|${it.duration}|${if (it.deleted) 1 else 0}"
        })
    }

    private fun key(e: CallEntry): String = "${e.date}_${e.number}"

    // Kept for compatibility with older code. Deleted/history records are
    // intentionally retained so later phone-log deletions can be detected.
    fun clearBefore(sentUpTo: Long) = Unit

    fun getLastCheckTime(): Long = prefs.getLong("last_check_ts", 0L)
    fun setLastCheckTime(ts: Long) = prefs.edit().putLong("last_check_ts", ts).apply()
}
