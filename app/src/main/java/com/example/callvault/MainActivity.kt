package com.example.callvault

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private lateinit var pin: SecurePinStore
    private var unlocked = false
    private var last = 0L

    private val BG = Color.parseColor("#07111F")
    private val SURFACE = Color.parseColor("#0D1B2A")
    private val CARD = Color.parseColor("#10243A")
    private val ACCENT = Color.parseColor("#35E0C2")
    private val ACCENT2 = Color.parseColor("#62A8FF")
    private val WHITE = Color.parseColor("#F4F8FC")
    private val MUTED = Color.parseColor("#7F95AA")
    private val RED = Color.parseColor("#FF647C")
    private val pinBuffer = StringBuilder()
    private val dots = mutableListOf<View>()

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.statusBarColor = BG
        window.navigationBarColor = BG
        NotificationHelper.createChannels(this)
        pin = SecurePinStore(this)
        if (pin.hasPin()) showPinEnter() else showPinCreate()
    }

    private fun root(center: Boolean = true) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = if (center) Gravity.CENTER else Gravity.TOP or Gravity.CENTER_HORIZONTAL
        setBackgroundColor(BG)
        layoutParams = LinearLayout.LayoutParams(-1, -1)
    }

    private fun logo(sub: String = "") = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        addView(ImageView(this@MainActivity).apply {
            setImageResource(R.drawable.ic_callvault_logo)
            layoutParams = LinearLayout.LayoutParams(82.dp, 82.dp)
            contentDescription = "CallVault"
        })
        addView(tv("CALLVAULT", 23f, WHITE).also {
            it.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            it.letterSpacing = .12f
            it.setPadding(0, 8.dp, 0, 0)
        })
        addView(tv(if (sub.isEmpty()) "PRIVATE CALL BACKUP" else sub, 9f, MUTED).also {
            it.letterSpacing = .22f
            it.setPadding(0, 5.dp, 0, 0)
        })
    }

    private fun dotRow(): LinearLayout {
        dots.clear()
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 26.dp, 0, 26.dp)
            repeat(6) {
                val d = View(context).apply {
                    layoutParams = LinearLayout.LayoutParams(12.dp, 12.dp).also { it.setMargins(7.dp,0,7.dp,0) }
                    background = ContextCompat.getDrawable(context, R.drawable.dot_empty)
                }
                dots.add(d); addView(d)
            }
        }
    }

    private fun refreshDots() = dots.forEachIndexed { i,v ->
        v.background = ContextCompat.getDrawable(this, if (i < pinBuffer.length) R.drawable.dot_filled else R.drawable.dot_empty)
    }

    private fun numpad(onDigit:(Char)->Unit, onDelete:()->Unit): GridLayout =
        GridLayout(this).apply {
            columnCount=3; rowCount=4; setPadding(18.dp,0,18.dp,0)
            listOf("1","2","3","4","5","6","7","8","9","","0","⌫").forEachIndexed { i,label ->
                addView(tv(label,22f,if(label=="⌫") MUTED else WHITE).also { t ->
                    t.typeface=Typeface.create("sans-serif",Typeface.NORMAL)
                    t.layoutParams=GridLayout.LayoutParams().apply {
                        width=76.dp;height=64.dp;setMargins(7.dp,7.dp,7.dp,7.dp)
                        columnSpec=GridLayout.spec(i%3);rowSpec=GridLayout.spec(i/3)
                    }
                    if(label.isNotEmpty()){
                        t.background=ContextCompat.getDrawable(context,R.drawable.key_bg)
                        t.isClickable=true;t.isFocusable=true
                        t.setOnClickListener { if(label=="⌫") onDelete() else onDigit(label[0]) }
                    }
                })
            }
        }

    private fun showPinCreate() {
        pinBuffer.clear(); var first=""; var phase=0
        val status=tv("Create your secure 6-digit PIN",13f,MUTED)
        val l=root()
        l.addView(logo()); l.addView(space(18)); l.addView(dotRow()); l.addView(status); l.addView(space(12))
        l.addView(numpad({c->
            if(pinBuffer.length<6){pinBuffer.append(c);refreshDots()
                if(pinBuffer.length==6){
                    if(phase==0){first=pinBuffer.toString();pinBuffer.clear();refreshDots();phase=1;status.text="Confirm your PIN"}
                    else if(pinBuffer.toString()==first){pin.setPin(pinBuffer.toString());openVault()}
                    else{pinBuffer.clear();first="";phase=0;refreshDots();status.text="PINs do not match";status.setTextColor(RED)}
                }
            }
        },{if(pinBuffer.isNotEmpty()){pinBuffer.deleteCharAt(pinBuffer.lastIndex);refreshDots()}}))
        setContentView(l)
    }

    private fun showPinEnter() {
        pinBuffer.clear()
        val status=tv("Enter your secure PIN",13f,MUTED)
        val l=root()
        l.addView(logo());l.addView(space(18));l.addView(dotRow());l.addView(status);l.addView(space(12))
        l.addView(numpad({c->
            if(pinBuffer.length<6){pinBuffer.append(c);refreshDots()
                if(pinBuffer.length==6){
                    if(pin.verify(pinBuffer.toString())) openVault()
                    else{pinBuffer.clear();refreshDots();status.text="Incorrect PIN";status.setTextColor(RED)}
                }
            }
        },{if(pinBuffer.isNotEmpty()){pinBuffer.deleteCharAt(pinBuffer.lastIndex);refreshDots()}}))
        setContentView(l)
    }

    private fun openVault() {
        unlocked=true;last=SystemClock.elapsedRealtime()
        requestPerms();requestBatteryExemption();ScheduleHelper.schedule(this)
        val store=CallStore(this)
        val mins=ScheduleHelper.minutesUntilNext()
        val next=ScheduleHelper.nextSlotLabel()
        val pending=store.readSince(store.getLastCheckTime()).size

        val l=root(false).also{it.setPadding(20.dp,30.dp,20.dp,28.dp)}
        l.addView(logo("SMART HOURLY BACKUP"))
        l.addView(space(22))
        l.addView(TextClock(this).apply{
            format12Hour=null;format24Hour="HH:mm:ss";textSize=48f
            typeface=Typeface.create("sans-serif-light",Typeface.NORMAL);setTextColor(ACCENT);gravity=Gravity.CENTER
        })
        l.addView(TextClock(this).apply{
            format12Hour=null;format24Hour="EEEE, d MMM yyyy";textSize=12f;setTextColor(MUTED);gravity=Gravity.CENTER
        })
        l.addView(space(24))
        l.addView(LinearLayout(this).apply{
            orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER
            addView(infoCard("NEXT BACKUP",next,"in ${mins/60}h ${mins%60}m"))
            addView(space(10));addView(infoCard("PENDING","$pending","calls in queue"))
        })
        l.addView(space(14))
        l.addView(panel("HOURLY BACKUP","Only calls from the current 1-hour window are sent. Deleted calls are marked DELETED."))
        l.addView(space(12))
        l.addView(panel("SECURITY","Encrypted local PIN • Background backup • Private storage"))
        l.addView(space(22))
        l.addView(tv("LOCK VAULT",11f,MUTED).also{v->
            v.letterSpacing=.18f;v.setPadding(30.dp,16.dp,30.dp,16.dp);v.setOnClickListener{unlocked=false;showPinEnter()}
        })
        setContentView(l)
    }

    private fun panel(title:String,body:String)=LinearLayout(this).apply{
        orientation=LinearLayout.VERTICAL;background=ContextCompat.getDrawable(context,R.drawable.card_bg)
        setPadding(18.dp,16.dp,18.dp,16.dp)
        addView(tv(title,10f,ACCENT).also{it.gravity=Gravity.LEFT;it.letterSpacing=.12f})
        addView(tv(body,12f,MUTED).also{it.gravity=Gravity.LEFT;it.setPadding(0,7.dp,0,0)})
    }

    private fun infoCard(label:String,value:String,sub:String)=LinearLayout(this).apply{
        orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER_HORIZONTAL
        background=ContextCompat.getDrawable(context,R.drawable.card_bg);setPadding(16.dp,16.dp,16.dp,16.dp)
        layoutParams=LinearLayout.LayoutParams(0,-2,1f)
        addView(tv(label,9f,MUTED).also{it.letterSpacing=.12f})
        addView(tv(value,21f,ACCENT).also{it.typeface=Typeface.create("sans-serif-medium",0);it.setPadding(0,5.dp,0,0)})
        addView(tv(sub,10f,MUTED).also{it.setPadding(0,3.dp,0,0)})
    }

    private fun tv(text:String,size:Float,color:Int)=TextView(this).apply{this.text=text;textSize=size;setTextColor(color);gravity=Gravity.CENTER}
    private fun space(dp:Int)=View(this).apply{layoutParams=LinearLayout.LayoutParams(-1,dp.dp)}
    private val Int.dp get()=(this*resources.displayMetrics.density).toInt()

    private fun requestPerms(){
        val needed=mutableListOf(Manifest.permission.READ_CALL_LOG,Manifest.permission.READ_PHONE_STATE)
            .filter{ContextCompat.checkSelfPermission(this,it)!=PackageManager.PERMISSION_GRANTED}.toMutableList()
        if(Build.VERSION.SDK_INT>=33&&ContextCompat.checkSelfPermission(this,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            needed.add(Manifest.permission.POST_NOTIFICATIONS)
        if(needed.isNotEmpty())ActivityCompat.requestPermissions(this,needed.toTypedArray(),10)
    }

    private fun requestBatteryExemption(){
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
            val pm=getSystemService(PowerManager::class.java)
            if(!pm.isIgnoringBatteryOptimizations(packageName))
                startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply{data=Uri.parse("package:$packageName")})
        }
    }

    override fun onUserInteraction(){super.onUserInteraction();if(unlocked)last=SystemClock.elapsedRealtime()}
    override fun onResume(){super.onResume();if(unlocked&&SystemClock.elapsedRealtime()-last>=30_000){unlocked=false;showPinEnter()}}
}
