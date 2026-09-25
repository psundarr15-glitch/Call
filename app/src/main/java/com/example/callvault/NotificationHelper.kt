package com.example.callvault

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationHelper {

    private const val CH_BACKUP = "jeevi_backup"

    fun createChannels(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = ctx.getSystemService(NotificationManager::class.java)
        NotificationChannel(CH_BACKUP, "Backup Results", NotificationManager.IMPORTANCE_DEFAULT)
            .also { nm.createNotificationChannel(it) }
    }

    fun showResult(ctx: Context, count: Int, error: String?) {
        val ok = error == null
        val nm = ctx.getSystemService(NotificationManager::class.java)
        val n  = NotificationCompat.Builder(ctx, CH_BACKUP)
            .setSmallIcon(android.R.drawable.ic_menu_send)
            .setContentTitle(if (ok) "JEEVI — Backup done" else "JEEVI — Backup failed")
            .setContentText(if (ok) "$count calls sent to Telegram" else (error ?: "Unknown error"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        nm.notify(101, n)
    }
}
