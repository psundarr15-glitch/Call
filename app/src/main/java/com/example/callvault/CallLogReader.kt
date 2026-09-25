package com.example.callvault

import android.content.Context
import android.provider.CallLog

// deleted = true means the call was in our local store but no longer in system call log
data class CallEntry(
    val number: String,
    val type: Int,
    val date: Long,
    val duration: Long,
    val deleted: Boolean = false
)

class CallLogReader(private val c: Context) {
    fun read(): List<CallEntry> {
        val o = mutableListOf<CallEntry>()
        val p = arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.TYPE, CallLog.Calls.DATE, CallLog.Calls.DURATION)
        c.contentResolver.query(
            CallLog.Calls.CONTENT_URI, p, null, null, "${CallLog.Calls.DATE} DESC"
        )?.use { x ->
            val n = x.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
            val t = x.getColumnIndexOrThrow(CallLog.Calls.TYPE)
            val d = x.getColumnIndexOrThrow(CallLog.Calls.DATE)
            val u = x.getColumnIndexOrThrow(CallLog.Calls.DURATION)
            while (x.moveToNext())
                o += CallEntry(x.getString(n) ?: "", x.getInt(t), x.getLong(d), x.getLong(u))
        }
        return o
    }

    // Returns a set of "date_number" keys for fast lookup
    fun readKeys(): Set<String> = read().map { "${it.date}_${it.number}" }.toSet()
}
