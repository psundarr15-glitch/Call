package com.example.callvault
import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
class SecurePinStore(c:Context){
 private val p=c.getSharedPreferences("pin",0)
 fun hasPin()=p.contains("hash")
 fun setPin(pin:String){val s=ByteArray(16).also{SecureRandom().nextBytes(it)}; val h=hash(s+pin.toByteArray()); p.edit().putString("s",Base64.encodeToString(s,0)).putString("hash",Base64.encodeToString(h,0)).apply()}
 fun verify(pin:String):Boolean{val s=p.getString("s",null)?:return false; val h=p.getString("hash",null)?:return false; return MessageDigest.isEqual(hash(Base64.decode(s,0)+pin.toByteArray()),Base64.decode(h,0))}
 private fun hash(b:ByteArray)=MessageDigest.getInstance("SHA-256").digest(b)
}