package com.example.callvault

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class BackupHistoryItem(
    val time: Long,
    val mode: String,
    val count: Int,
    val deleted: Int,
    val success: Boolean
)

class BackupHistoryStore(ctx: Context) {
    private val p = ctx.getSharedPreferences("backup_history", Context.MODE_PRIVATE)
    fun add(item: BackupHistoryItem) {
        val old = JSONArray(p.getString("items", "[]"))
        val out = JSONArray()
        out.put(JSONObject().apply {
            put("time", item.time); put("mode", item.mode); put("count", item.count)
            put("deleted", item.deleted); put("success", item.success)
        })
        for (i in 0 until minOf(old.length(), 49)) out.put(old.getJSONObject(i))
        p.edit().putString("items", out.toString()).apply()
    }
    fun all(): List<BackupHistoryItem> {
        val a = JSONArray(p.getString("items", "[]"))
        return (0 until a.length()).map { i ->
            val o = a.getJSONObject(i)
            BackupHistoryItem(o.getLong("time"), o.getString("mode"), o.getInt("count"), o.optInt("deleted", 0), o.optBoolean("success", true))
        }
    }
}
