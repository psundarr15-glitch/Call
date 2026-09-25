package com.example.callvault

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationHelper {

    private const val CH_BACKUP = "jeevi_backup"
    private const val CH_ALARM  = "jeevi_alarm"

    fun createChannels(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm       = ctx.getSystemService(NotificationManager::class.java)
        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val audioAttr = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val vibPat = longArrayOf(0, 500, 300, 500)

        // Backup result channel (normal)
        NotificationChannel(CH_BACKUP, "Backup Results", NotificationManager.IMPORTANCE_DEFAULT)
            .also { nm.createNotificationChannel(it) }

        // Alarm channel (high, with alarm sound + vibration)
        NotificationChannel(CH_ALARM, "Alarms", NotificationManager.IMPORTANCE_HIGH).apply {
            setSound(alarmUri, audioAttr)
            enableVibration(true); vibrationPattern = vibPat
        }.also { nm.createNotificationChannel(it) }
    }

    // Fired when backup completes
    fun showResult(ctx: Context, count: Int, error: String?) {
        val ok = error == null
        build(ctx, CH_BACKUP, 101,
            title = if (ok) "JEEVI — Backup done" else "JEEVI — Backup failed",
            text  = if (ok) "$count calls sent to Telegram" else (error ?: "Unknown error")
        )
    }

    // Fired when user-set alarm rings
    fun showUserAlarm(ctx: Context, time: String) {
        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val nm = ctx.getSystemService(NotificationManager::class.java)
        val n  = androidx.core.app.NotificationCompat.Builder(ctx, CH_ALARM)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("⏰  Alarm — $time")
            .setContentText("JEEVI")
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setSound(alarmUri)
            .setVibrate(longArrayOf(0, 500, 300, 500))
            .setAutoCancel(true)
            .build()
        nm.notify(200 + time.hashCode(), n)
    }

    private fun build(ctx: Context, channel: String, id: Int, title: String, text: String) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        val n  = NotificationCompat.Builder(ctx, channel)
            .setSmallIcon(android.R.drawable.ic_menu_send)
            .setContentTitle(title).setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true).build()
        nm.notify(id, n)
    }
}
