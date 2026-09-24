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
    private val SDF      = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    private val DATE_FMT = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val BOUNDARY = "CallVaultBoundary${System.currentTimeMillis()}"

    // lastBackup = 0 means first ever backup (all calls)
    fun send(entries: List<CallEntry>, token: String, chatId: String, lastBackup: Long): String? {
        return try {
            val csv      = buildCsv(entries)
            val fileName = "callvault_${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())}.csv"
            val caption  = buildCaption(entries, lastBackup)
            sendDocument(token, chatId, fileName, csv.toByteArray(Charsets.UTF_8), caption)
        } catch (e: Exception) {
            e.message ?: "Unknown error"
        }
    }

    private fun buildCaption(entries: List<CallEntry>, lastBackup: Long): String {
        val range = if (lastBackup == 0L) "All calls"
                    else "Since ${DATE_FMT.format(Date(lastBackup))}"
        return "CallVault Backup - ${entries.size} new entries ($range)"
    }

    private fun buildCsv(entries: List<CallEntry>): String {
        val sb = StringBuilder("Type,Number,Date,Duration(s)\n")
        entries.forEach { e ->
            val type = TYPE_LABEL[e.type] ?: "Unknown"
            val num  = e.number.ifBlank { "Unknown" }.replace(",", " ")
            val date = SDF.format(Date(e.date))
            sb.append("$type,$num,$date,${e.duration}\n")
        }
        return sb.toString()
    }

    private fun sendDocument(
        token: String, chatId: String,
        fileName: String, fileBytes: ByteArray, caption: String
    ): String? {
        val url  = URL("https://api.telegram.org/bot$token/sendDocument")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$BOUNDARY")
        conn.connectTimeout = 20_000
        conn.readTimeout    = 20_000
        conn.doOutput = true

        DataOutputStream(conn.outputStream).use { out ->
            writePart(out, "chat_id", chatId.toByteArray(Charsets.UTF_8))
            writePart(out, "caption", caption.toByteArray(Charsets.UTF_8))
            writeFilePart(out, "document", fileName, fileBytes)
            out.write("--$BOUNDARY--\r\n".toByteArray(Charsets.UTF_8))
            out.flush()
        }

        val code = conn.responseCode
        if (code == 200) return null

        val body = try {
            (conn.errorStream ?: conn.inputStream).bufferedReader().readText()
        } catch (e: Exception) { "(no body)" }
        conn.disconnect()
        return "HTTP $code — $body"
    }

    private fun writePart(out: DataOutputStream, name: String, value: ByteArray) {
        out.write("--$BOUNDARY\r\n".toByteArray(Charsets.UTF_8))
        out.write("Content-Disposition: form-data; name=\"$name\"\r\n\r\n".toByteArray(Charsets.UTF_8))
        out.write(value)
        out.write("\r\n".toByteArray(Charsets.UTF_8))
    }

    private fun writeFilePart(out: DataOutputStream, name: String, fileName: String, fileBytes: ByteArray) {
        out.write("--$BOUNDARY\r\n".toByteArray(Charsets.UTF_8))
        out.write("Content-Disposition: form-data; name=\"$name\"; filename=\"$fileName\"\r\n".toByteArray(Charsets.UTF_8))
        out.write("Content-Type: text/csv\r\n\r\n".toByteArray(Charsets.UTF_8))
        out.write(fileBytes)
        out.write("\r\n".toByteArray(Charsets.UTF_8))
    }
}
