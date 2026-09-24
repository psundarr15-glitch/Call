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
    private val BOUNDARY = "----CallVaultBoundary${System.currentTimeMillis()}"

    // Returns null on success, error string on failure
    fun send(entries: List<CallEntry>, token: String, chatId: String): String? {
        return try {
            val csvBytes = buildCsv(entries).toByteArray(Charsets.UTF_8)
            val fileName = "callvault_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.csv"
            sendDocument(token, chatId, fileName, csvBytes)
        } catch (e: Exception) {
            e.message ?: "Unknown error"
        }
    }

    private fun buildCsv(entries: List<CallEntry>): String {
        val sb = StringBuilder()
        sb.append("Type,Number,Date,Duration(s)\n")
        entries.forEach { e ->
            val type = TYPE_LABEL[e.type] ?: "Unknown"
            val num  = e.number.ifBlank { "Unknown" }.replace(",", " ")
            val date = SDF.format(Date(e.date))
            sb.append("$type,$num,$date,${e.duration}\n")
        }
        return sb.toString()
    }

    // Send as file using multipart/form-data → Telegram sendDocument
    private fun sendDocument(
        token: String, chatId: String,
        fileName: String, fileBytes: ByteArray
    ): String? {
        val url  = URL("https://api.telegram.org/bot$token/sendDocument")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$BOUNDARY")
        conn.connectTimeout = 20_000
        conn.readTimeout    = 20_000
        conn.doOutput = true

        DataOutputStream(conn.outputStream).use { out ->
            // chat_id field
            out.writeBytes("--$BOUNDARY\r\n")
            out.writeBytes("Content-Disposition: form-data; name=\"chat_id\"\r\n\r\n")
            out.writeBytes("$chatId\r\n")

            // caption field
            out.writeBytes("--$BOUNDARY\r\n")
            out.writeBytes("Content-Disposition: form-data; name=\"caption\"\r\n\r\n")
            out.writeBytes("📞 CallVault Backup — ${fileBytes.toString(Charsets.UTF_8).lines().size - 2} entries\r\n")

            // document file field
            out.writeBytes("--$BOUNDARY\r\n")
            out.writeBytes("Content-Disposition: form-data; name=\"document\"; filename=\"$fileName\"\r\n")
            out.writeBytes("Content-Type: text/csv\r\n\r\n")
            out.write(fileBytes)
            out.writeBytes("\r\n")

            // closing boundary
            out.writeBytes("--$BOUNDARY--\r\n")
            out.flush()
        }

        val code = conn.responseCode
        if (code == 200) return null  // success

        val body = try {
            (conn.errorStream ?: conn.inputStream).bufferedReader().readText()
        } catch (e: Exception) { "(no body)" }

        conn.disconnect()
        return "HTTP $code — $body"
    }
}
