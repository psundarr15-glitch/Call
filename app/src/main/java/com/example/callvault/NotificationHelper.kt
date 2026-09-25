package com.example.callvault

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationHelper {

    private const val CH_ALARM  = "jeevi_alarm"
    private const val CH_RESULT = "jeevi_result"

    fun createChannels(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = ctx.getSystemService(NotificationManager::class.java)

        // Alarm channel — high priority, default alarm sound
        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val audioAttr = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        NotificationChannel(CH_ALARM, "Backup Alarm", NotificationManager.IMPORTANCE_HIGH).apply {
            description   = "Fires when backup starts"
            setSound(alarmUri, audioAttr)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 400, 200, 400)
        }.also { nm.createNotificationChannel(it) }

        // Result channel — normal priority
        NotificationChannel(CH_RESULT, "Backup Result", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Shows backup success/failure"
        }.also { nm.createNotificationChannel(it) }
    }

    // "Alarm ringing" notification — fires at the set time
    fun showAlarm(ctx: Context, time: String) {
        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val n = NotificationCompat.Builder(ctx, CH_ALARM)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("JEEVI — Backup Running")
            .setContentText("Sending call log to Telegram ($time)")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(alarmUri)
            .setVibrate(longArrayOf(0, 400, 200, 400))
            .setAutoCancel(true)
            .build()
        ctx.getSystemService(NotificationManager::class.java).notify(101, n)
    }

    // Result notification after backup completes
    fun showResult(ctx: Context, count: Int, error: String?) {
        val ok = error == null
        val n = NotificationCompat.Builder(ctx, CH_RESULT)
            .setSmallIcon(if (ok) android.R.drawable.ic_menu_send else android.R.drawable.ic_dialog_alert)
            .setContentTitle(if (ok) "JEEVI — Backup Complete" else "JEEVI — Backup Failed")
            .setContentText(if (ok) "$count calls backed up to Telegram" else error)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        ctx.getSystemService(NotificationManager::class.java).notify(102, n)
    }
}
