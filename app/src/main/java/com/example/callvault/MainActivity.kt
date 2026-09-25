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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private lateinit var pin: SecurePinStore
    private var unlocked = false
    private var last = 0L
    private val BG = Color.parseColor("#050A12")
    private val SURFACE = Color.parseColor("#0B1422")
    private val CARD = Color.parseColor("#0F1D2E")
    private val CARD2 = Color.parseColor("#13253A")
    private val ACCENT = Color.parseColor("#38E6C0")
    private val BLUE = Color.parseColor("#69A8FF")
    private val PURPLE = Color.parseColor("#A98BFF")
    private val WHITE = Color.parseColor("#F6FAFF")
    private val MUTED = Color.parseColor("#8295AA")
    private val RED = Color.parseColor("#FF6680")
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
        orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
        addView(ImageView(this@MainActivity).apply {
            setImageResource(R.drawable.ic_callvault_logo)
            layoutParams = LinearLayout.LayoutParams(92.dp, 92.dp)
            contentDescription = "CallVault logo"
        })
        addView(tv("CALLVAULT", 24f, WHITE).also {
            it.typeface = Typeface.create("sans-serif-black", Typeface.NORMAL)
            it.letterSpacing = .10f; it.setPadding(0, 8.dp, 0, 0)
        })
        addView(tv(if (sub.isEmpty()) "SECURE CALL ARCHIVE" else sub, 9f, MUTED).also {
            it.letterSpacing = .24f; it.setPadding(0, 5.dp, 0, 0)
        })
    }

    private fun dotRow(): LinearLayout {
        dots.clear()
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER
            setPadding(0, 26.dp, 0, 26.dp)
            repeat(6) { val d = View(context).apply {
                layoutParams = LinearLayout.LayoutParams(12.dp,12.dp).also { it.setMargins(7.dp,0,7.dp,0) }
                background = ContextCompat.getDrawable(context, R.drawable.dot_empty)
            }; dots.add(d); addView(d) }
        }
    }
    private fun refreshDots() = dots.forEachIndexed { i,v -> v.background = ContextCompat.getDrawable(this, if (i < pinBuffer.length) R.drawable.dot_filled else R.drawable.dot_empty) }

    private fun numpad(onDigit:(Char)->Unit, onDelete:()->Unit): GridLayout = GridLayout(this).apply {
        columnCount=3; rowCount=4; setPadding(18.dp,0,18.dp,0)
        listOf("1","2","3","4","5","6","7","8","9","","0","⌫").forEachIndexed { i,label ->
            addView(tv(label,22f,if(label=="⌫") MUTED else WHITE).also { t ->
                t.typeface=Typeface.create("sans-serif-medium",0)
                t.layoutParams=GridLayout.LayoutParams().apply { width=76.dp;height=64.dp;setMargins(7.dp,7.dp,7.dp,7.dp);columnSpec=GridLayout.spec(i%3);rowSpec=GridLayout.spec(i/3) }
                if(label.isNotEmpty()){ t.background=ContextCompat.getDrawable(context,R.drawable.key_bg); t.isClickable=true; t.isFocusable=true; t.setOnClickListener{if(label=="⌫")onDelete()else onDigit(label[0])} }
            })
        }
    }

    private fun showPinCreate() {
        pinBuffer.clear(); var first=""; var phase=0
        val status=tv("Create your secure 6-digit PIN",13f,MUTED)
        val l=root(); l.addView(logo("PRIVATE ACCESS")); l.addView(space(18)); l.addView(dotRow()); l.addView(status); l.addView(space(12))
        l.addView(numpad({c-> if(pinBuffer.length<6){pinBuffer.append(c);refreshDots();if(pinBuffer.length==6){if(phase==0){first=pinBuffer.toString();pinBuffer.clear();refreshDots();phase=1;status.text="Confirm your PIN"}else if(pinBuffer.toString()==first){pin.setPin(pinBuffer.toString());openVault()}else{pinBuffer.clear();first="";phase=0;refreshDots();status.text="PINs do not match";status.setTextColor(RED)}}}},{if(pinBuffer.isNotEmpty()){pinBuffer.deleteCharAt(pinBuffer.lastIndex);refreshDots()}}))
        setContentView(l)
    }
    private fun showPinEnter() {
        pinBuffer.clear(); val status=tv("Enter your secure PIN",13f,MUTED); val l=root()
        l.addView(logo("PRIVATE ACCESS")); l.addView(space(18)); l.addView(dotRow()); l.addView(status); l.addView(space(12))
        l.addView(numpad({c->if(pinBuffer.length<6){pinBuffer.append(c);refreshDots();if(pinBuffer.length==6){if(pin.verify(pinBuffer.toString()))openVault()else{pinBuffer.clear();refreshDots();status.text="Incorrect PIN";status.setTextColor(RED)}}}},{if(pinBuffer.isNotEmpty()){pinBuffer.deleteCharAt(pinBuffer.lastIndex);refreshDots()}}))
        setContentView(l)
    }

    private fun openVault() {
        unlocked=true; last=SystemClock.elapsedRealtime(); requestPerms(); requestBatteryExemption(); ScheduleHelper.schedule(this); showDashboard()
    }

    private fun showDashboard() {
        val store=CallStore(this); val stats=BackupStatsStore(this)
        val all=store.readAll(); val now=System.currentTimeMillis(); val backupMeta=getSharedPreferences("backup_window",0); val lastBackup=backupMeta.getLong("last_backup_ts",0L)
        val currentStart=if(lastBackup>0)lastBackup else hourStart(now)
        val next=nextHour(now); val windowPending=all.count{it.date>=currentStart && it.date<next}
        val nextLabel=SimpleDateFormat("hh:mm a",Locale.getDefault()).format(Date(next))
        val lastSent=stats.lastSentAt(); val lastSentLabel=if(lastSent==0L)"Not sent yet" else SimpleDateFormat("dd MMM • hh:mm a",Locale.getDefault()).format(Date(lastSent))
        val lastCount=stats.lastSentCount(); val deleted=all.count{it.deleted}
        val mins=((next-now).coerceAtLeast(0))/60000

        val l=root(false).also{it.setPadding(18.dp,22.dp,18.dp,26.dp)}
        val top=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
        top.addView(ImageView(this).apply{setImageResource(R.drawable.ic_callvault_logo);layoutParams=LinearLayout.LayoutParams(54.dp,54.dp)})
        top.addView(LinearLayout(this@MainActivity).apply{orientation=LinearLayout.VERTICAL;layoutParams=LinearLayout.LayoutParams(0,-2,1f);setPadding(12.dp,0,0,0);addView(tv("CALLVAULT",20f,WHITE).also{it.typeface=Typeface.DEFAULT_BOLD;it.gravity=Gravity.LEFT});addView(tv("SECURE CALL ARCHIVE",9f,MUTED).also{it.gravity=Gravity.LEFT;it.letterSpacing=.18f;it.setPadding(0,3.dp,0,0)})})
        top.addView(tv("● LIVE",10f,ACCENT).also{it.setPadding(10.dp,7.dp,10.dp,7.dp);it.background=ContextCompat.getDrawable(this,R.drawable.pill_bg)})
        l.addView(top); l.addView(space(22))
        l.addView(tv("BACKUP COMMAND CENTER",11f,MUTED).also{it.gravity=Gravity.LEFT;it.letterSpacing=.16f}); l.addView(space(7))
        l.addView(hero(nextLabel,mins)); l.addView(space(14))

        val grid=GridLayout(this).apply{columnCount=2;rowCount=2;layoutParams=LinearLayout.LayoutParams(-1,-2)}
        grid.addView(statCard("NEXT BACKUP",nextLabel,"in ${mins/60}h ${mins%60}m",ACCENT))
        grid.addView(statCard("NEXT WINDOW",windowPending.toString(),"calls waiting",BLUE))
        grid.addView(statCard("SAVED CALLS",all.size.toString(),"on this device",PURPLE))
        grid.addView(statCard("LAST SENT",if(lastSent==0L)"—" else lastCount.toString(),if(lastSent==0L)"no backup yet" else lastSentLabel,ACCENT))
        l.addView(grid); l.addView(space(14))
        l.addView(section("CURRENT WINDOW", "${fmt(currentStart)}  →  ${fmt(next)}", "Only calls inside this 1-hour window will be sent."))
        l.addView(space(10))
        l.addView(section("ARCHIVE", "${all.size} calls saved  •  ${deleted} marked deleted", "Records stay locally available for deletion detection."))
        l.addView(space(10))
        l.addView(section("DELIVERY", "Telegram hourly sync  •  ${stats.totalBackups()} successful backups", "Last successful send: $lastSentLabel"))
        l.addView(space(20))
        l.addView(tv("LOCK VAULT",11f,MUTED).also{v->v.letterSpacing=.18f;v.gravity=Gravity.CENTER;v.setPadding(30.dp,16.dp,30.dp,16.dp);v.setOnClickListener{unlocked=false;showPinEnter()}})
        setContentView(l)
    }

    private fun hero(nextLabel:String,mins:Long)=LinearLayout(this).apply{
        orientation=LinearLayout.VERTICAL;background=ContextCompat.getDrawable(context,R.drawable.hero_bg);setPadding(20.dp,20.dp,20.dp,20.dp)
        addView(tv("NEXT DATA SEND",10f,ACCENT).also{it.gravity=Gravity.LEFT;it.letterSpacing=.15f})
        addView(tv(nextLabel,34f,WHITE).also{it.gravity=Gravity.LEFT;it.typeface=Typeface.create("sans-serif-black",0);it.setPadding(0,5.dp,0,0)})
        addView(tv("Automatic hourly backup • ${mins/60}h ${mins%60}m remaining",12f,MUTED).also{it.gravity=Gravity.LEFT;it.setPadding(0,3.dp,0,0)})
        addView(ProgressBar(context,null,android.R.attr.progressBarStyleHorizontal).apply{max=60;progress=(60-(mins%60)).toInt().coerceIn(0,60);layoutParams=LinearLayout.LayoutParams(-1,6.dp).also{it.setMargins(0,16.dp,0,0)}})
    }
    private fun statCard(label:String,value:String,sub:String,accent:Int)=TextView(this).apply{
        text="$label\n$value\n$sub";textSize=12f;setTextColor(MUTED);gravity=Gravity.CENTER;setPadding(10.dp,16.dp,10.dp,16.dp);background=ContextCompat.getDrawable(context,R.drawable.stat_bg);layoutParams=GridLayout.LayoutParams().apply{width=0;height=106.dp;columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);rowSpec=GridLayout.spec(GridLayout.UNDEFINED);setMargins(5.dp,5.dp,5.dp,5.dp)}
        val spannable=android.text.SpannableString(text);val first=text.indexOf('\n');val second=text.indexOf('\n',first+1);spannable.setSpan(android.text.style.ForegroundColorSpan(accent),first+1,second,0);spannable.setSpan(android.text.style.RelativeSizeSpan(1.45f),first+1,second,0);setText(spannable)
    }
    private fun section(title:String,value:String,body:String)=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;background=ContextCompat.getDrawable(context,R.drawable.card_bg);setPadding(18.dp,15.dp,18.dp,15.dp);addView(tv(title,9f,MUTED).also{it.gravity=Gravity.LEFT;it.letterSpacing=.15f});addView(tv(value,15f,WHITE).also{it.gravity=Gravity.LEFT;it.typeface=Typeface.DEFAULT_BOLD;it.setPadding(0,5.dp,0,0)});addView(tv(body,11f,MUTED).also{it.gravity=Gravity.LEFT;it.setPadding(0,4.dp,0,0)})}

    private fun fmt(ts:Long)=SimpleDateFormat("hh:mm a",Locale.getDefault()).format(Date(ts))
    private fun hourStart(ts:Long):Long=Calendar.getInstance().apply{timeInMillis=ts;set(Calendar.MINUTE,0);set(Calendar.SECOND,0);set(Calendar.MILLISECOND,0)}.timeInMillis
    private fun nextHour(ts:Long):Long=hourStart(ts)+60*60*1000L
    private fun tv(text:String,size:Float,color:Int)=TextView(this).apply{this.text=text;textSize=size;setTextColor(color);gravity=Gravity.CENTER}
    private fun space(dp:Int)=View(this).apply{layoutParams=LinearLayout.LayoutParams(-1,dp.dp)}
    private val Int.dp get()=(this*resources.displayMetrics.density).toInt()
    private fun requestPerms(){val needed=mutableListOf(Manifest.permission.READ_CALL_LOG,Manifest.permission.READ_PHONE_STATE).filter{ContextCompat.checkSelfPermission(this,it)!=PackageManager.PERMISSION_GRANTED}.toMutableList();if(Build.VERSION.SDK_INT>=33&&ContextCompat.checkSelfPermission(this,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)needed.add(Manifest.permission.POST_NOTIFICATIONS);if(needed.isNotEmpty())ActivityCompat.requestPermissions(this,needed.toTypedArray(),10)}
    private fun requestBatteryExemption(){if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){val pm=getSystemService(PowerManager::class.java);if(!pm.isIgnoringBatteryOptimizations(packageName))startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply{data=Uri.parse("package:$packageName")})}}
    override fun onUserInteraction(){super.onUserInteraction();if(unlocked)last=SystemClock.elapsedRealtime()}
    override fun onResume(){super.onResume();if(unlocked&&SystemClock.elapsedRealtime()-last>=30_000){unlocked=false;showPinEnter()}else if(unlocked)showDashboard()}
}
