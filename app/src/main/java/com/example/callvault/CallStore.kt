package com.example.callvault

import android.content.Context
import java.io.File

// Local store — call log entries saved here immediately after each call
// BackupWorker reads from here, NOT from system call log
class CallStore(ctx: Context) {

    private val file  = File(ctx.filesDir, "call_store.csv")
    private val prefs = ctx.getSharedPreferences("call_store_meta", Context.MODE_PRIVATE)

    fun addAll(entries: List<CallEntry>) {
        if (entries.isEmpty()) return
        val lines = entries.joinToString("\n") { e ->
            "${e.number}|${e.type}|${e.date}|${e.duration}"
        }
        file.appendText(if (file.exists() && file.length() > 0) "\n$lines" else lines)
    }

    fun readAll(): List<CallEntry> {
        if (!file.exists()) return emptyList()
        return file.readLines()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val p = line.split("|")
                if (p.size == 4) CallEntry(p[0], p[1].toInt(), p[2].toLong(), p[3].toLong())
                else null
            }
    }

    fun clearAfterBackup() = file.delete()

    // Track last capture time so CaptureCallWorker knows what's new
    fun getLastCaptureTime(): Long = prefs.getLong("last_capture_ts", 0L)
    fun setLastCaptureTime(ts: Long) = prefs.edit().putLong("last_capture_ts", ts).apply()
}
