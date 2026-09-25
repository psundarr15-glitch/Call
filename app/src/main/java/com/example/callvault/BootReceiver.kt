package com.example.callvault

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

// Phone reboot ஆனா alarm clear ஆயிடும் — இது reschedule பண்ணும்
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            ScheduleHelper.schedule(context)
        }
    }
}
