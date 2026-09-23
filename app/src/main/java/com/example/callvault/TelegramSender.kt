package com.example.callvault

import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

object TelegramSender {

    private val TYPE_LABEL = mapOf(
        1 to "IN", 2 to "OUT", 3 to "MISSED",
        4 to "VOICEMAIL", 5 to "REJECTED", 6 to "BLOCKED"
    )
    private val SDF = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())

    // Bug 3 fix: network call must never run on main thread — caller wraps in Thread{}
    fun send(entries: List<CallEntry>, token: String, chatId: String): Boolean {
        val message = buildMessage(entries)
        // Bug 7 fix: Telegram max message = 4096 chars, chunk if needed
        val chunks = message.chunked(4000)
        return chunks.all { postMessage(token, chatId, it) }
    }

    private fun buildMessage(entries: List<CallEntry>): String {
        val sb = StringBuilder("📞 CallVault Backup — ${entries.size} entries\n\n")
        entries.forEach { e ->
            val label = TYPE_LABEL[e.type] ?: "?"
            val num = e.number.ifBlank { "Unknown" }
            val date = SDF.format(Date(e.date))
            val dur = if (e.duration > 0) " (${e.duration}s)" else ""
            sb.append("[$label] $num | $date$dur\n")
        }
        return sb.toString()
    }

    private fun postMessage(token: String, chatId: String, text: String): Boolean {
        return try {
            val url = URL("https://api.telegram.org/bot$token/sendMessage")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.connectTimeout = 15_000
            conn.readTimeout = 15_000
            conn.doOutput = true
            val json = buildJson(chatId, text)
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(json) }
            val code = conn.responseCode
            conn.disconnect()
            code == 200
        } catch (e: Exception) {
            false
        }
    }

    private fun buildJson(chatId: String, text: String): String {
        val escaped = text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
        return """{"chat_id":"$chatId","text":"$escaped"}"""
    }
}
