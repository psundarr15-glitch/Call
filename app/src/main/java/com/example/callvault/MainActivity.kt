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
import android.provider.ContactsContract
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
    private var lastInteraction = 0L
    private val pinBuffer = StringBuilder()
    private val dots = mutableListOf<View>()
    private val BG = Color.parseColor("#060B14")
    private val SURFACE = Color.parseColor("#0D1625")
    private val CARD = Color.parseColor("#111E31")
    private val ACCENT = Color.parseColor("#55E5CA")
    private val BLUE = Color.parseColor("#6EA8FF")
    private val PURPLE = Color.parseColor("#A98BFF")
    private val AMBER = Color.parseColor("#FFC857")
    private val WHITE = Color.parseColor("#F7FAFF")
    private val MUTED = Color.parseColor("#8798AD")
    private val RED = Color.parseColor("#FF6B82")
    private var activeTab = 0

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.statusBarColor = BG; window.navigationBarColor = BG
        NotificationHelper.createChannels(this)
        pin = SecurePinStore(this)
        if (pin.hasPin()) showPinEnter() else showPinCreate()
    }

    private fun page(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setBackgroundColor(BG); setPadding(18.dp, 18.dp, 18.dp, 18.dp)
    }

    private fun scrollPage(content: LinearLayout): ScrollView = ScrollView(this).apply {
        setBackgroundColor(BG); isFillViewport = true; addView(content)
    }

    private fun logo(sub: String = "PRIVATE ACCESS") = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
        addView(ImageView(this@MainActivity).apply { setImageResource(R.drawable.ic_vaultcall_mark); layoutParams = LinearLayout.LayoutParams(100.dp,100.dp) })
        addView(tv("VAULTCALL", 25f, WHITE).also { it.typeface = Typeface.create("sans-serif-black",0); it.letterSpacing=.08f; it.setPadding(0,8.dp,0,0) })
        addView(tv(sub, 9f, MUTED).also { it.letterSpacing=.22f; it.setPadding(0,4.dp,0,0) })
    }

    private fun showPinCreate() {
        pinBuffer.clear(); var first=""; var phase=0
        val status=tv("Create your secure 6-digit PIN",13f,MUTED)
        val l=page(); l.gravity=Gravity.CENTER; l.addView(space(28)); l.addView(logo()); l.addView(dotRow()); l.addView(status); l.addView(numpad({c->
            if(pinBuffer.length<6){pinBuffer.append(c);refreshDots();if(pinBuffer.length==6){if(phase==0){first=pinBuffer.toString();pinBuffer.clear();refreshDots();phase=1;status.text="Confirm your PIN"}else if(pinBuffer.toString()==first){pin.setPin(pinBuffer.toString());openVault()}else{pinBuffer.clear();first="";phase=0;refreshDots();status.text="PINs do not match";status.setTextColor(RED)}}}
        },{if(pinBuffer.isNotEmpty()){pinBuffer.deleteCharAt(pinBuffer.lastIndex);refreshDots()}})); l.addView(space(30)); setContentView(l)
    }
    private fun showPinEnter() {
        pinBuffer.clear(); val status=tv("Enter your secure PIN",13f,MUTED); val l=page(); l.gravity=Gravity.CENTER
        l.addView(space(28)); l.addView(logo()); l.addView(dotRow()); l.addView(status); l.addView(numpad({c->if(pinBuffer.length<6){pinBuffer.append(c);refreshDots();if(pinBuffer.length==6){if(pin.verify(pinBuffer.toString()))openVault()else{pinBuffer.clear();refreshDots();status.text="Incorrect PIN";status.setTextColor(RED)}}}},{if(pinBuffer.isNotEmpty()){pinBuffer.deleteCharAt(pinBuffer.lastIndex);refreshDots()}})); setContentView(l)
    }
    private fun dotRow()=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER;setPadding(0,22.dp,0,18.dp);dots.clear();repeat(6){val d=View(context).apply{layoutParams=LinearLayout.LayoutParams(11.dp,11.dp).also{it.setMargins(6.dp,0,6.dp,0)};background=ContextCompat.getDrawable(context,R.drawable.dot_empty)};dots.add(d);addView(d)}}
    private fun refreshDots(){dots.forEachIndexed{i,v->v.background=ContextCompat.getDrawable(this,if(i<pinBuffer.length)R.drawable.dot_filled else R.drawable.dot_empty)}}
    private fun numpad(onDigit:(Char)->Unit,onDelete:()->Unit)=GridLayout(this).apply{columnCount=3;rowCount=4;setPadding(12.dp,0,12.dp,0);listOf("1","2","3","4","5","6","7","8","9","","0","⌫").forEachIndexed{i,label->addView(tv(label,22f,if(label=="⌫")MUTED else WHITE).also{t->t.typeface=Typeface.DEFAULT_BOLD;t.layoutParams=GridLayout.LayoutParams().apply{width=82.dp;height=66.dp;setMargins(6.dp,6.dp,6.dp,6.dp);columnSpec=GridLayout.spec(i%3);rowSpec=GridLayout.spec(i/3)};if(label.isNotEmpty()){t.background=ContextCompat.getDrawable(context,R.drawable.key_bg);t.setOnClickListener{if(label=="⌫")onDelete()else onDigit(label[0])}}}})}

    private fun openVault(){unlocked=true;lastInteraction=SystemClock.elapsedRealtime();requestPerms();requestBatteryExemption();ScheduleHelper.schedule(this);showDashboard()}

    private fun header(title:String,subtitle:String=""): LinearLayout = LinearLayout(this).apply {
        orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL
        addView(ImageView(this@MainActivity).apply{setImageResource(R.drawable.ic_vaultcall_mark);layoutParams=LinearLayout.LayoutParams(46.dp,46.dp)})
        addView(LinearLayout(this@MainActivity).apply{orientation=LinearLayout.VERTICAL;layoutParams=LinearLayout.LayoutParams(0,-2,1f);setPadding(12.dp,0,0,0);addView(tv(title,20f,WHITE).also{it.gravity=Gravity.LEFT;it.typeface=Typeface.DEFAULT_BOLD});if(subtitle.isNotEmpty())addView(tv(subtitle,9f,MUTED).also{it.gravity=Gravity.LEFT;it.letterSpacing=.12f;it.setPadding(0,3.dp,0,0)})})
        addView(tv("●",18f,ACCENT))
    }

    private fun showDashboard(){activeTab=0; val p=page(); p.addView(header("VaultCall","SECURE CALL ARCHIVE"));p.addView(space(18));
        val event=ScheduleHelper.nextEvent(); val mins=((event.time-System.currentTimeMillis()).coerceAtLeast(0))/60000; val all=CallStore(this).readAll(); val stats=BackupStatsStore(this); val history=BackupHistoryStore(this).all(); val deleted=all.count{it.deleted}
        p.addView(hero(SimpleDateFormat("hh:mm a",Locale.getDefault()).format(Date(event.time)),mins,ScheduleHelper.modeLabel(event.mode)));p.addView(space(12));
        val grid=GridLayout(this).apply{columnCount=2;layoutParams=LinearLayout.LayoutParams(-1,-2)}
        grid.addView(stat("SAVED CALLS",all.size.toString(),"total records",ACCENT));grid.addView(stat("DELETED",deleted.toString(),"marked deleted",RED));grid.addView(stat("LAST SENT",if(stats.lastSentCount()==0)"—" else stats.lastSentCount().toString(),"calls in last send",AMBER));grid.addView(stat("BACKUPS",stats.totalBackups().toString(),"successful sends",PURPLE));p.addView(grid);p.addView(space(14));
        p.addView(section("BACKUP TIMELINE","4-HOUR INCREMENTAL","Every 4 hours • only the current window"));p.addView(space(8));p.addView(section("DAILY FULL","07:00 PM","Full backup for the current day"));p.addView(space(8));p.addView(section("WEEKLY FULL","TUE + FRI • 05:00 PM","Full saved archive"));p.addView(space(14));
        p.addView(action("VIEW BACKUP HISTORY","See every successful send",PURPLE){showHistory()});p.addView(space(8));p.addView(action("SECURITY CENTER","PIN, storage and protection",BLUE){showSecurity()});p.addView(space(14));setContentView(withNav(p,0))
    }

    private fun showCalls(){
        activeTab=1; val p=page(); p.addView(header("Call Records","LOCAL CALL ARCHIVE")); p.addView(space(14))
        val all=CallStore(this).readAll().sortedByDescending{it.date}
        val holder=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        p.addView(searchBox("Search number") { q -> holder.removeAllViews(); renderCallsInto(holder, if(q.isBlank()) all else all.filter{it.number.contains(q,true)}) })
        p.addView(space(10)); renderCallsInto(holder,all); p.addView(holder); setContentView(withNav(p,1))
    }
    private fun renderCallsInto(holder:LinearLayout,list:List<CallEntry>){
        holder.removeAllViews(); list.take(100).forEach{e->holder.addView(callRow(e));holder.addView(space(7))}
        if(list.isEmpty())holder.addView(empty("No saved calls","New captured calls will appear here."))
    }
    private fun callRow(e:CallEntry)=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;background=ContextCompat.getDrawable(context,R.drawable.card_bg);setPadding(14.dp,13.dp,14.dp,13.dp);addView(tv(if(e.deleted)"●" else "●",18f,if(e.deleted)RED else ACCENT).apply{layoutParams=LinearLayout.LayoutParams(26.dp, -2)});addView(LinearLayout(context).apply{orientation=LinearLayout.VERTICAL;layoutParams=LinearLayout.LayoutParams(0,-2,1f);addView(tv(e.number.ifBlank{"Unknown"},14f,WHITE).also{it.gravity=Gravity.LEFT;it.typeface=Typeface.DEFAULT_BOLD});addView(tv("${typeLabel(e.type)} • ${formatDate(e.date)} • ${e.duration}s",10f,MUTED).also{it.gravity=Gravity.LEFT;it.setPadding(0,4.dp,0,0)})});if(e.deleted)addView(tv("DELETED",9f,RED).apply{setPadding(9.dp,5.dp,9.dp,5.dp);background=ContextCompat.getDrawable(context,R.drawable.pill_bg)})}

    private fun showContacts(){activeTab=3;val p=page();p.addView(header("Contacts","SAVED CONTACT DIRECTORY"));p.addView(space(14));p.addView(searchBox("Search contacts"){});p.addView(space(10));val contacts=readContacts();contacts.take(100).forEach{(name,phone)->p.addView(contactRow(name,phone));p.addView(space(7))};if(contacts.isEmpty())p.addView(empty("No contacts available","Allow Contacts permission to show names here."));setContentView(withNav(p,3))}
    private fun readContacts():List<Pair<String,String>>{if(ContextCompat.checkSelfPermission(this,Manifest.permission.READ_CONTACTS)!=PackageManager.PERMISSION_GRANTED)return emptyList();val out=mutableListOf<Pair<String,String>>();contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,ContactsContract.CommonDataKinds.Phone.NUMBER),null,null,ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME+" ASC")?.use{c->val n=c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);val ph=c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);while(c.moveToNext())out.add((c.getString(n) ?: "Unknown") to (c.getString(ph) ?: ""))};return out.distinct()}
    private fun contactRow(name:String,phone:String)=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;background=ContextCompat.getDrawable(context,R.drawable.card_bg);setPadding(13.dp,12.dp,13.dp,12.dp);addView(tv(name.take(1).uppercase(),16f,ACCENT).apply{gravity=Gravity.CENTER;background=ContextCompat.getDrawable(context,R.drawable.pill_bg);layoutParams=LinearLayout.LayoutParams(44.dp,44.dp)});addView(LinearLayout(context).apply{orientation=LinearLayout.VERTICAL;layoutParams=LinearLayout.LayoutParams(0,-2,1f);setPadding(12.dp,0,0,0);addView(tv(name,14f,WHITE).also{it.gravity=Gravity.LEFT;it.typeface=Typeface.DEFAULT_BOLD});addView(tv(phone,10f,MUTED).also{it.gravity=Gravity.LEFT;it.setPadding(0,3.dp,0,0)})})}

    private fun showSchedule(){activeTab=2;val p=page();p.addView(header("Backup Schedule","AUTOMATION CONTROL"));p.addView(space(16));val e=ScheduleHelper.nextEvent();p.addView(hero(SimpleDateFormat("hh:mm a",Locale.getDefault()).format(Date(e.time)),ScheduleHelper.minutesUntilNext(),ScheduleHelper.modeLabel(e.mode)));p.addView(space(14));p.addView(section("4-HOUR INCREMENTAL","00:00 • 04:00 • 08:00 • 12:00 • 16:00 • 20:00","Only calls inside each 4-hour window are sent."));p.addView(space(9));p.addView(section("DAILY FULL","Every day • 07:00 PM","Sends the current day's complete saved call list."));p.addView(space(9));p.addView(section("WEEKLY FULL","Tuesday + Friday • 05:00 PM","Sends the complete saved archive."));p.addView(space(16));p.addView(action("OPEN BACKUP HISTORY","Review actual sends",PURPLE){showHistory()});setContentView(withNav(p,2))}

    private fun showHistory(){val p=page();p.addView(header("Backup History","DELIVERY TIMELINE"));p.addView(space(14));val h=BackupHistoryStore(this).all();if(h.isEmpty())p.addView(empty("No backups yet","Your first successful backup will appear here."));h.forEach{item->p.addView(historyRow(item));p.addView(space(8))};p.addView(space(12));p.addView(action("BACK TO DASHBOARD","Return to command center",ACCENT){showDashboard()});setContentView(scrollPage(p))}
    private fun historyRow(i:BackupHistoryItem)=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;background=ContextCompat.getDrawable(context,R.drawable.card_bg);setPadding(16.dp,14.dp,16.dp,14.dp);addView(LinearLayout(context).apply{orientation=LinearLayout.HORIZONTAL;addView(tv(i.mode.replace('_',' '),11f,ACCENT).apply{layoutParams=LinearLayout.LayoutParams(0,-2,1f);gravity=Gravity.LEFT;typeface=Typeface.DEFAULT_BOLD});addView(tv(if(i.success)"SUCCESS" else "FAILED",9f,if(i.success)ACCENT else RED).also{it.setPadding(8.dp,4.dp,8.dp,4.dp);it.background=ContextCompat.getDrawable(context,R.drawable.pill_bg)})});addView(tv("${i.count} calls • ${i.deleted} deleted",14f,WHITE).also{it.gravity=Gravity.LEFT;it.setPadding(0,8.dp,0,0)});addView(tv(formatDate(i.time),10f,MUTED).also{it.gravity=Gravity.LEFT;it.setPadding(0,4.dp,0,0)})}

    private fun showSettings(){activeTab=4;val p=page();p.addView(header("Settings","VAULTCALL CONTROL CENTER"));p.addView(space(16));p.addView(setting("Backup automation","4-hour + Daily 7 PM + Tue/Fri 5 PM","Active",ACCENT));p.addView(space(8));p.addView(setting("Telegram delivery","Secure document delivery","Configured by build settings",BLUE));p.addView(space(8));p.addView(setting("Battery protection","Background reliability","Open system settings",AMBER){requestBatteryExemption()});p.addView(space(8));p.addView(setting("PIN security","6-digit local vault lock","Enabled",PURPLE));p.addView(space(8));p.addView(setting("Contacts access","Used to display contact names","Grant permission",ACCENT){requestContacts()});p.addView(space(16));p.addView(action("SECURITY INFO","See privacy and protection details",BLUE){showSecurity()});p.addView(space(8));p.addView(action("LOCK VAULT","Require PIN again",RED){unlocked=false;showPinEnter()});setContentView(withNav(p,4))}
    private fun showSecurity(){val p=page();p.addView(header("Security Info","PRIVATE BY DESIGN"));p.addView(space(16));p.addView(section("LOCAL VAULT","PIN protected","The app locks after inactivity and stores its call archive locally."));p.addView(space(8));p.addView(section("DELETED CALL DETECTION","Enabled","Previously saved calls can be marked DELETED when they disappear from the phone call log."));p.addView(space(8));p.addView(section("DELIVERY","Telegram document","Backup CSV files are delivered through the configured Telegram bot."));p.addView(space(8));p.addView(section("SCHEDULE","Automated","4-hour incremental, daily 7 PM full, Tuesday/Friday 5 PM full."));p.addView(space(8));p.addView(section("APP LOCK","6-digit PIN","PIN is stored using Android encrypted preferences through the app's PIN store."));p.addView(space(18));p.addView(action("BACK TO SETTINGS","Return to controls",ACCENT){showSettings()});setContentView(scrollPage(p))}

    private fun withNav(content:LinearLayout,selected:Int):LinearLayout{val wrap=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(BG);addView(scrollPage(content),LinearLayout.LayoutParams(-1,0,1f));val nav=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER;background=ColorDrawableCompat(SURFACE);setPadding(8.dp,8.dp,8.dp,10.dp)};listOf("⌂\nHome","◉\nCalls","◷\nSchedule","♙\nContacts","⚙\nSettings").forEachIndexed{i,label->nav.addView(tv(label,10f,if(i==selected)ACCENT else MUTED).apply{layoutParams=LinearLayout.LayoutParams(0,58.dp,1f);setPadding(3.dp,6.dp,3.dp,3.dp);setOnClickListener{when(i){0->showDashboard();1->showCalls();2->showSchedule();3->showContacts();4->showSettings()}}})};addView(nav)};return wrap}

    private fun hero(next:String,mins:Long,label:String)=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;background=ContextCompat.getDrawable(context,R.drawable.hero_bg);setPadding(20.dp,20.dp,20.dp,20.dp);addView(tv("NEXT DATA SEND",10f,ACCENT).also{it.gravity=Gravity.LEFT;it.letterSpacing=.15f});addView(tv(next,34f,WHITE).also{it.gravity=Gravity.LEFT;it.typeface=Typeface.create("sans-serif-black",0);it.setPadding(0,4.dp,0,0)});addView(tv("$label • ${mins/60}h ${mins%60}m remaining",11f,MUTED).also{it.gravity=Gravity.LEFT;it.setPadding(0,3.dp,0,0)})}
    private fun stat(label:String,value:String,sub:String,accent:Int)=TextView(this).apply{text="$label\n$value\n$sub";textSize=11f;setTextColor(MUTED);gravity=Gravity.CENTER;background=ContextCompat.getDrawable(context,R.drawable.stat_bg);setPadding(8.dp,14.dp,8.dp,14.dp);layoutParams=GridLayout.LayoutParams().apply{width=0;height=104.dp;columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);setMargins(5.dp,5.dp,5.dp,5.dp)};val s=android.text.SpannableString(text);val a=text.indexOf('\n');val b=text.indexOf('\n',a+1);s.setSpan(android.text.style.ForegroundColorSpan(accent),a+1,b,0);s.setSpan(android.text.style.RelativeSizeSpan(1.55f),a+1,b,0);setText(s)}
    private fun section(title:String,value:String,body:String)=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;background=ContextCompat.getDrawable(context,R.drawable.card_bg);setPadding(16.dp,14.dp,16.dp,14.dp);addView(tv(title,9f,MUTED).also{it.gravity=Gravity.LEFT;it.letterSpacing=.14f});addView(tv(value,15f,WHITE).also{it.gravity=Gravity.LEFT;it.typeface=Typeface.DEFAULT_BOLD;it.setPadding(0,5.dp,0,0)});addView(tv(body,10f,MUTED).also{it.gravity=Gravity.LEFT;it.setPadding(0,4.dp,0,0)})}
    private fun setting(title:String,value:String,status:String,color:Int,onClick:(()->Unit)?=null)=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;background=ContextCompat.getDrawable(context,R.drawable.card_bg);setPadding(16.dp,14.dp,14.dp,14.dp);addView(LinearLayout(context).apply{orientation=LinearLayout.VERTICAL;layoutParams=LinearLayout.LayoutParams(0,-2,1f);addView(tv(title,14f,WHITE).also{it.gravity=Gravity.LEFT;it.typeface=Typeface.DEFAULT_BOLD});addView(tv(value,10f,MUTED).also{it.gravity=Gravity.LEFT;it.setPadding(0,4.dp,0,0)})});addView(tv(status,9f,color).apply{setPadding(9.dp,6.dp,9.dp,6.dp);background=ContextCompat.getDrawable(context,R.drawable.pill_bg);if(onClick!=null)setOnClickListener{onClick()}})}
    private fun action(title:String,sub:String,color:Int,onClick:()->Unit)=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;background=ContextCompat.getDrawable(context,R.drawable.card_bg);setPadding(16.dp,15.dp,16.dp,15.dp);setOnClickListener{onClick()};addView(LinearLayout(context).apply{orientation=LinearLayout.VERTICAL;layoutParams=LinearLayout.LayoutParams(0,-2,1f);addView(tv(title,12f,color).also{it.gravity=Gravity.LEFT;it.typeface=Typeface.DEFAULT_BOLD});addView(tv(sub,10f,MUTED).also{it.gravity=Gravity.LEFT;it.setPadding(0,4.dp,0,0)})});addView(tv("›",25f,color))}
    private fun searchBox(hint:String,onText:(String)->Unit)=EditText(this).apply{setHint(hint);setHintTextColor(MUTED);setTextColor(WHITE);textSize=13f;singleLine=true;background=ContextCompat.getDrawable(context,R.drawable.card_bg);setPadding(14.dp,0,14.dp,0);layoutParams=LinearLayout.LayoutParams(-1,52.dp);addTextChangedListener(object:android.text.TextWatcher{override fun beforeTextChanged(s:CharSequence?,st:Int,c:Int,a:Int){};override fun onTextChanged(s:CharSequence?,st:Int,b:Int,c:Int){onText(s?.toString() ?: "")};override fun afterTextChanged(s:android.text.Editable?){}})}
    private fun empty(title:String,body:String)=section("EMPTY",title,body)
    private fun typeLabel(t:Int)=when(t){1->"Incoming";2->"Outgoing";3->"Missed";4->"Voicemail";5->"Rejected";6->"Blocked";else->"Unknown"}
    private fun formatDate(ts:Long)=SimpleDateFormat("dd MMM yyyy • hh:mm a",Locale.getDefault()).format(Date(ts))
    private fun tv(text:String,size:Float,color:Int)=TextView(this).apply{this.text=text;textSize=size;setTextColor(color);gravity=Gravity.CENTER}
    private fun space(dp:Int)=View(this).apply{layoutParams=LinearLayout.LayoutParams(-1,dp.dp)}
    private val Int.dp get()=(this*resources.displayMetrics.density).toInt()
    private fun requestPerms(){val needed=mutableListOf(Manifest.permission.READ_CALL_LOG,Manifest.permission.READ_PHONE_STATE).filter{ContextCompat.checkSelfPermission(this,it)!=PackageManager.PERMISSION_GRANTED}.toMutableList();if(Build.VERSION.SDK_INT>=33&&ContextCompat.checkSelfPermission(this,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)needed.add(Manifest.permission.POST_NOTIFICATIONS);if(needed.isNotEmpty())ActivityCompat.requestPermissions(this,needed.toTypedArray(),10);requestContacts()}
    private fun requestContacts(){if(ContextCompat.checkSelfPermission(this,Manifest.permission.READ_CONTACTS)!=PackageManager.PERMISSION_GRANTED)ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.READ_CONTACTS),11)}
    private fun requestBatteryExemption(){if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){val pm=getSystemService(PowerManager::class.java);if(!pm.isIgnoringBatteryOptimizations(packageName))startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply{data=Uri.parse("package:$packageName")})}}
    override fun onUserInteraction(){super.onUserInteraction();if(unlocked)lastInteraction=SystemClock.elapsedRealtime()}
    override fun onResume(){super.onResume();if(unlocked&&SystemClock.elapsedRealtime()-lastInteraction>=30_000){unlocked=false;showPinEnter()}else if(unlocked)showDashboard()}
}

private class ColorDrawableCompat(color:Int): android.graphics.drawable.ColorDrawable(color)
