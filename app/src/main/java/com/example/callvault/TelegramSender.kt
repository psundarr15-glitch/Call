package com.example.callvault

import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

object TelegramSender {

    private val TYPE_LABEL = mapOf(
        1 to "Incoming", 2 to "Outgoing", 3 to "Missed",
        4 to "Voicemail", 5 to "Rejected", 6 to "Blocked"
    )
    private val SDF = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    private val DATE_FMT = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    private val BOUNDARY = "JeeviBoundary${System.currentTimeMillis()}"

    fun send(entries: List<CallEntry>, token: String, chatId: String,
             start: Long, end: Long): String? {
        return try {
            val csv = buildCsv(entries)
            val fileName = "jeevi_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date(end))}.csv"
            val window = "${DATE_FMT.format(Date(start))} to ${DATE_FMT.format(Date(end))}"
            val deleted = entries.count { it.deleted }
            val caption = buildString {
                append("JEEVI Backup | $window | ${entries.size} calls")
                if (deleted > 0) append(" | $deleted DELETED")
            }
            sendDocument(token, chatId, fileName, csv.toByteArray(Charsets.UTF_8), caption)
        } catch (e: Exception) { e.message ?: "Unknown error" }
    }

    fun sendNoBackup(token: String, chatId: String, start: Long, end: Long): String? {
        val window = "${DATE_FMT.format(Date(start))} to ${DATE_FMT.format(Date(end))}"
        return sendText(token, chatId, "JEEVI | $window | No calls in this window")
    }

    private fun buildCsv(entries: List<CallEntry>): String {
        val sb = StringBuilder("Status,Type,Number,Date,Duration(s)\n")
        entries.forEach { e ->
            val status = if (e.deleted) "DELETED 🗑" else "OK"
            val type = TYPE_LABEL[e.type] ?: "Unknown"
            val num = e.number.ifBlank { "Unknown" }.replace(",", " ")
            sb.append("$status,$type,$num,${SDF.format(Date(e.date))},${e.duration}\n")
        }
        return sb.toString()
    }

    private fun sendText(token: String, chatId: String, text: String): String? {
        return try {
            val url = URL("https://api.telegram.org/bot$token/sendMessage")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.connectTimeout = 15_000; conn.readTimeout = 15_000; conn.doOutput = true
            val escaped = text.replace("\\", "\\\\").replace("\"", "\\\"")
            DataOutputStream(conn.outputStream).use {
                it.write("{\"chat_id\":\"$chatId\",\"text\":\"$escaped\"}".toByteArray(Charsets.UTF_8))
            }
            if (conn.responseCode == 200) null else "HTTP ${conn.responseCode}"
        } catch (e: Exception) { e.message }
    }

    private fun sendDocument(token: String, chatId: String, fileName: String,
                             fileBytes: ByteArray, caption: String): String? {
        return try {
            val url = URL("https://api.telegram.org/bot$token/sendDocument")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$BOUNDARY")
            conn.connectTimeout = 20_000; conn.readTimeout = 20_000; conn.doOutput = true
            DataOutputStream(conn.outputStream).use { out ->
                writePart(out, "chat_id", chatId.toByteArray(Charsets.UTF_8))
                writePart(out, "caption", caption.toByteArray(Charsets.UTF_8))
                writeFilePart(out, "document", fileName, fileBytes)
                out.write("--$BOUNDARY--\r\n".toByteArray(Charsets.UTF_8)); out.flush()
            }
            val code = conn.responseCode
            if (code == 200) null else {
                val body = try { (conn.errorStream ?: conn.inputStream).bufferedReader().readText() }
                           catch (_: Exception) { "" }
                "HTTP $code — $body"
            }
        } catch (e: Exception) { e.message }
    }

    private fun writePart(out: DataOutputStream, name: String, value: ByteArray) {
        out.write("--$BOUNDARY\r\n".toByteArray(Charsets.UTF_8))
        out.write("Content-Disposition: form-data; name=\"$name\"\r\n\r\n".toByteArray(Charsets.UTF_8))
        out.write(value); out.write("\r\n".toByteArray(Charsets.UTF_8))
    }

    private fun writeFilePart(out: DataOutputStream, name: String, fileName: String, bytes: ByteArray) {
        out.write("--$BOUNDARY\r\n".toByteArray(Charsets.UTF_8))
        out.write("Content-Disposition: form-data; name=\"$name\"; filename=\"$fileName\"\r\n".toByteArray(Charsets.UTF_8))
        out.write("Content-Type: text/csv\r\n\r\n".toByteArray(Charsets.UTF_8))
        out.write(bytes); out.write("\r\n".toByteArray(Charsets.UTF_8))
    }
}
