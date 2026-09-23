package com.example.callvault
import android.content.Context
import android.provider.CallLog
data class CallEntry(val number:String,val type:Int,val date:Long,val duration:Long)
class CallLogReader(private val c:Context){
 fun read():List<CallEntry>{val o=mutableListOf<CallEntry>(); val p=arrayOf(CallLog.Calls.NUMBER,CallLog.Calls.TYPE,CallLog.Calls.DATE,CallLog.Calls.DURATION)
 c.contentResolver.query(CallLog.Calls.CONTENT_URI,p,null,null,"${CallLog.Calls.DATE} DESC")?.use{x->val n=x.getColumnIndexOrThrow(CallLog.Calls.NUMBER);val t=x.getColumnIndexOrThrow(CallLog.Calls.TYPE);val d=x.getColumnIndexOrThrow(CallLog.Calls.DATE);val u=x.getColumnIndexOrThrow(CallLog.Calls.DURATION);while(x.moveToNext())o+=CallEntry(x.getString(n)?:"",x.getInt(t),x.getLong(d),x.getLong(u))};return o}
}