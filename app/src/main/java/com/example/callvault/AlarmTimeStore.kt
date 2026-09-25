package com.example.callvault

import android.content.Context

class AlarmTimeStore(ctx: Context) {
    private val p = ctx.getSharedPreferences("alarm_time", Context.MODE_PRIVATE)
    fun getHour(): Int   = p.getInt("hour", 5)
    fun getMinute(): Int = p.getInt("minute", 0)
    fun set(hour: Int, minute: Int) = p.edit().putInt("hour", hour).putInt("minute", minute).apply()
    fun label(): String  = "${getHour().pad()}:${getMinute().pad()}"
    private fun Int.pad() = toString().padStart(2, '0')
}
