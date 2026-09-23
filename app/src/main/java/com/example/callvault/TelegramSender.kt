package com.example.callvault

import java.io.BufferedReader
import java.io.InputStreamReader
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

    // Returns null on success, error string on failure
    fun send(entries: List<CallEntry>, token: String, chatId: String): String? {
        val chunks = buildMessage(entries).chunked(4000)
        for (chunk in chunks) {
            val err = postMessage(token, chatId, chunk)
            if (err != null) return err
        }
        return null
    }

    private fun buildMessage(entries: List<CallEntry>): String {
        val sb = StringBuilder("📞 CallVault Backup — ${entries.size} entries\n\n")
        entries.forEach { e ->
            val label = TYPE_LABEL[e.type] ?: "?"
            val num   = e.number.ifBlank { "Unknown" }
            val date  = SDF.format(Date(e.date))
            val dur   = if (e.duration > 0) " (${e.duration}s)" else ""
            sb.append("[$label] $num | $date$dur\n")
        }
        return sb.toString()
    }

    // Returns null on success, error description on failure
    private fun postMessage(token: String, chatId: String, text: String): String? {
        return try {
            val url  = URL("https://api.telegram.org/bot$token/sendMessage")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.connectTimeout = 15_000
            conn.readTimeout    = 15_000
            conn.doOutput = true

            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use {
                it.write(buildJson(chatId, text))
            }

            val code = conn.responseCode
            if (code == 200) return null  // success

            // Read Telegram's error body so we can show exactly what's wrong
            val body = try {
                val stream = conn.errorStream ?: conn.inputStream
                BufferedReader(InputStreamReader(stream)).readText()
            } catch (e: Exception) { "(no body)" }

            conn.disconnect()
            "HTTP $code — $body"
        } catch (e: Exception) {
            e.message ?: "Unknown exception"
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
