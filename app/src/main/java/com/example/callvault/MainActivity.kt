package com.example.callvault

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
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

    private val BG = Color.parseColor("#070A12")
    private val SURFACE = Color.parseColor("#0D1220")
    private val CARD = Color.parseColor("#111827")
    private val GOLD = Color.parseColor("#D7B56D")
    private val GOLD_SOFT = Color.parseColor("#F1D79A")
    private val WHITE = Color.parseColor("#F7F8FC")
    private val MUTED = Color.parseColor("#8791A6")
    private val DIM = Color.parseColor("#252C3B")
    private val RED = Color.parseColor("#FF6B6B")
    private val GREEN = Color.parseColor("#57D9A3")

    private val pinBuffer = StringBuilder()
    private val dots = mutableListOf<View>()

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.statusBarColor = BG
        window.navigationBarColor = BG
        if (Build.VERSION.SDK_INT >= 23) window.decorView.systemUiVisibility = 0
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

    private fun brand(sub: String = "", compact: Boolean = false): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            val mark = TextView(context).apply {
                text = "◈"
                textSize = if (compact) 30f else 42f
                setTextColor(GOLD)
                gravity = Gravity.CENTER
                typeface = Typeface.DEFAULT_BOLD
            }
            addView(mark, LinearLayout.LayoutParams(-2, if (compact) 38.dp else 48.dp))
            addView(tv("JEEVI", if (compact) 20f else 28f, WHITE).also {
                it.typeface = Typeface.create("sans-serif", Typeface.BOLD)
                it.letterSpacing = 0.28f
            })
            addView(tv("CALL VAULT", 9f, GOLD).also {
                it.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                it.letterSpacing = 0.28f
                it.setPadding(0, 3.dp, 0, 0)
            })
            if (sub.isNotEmpty()) addView(tv(sub, 10f, MUTED).also {
                it.letterSpacing = 0.12f
                it.setPadding(0, 7.dp, 0, 0)
            })
        }
    }

    private fun panel(): GradientDrawable = GradientDrawable().apply {
        cornerRadius = 22.dp.toFloat()
        setColor(SURFACE)
        setStroke(1.dp, Color.parseColor("#20283A"))
    }

    private fun goldButton(): GradientDrawable = GradientDrawable(
        GradientDrawable.Orientation.TL_BR,
        intArrayOf(Color.parseColor("#E8CA83"), Color.parseColor("#B98E45"))
    ).apply { cornerRadius = 16.dp.toFloat() }

    private fun dotRow(): LinearLayout {
        dots.clear()
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 24.dp, 0, 22.dp)
            repeat(6) {
                val d = View(context).apply {
                    layoutParams = LinearLayout.LayoutParams(11.dp, 11.dp).also {
                        it.setMargins(7.dp, 0, 7.dp, 0)
                    }
                    background = ContextCompat.getDrawable(context, R.drawable.dot_empty)
                }
                dots.add(d); addView(d)
            }
        }
    }

    private fun refreshDots() = dots.forEachIndexed { i, v ->
        v.background = ContextCompat.getDrawable(this,
            if (i < pinBuffer.length) R.drawable.dot_filled else R.drawable.dot_empty)
    }

    private fun numpad(onDigit: (Char) -> Unit, onDelete: () -> Unit): GridLayout {
        return GridLayout(this).apply {
            columnCount = 3; rowCount = 4
            setPadding(4.dp, 0, 4.dp, 0)
            listOf("1","2","3","4","5","6","7","8","9","","0","⌫").forEachIndexed { i, label ->
                val t = tv(label, 22f, if (label == "⌫") MUTED else WHITE).apply {
                    typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                    gravity = Gravity.CENTER
                    layoutParams = GridLayout.LayoutParams().apply {
                        width = 76.dp; height = 64.dp
                        setMargins(7.dp, 7.dp, 7.dp, 7.dp)
                        columnSpec = GridLayout.spec(i % 3)
                        rowSpec = GridLayout.spec(i / 3)
                    }
                    if (label.isNotEmpty()) {
                        background = GradientDrawable().apply {
                            cornerRadius = 20.dp.toFloat()
                            setColor(CARD)
                            setStroke(1.dp, Color.parseColor("#222B3D"))
                        }
                        isClickable = true; isFocusable = true
                        setOnClickListener { if (label == "⌫") onDelete() else onDigit(label[0]) }
                    }
                }
                addView(t)
            }
        }
    }

    private fun showPinCreate() {
        pinBuffer.clear(); var first = ""; var phase = 0
        val status = tv("Create your 6-digit security PIN", 12f, MUTED)
        val l = root()
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            background = panel()
            setPadding(26.dp, 30.dp, 26.dp, 28.dp)
        }
        box.addView(brand("PRIVATE • ENCRYPTED BACKUP"))
        box.addView(space(16))
        box.addView(dotRow())
        box.addView(status)
        box.addView(space(12))
        box.addView(numpad(
            onDigit = { c ->
                if (pinBuffer.length < 6) {
                    pinBuffer.append(c); refreshDots()
                    if (pinBuffer.length == 6) {
                        if (phase == 0) {
                            first = pinBuffer.toString(); pinBuffer.clear(); refreshDots(); phase = 1
                            status.text = "Confirm the same PIN"; status.setTextColor(MUTED)
                        } else if (pinBuffer.toString() == first) {
                            pin.setPin(pinBuffer.toString()); openVault()
                        } else {
                            pinBuffer.clear(); first = ""; phase = 0; refreshDots()
                            status.text = "PINs do not match — try again"; status.setTextColor(RED)
                        }
                    }
                }
            },
            onDelete = { if (pinBuffer.isNotEmpty()) { pinBuffer.deleteCharAt(pinBuffer.lastIndex); refreshDots() } }
        ))
        l.addView(box, LinearLayout.LayoutParams(-1, -2).apply {
            setMargins(18.dp, 22.dp, 18.dp, 22.dp)
        })
        l.addView(tv("Your call records stay protected on this device", 10f, MUTED))
        setContentView(l)
    }

    private fun showPinEnter() {
        pinBuffer.clear()
        val status = tv("Enter your 6-digit security PIN", 12f, MUTED)
        val l = root()
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            background = panel()
            setPadding(26.dp, 30.dp, 26.dp, 28.dp)
        }
        box.addView(brand("PRIVATE • ENCRYPTED BACKUP"))
        box.addView(space(16)); box.addView(dotRow()); box.addView(status); box.addView(space(12))
        box.addView(numpad(
            onDigit = { c ->
                if (pinBuffer.length < 6) {
                    pinBuffer.append(c); refreshDots()
                    if (pinBuffer.length == 6) {
                        if (pin.verify(pinBuffer.toString())) openVault()
                        else {
                            pinBuffer.clear(); refreshDots()
                            status.text = "Incorrect PIN — try again"; status.setTextColor(RED)
                        }
                    }
                }
            },
            onDelete = { if (pinBuffer.isNotEmpty()) { pinBuffer.deleteCharAt(pinBuffer.lastIndex); refreshDots() } }
        ))
        l.addView(box, LinearLayout.LayoutParams(-1, -2).apply {
            setMargins(18.dp, 22.dp, 18.dp, 22.dp)
        })
        l.addView(tv("Protected access • Auto-lock after inactivity", 10f, MUTED))
        setContentView(l)
    }

    private fun openVault() {
        unlocked = true; last = SystemClock.elapsedRealtime()
        requestPerms(); requestBatteryExemption(); ScheduleHelper.schedule(this)

        val store = CallStore(this)
        val mins = ScheduleHelper.minutesUntilNext()
        val next = ScheduleHelper.nextSlotLabel()
        val pending = store.readSince(store.getLastCheckTime()).size

        val scroll = ScrollView(this).apply { setBackgroundColor(BG) }
        val l = root(center = false).apply {
            setPadding(18.dp, 22.dp, 18.dp, 30.dp)
        }
        scroll.addView(l)

        val header = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(brand(compact = true), LinearLayout.LayoutParams(0, -2, 1f))
            addView(tv("●  SECURE", 9f, GREEN).apply {
                background = rounded(Color.parseColor("#10251F"), 18)
                setPadding(12.dp, 8.dp, 12.dp, 8.dp)
                typeface = Typeface.DEFAULT_BOLD
            })
        }
        l.addView(header)
        l.addView(space(18))
        l.addView(divider())

        val clockCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            background = panel(); setPadding(20.dp, 24.dp, 20.dp, 22.dp)
        }
        clockCard.addView(tv("CURRENT TIME", 9f, MUTED).also { it.letterSpacing = 0.2f })
        clockCard.addView(TextClock(this).apply {
            format12Hour = null; format24Hour = "HH:mm:ss"
            textSize = 46f; typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setTextColor(GOLD_SOFT); gravity = Gravity.CENTER
            setPadding(0, 3.dp, 0, 0)
        })
        clockCard.addView(TextClock(this).apply {
            format12Hour = null; format24Hour = "EEEE  •  d MMM yyyy"
            textSize = 11f; setTextColor(MUTED); gravity = Gravity.CENTER
        })
        l.addView(clockCard)

        l.addView(space(14))
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(infoCard("NEXT CHECK", next, "in ${mins / 60}h ${mins % 60}m", "◷"))
        row.addView(space(10))
        row.addView(infoCard("PENDING", "$pending", "calls to send", "↗"))
        l.addView(row)

        l.addView(space(14))
        val schedule = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; background = rounded(CARD, 18)
            setPadding(18.dp, 16.dp, 18.dp, 16.dp)
        }
        schedule.addView(tv("AUTOMATIC BACKUP", 9f, GOLD).also { it.letterSpacing = 0.18f })
        schedule.addView(tv("Every 4 hours", 16f, WHITE).also {
            it.gravity = Gravity.LEFT; it.typeface = Typeface.DEFAULT_BOLD
            it.setPadding(0, 5.dp, 0, 3.dp)
        })
        schedule.addView(tv("00:00   04:00   08:00   12:00   16:00   20:00", 10f, MUTED).also {
            it.gravity = Gravity.LEFT
        })
        l.addView(schedule)

        l.addView(space(22))
        val lock = TextView(this).apply {
            text = "  LOCK VAULT  〉"
            textSize = 11f; setTextColor(BG); gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD; letterSpacing = 0.12f
            background = goldButton(); setPadding(0, 15.dp, 0, 15.dp)
            setOnClickListener { unlocked = false; showPinEnter() }
        }
        l.addView(lock, LinearLayout.LayoutParams(-1, -2))
        l.addView(space(10))
        l.addView(tv("Encrypted local vault  •  Auto-lock 30 seconds", 9f, MUTED))

        setContentView(scroll)
    }

    private fun infoCard(label: String, value: String, sub: String, icon: String) =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(CARD, 18)
            setPadding(16.dp, 16.dp, 16.dp, 15.dp)
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            addView(tv(icon, 18f, GOLD))
            addView(tv(label, 8f, MUTED).also { it.letterSpacing = 0.14f; it.setPadding(0, 7.dp, 0, 0) })
            addView(tv(value, 22f, WHITE).also {
                it.typeface = Typeface.create("sans-serif", Typeface.BOLD); it.setPadding(0, 2.dp, 0, 0)
            })
            addView(tv(sub, 9f, MUTED).also { it.setPadding(0, 2.dp, 0, 0) })
        }

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        cornerRadius = radius.dp.toFloat(); setColor(color)
    }

    private fun tv(text: String, size: Float, color: Int) = TextView(this).apply {
        this.text = text; textSize = size; setTextColor(color); gravity = Gravity.CENTER
    }

    private fun space(dp: Int) = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(-1, dp.dp)
    }

    private fun divider() = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(-1, 1.dp).also { it.setMargins(0, 0, 0, 18.dp) }
        setBackgroundColor(Color.parseColor("#20283A"))
    }

    private val Int.dp get() = (this * resources.displayMetrics.density).toInt()

    private fun requestPerms() {
        val needed = mutableListOf(
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_PHONE_STATE
        ).filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toMutableList()
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) needed.add(Manifest.permission.POST_NOTIFICATIONS)
        if (needed.isNotEmpty()) ActivityCompat.requestPermissions(this, needed.toTypedArray(), 10)
    }

    private fun requestBatteryExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(PowerManager::class.java)
            if (!pm.isIgnoringBatteryOptimizations(packageName))
                startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    .apply { data = Uri.parse("package:$packageName") })
        }
    }

    override fun onUserInteraction() { super.onUserInteraction(); if (unlocked) last = SystemClock.elapsedRealtime() }

    override fun onResume() {
        super.onResume()
        if (unlocked && SystemClock.elapsedRealtime() - last >= 30_000) {
            unlocked = false; showPinEnter()
        }
    }
}
