package com.example.callvault

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

// Manifest-registered receiver — fires on every call state change
// Detects call end → triggers CaptureCallWorker
class CallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        val prefs    = context.getSharedPreferences("phone_state", Context.MODE_PRIVATE)
        val prevState = prefs.getString("state", "IDLE") ?: "IDLE"
        val newState  = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return

        // Save current state for next transition check
        prefs.edit().putString("state", newState).apply()

        // Call just ended: was RINGING (missed/rejected) or OFFHOOK (connected) → now IDLE
        val callJustEnded = newState == "IDLE" && prevState != "IDLE"
        if (!callJustEnded) return

        // Wait 3 seconds for system to write the call log entry, then capture
        val captureWork = OneTimeWorkRequestBuilder<CaptureCallWorker>()
            .setInitialDelay(3, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueue(captureWork)
    }
}
