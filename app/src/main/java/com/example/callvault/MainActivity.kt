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

    private val BG = Color.parseColor("#07080C")
    private val SURFACE = Color.parseColor("#10121A")
    private val CARD = Color.parseColor("#151824")
    private val ACCENT = Color.parseColor("#C9A45C")
    private val ACCENT_SOFT = Color.parseColor("#3A3020")
    private val WHITE = Color.parseColor("#F7F5F0")
    private val MUTED = Color.parseColor("#858B9A")
    private val RED = Color.parseColor("#FF6B6B")

    private val pinBuffer = StringBuilder()
    private val dots = mutableListOf<View>()

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        if (Build.VERSION.SDK_INT >= 21) {
            window.statusBarColor = BG
            window.navigationBarColor = BG
        }
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

    private fun brand(sub: String = "") = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        val icon = ImageView(context).apply {
            setImageResource(R.drawable.ic_callvault_logo)
            layoutParams = LinearLayout.LayoutParams(74.dp, 74.dp)
        }
        addView(icon)
        addView(tv("CALLVAULT", 22f, WHITE).also {
            it.typeface = Typeface.create("sans-serif", Typeface.BOLD)
            it.letterSpacing = .18f
            it.setPadding(0, 10.dp, 0, 0)
        })
        if (sub.isNotEmpty()) addView(tv(sub, 10f, MUTED).also {
            it.letterSpacing = .16f
            it.setPadding(0, 6.dp, 0, 0)
        })
    }

    private fun dotRow(): LinearLayout {
        dots.clear()
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 24.dp, 0, 24.dp)
            repeat(6) {
                val d = View(context).apply {
                    layoutParams = LinearLayout.LayoutParams(12.dp, 12.dp).also {
                        it.setMargins(7.dp, 0, 7.dp, 0)
                    }
                    background = ContextCompat.getDrawable(context, R.drawable.dot_empty)
                }
                dots.add(d); addView(d)
            }
        }
    }

    private fun refreshDots() = dots.forEachIndexed { i, v ->
        v.background = ContextCompat.getDrawable(
            this, if (i < pinBuffer.length) R.drawable.dot_filled else R.drawable.dot_empty
        )
    }

    private fun numpad(onDigit: (Char) -> Unit, onDelete: () -> Unit): GridLayout {
        return GridLayout(this).apply {
            columnCount = 3; rowCount = 4
            setPadding(18.dp, 0, 18.dp, 0)
            listOf("1","2","3","4","5","6","7","8","9","","0","⌫").forEachIndexed { i, label ->
                val t = tv(label, 22f, if (label == "⌫") MUTED else WHITE).apply {
                    typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                    gravity = Gravity.CENTER
                    layoutParams = GridLayout.LayoutParams().apply {
                        width = 70.dp; height = 64.dp
                        setMargins(8.dp, 7.dp, 8.dp, 7.dp)
                        columnSpec = GridLayout.spec(i % 3)
                        rowSpec = GridLayout.spec(i / 3)
                    }
                    if (label.isNotEmpty()) {
                        background = ContextCompat.getDrawable(context, R.drawable.key_bg)
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
        val status = tv("Create your 6-digit security PIN", 13f, MUTED)
        val l = root()
        l.addView(brand("PRIVATE • ENCRYPTED BACKUP"))
        l.addView(space(18))
        l.addView(dotRow()); l.addView(status); l.addView(space(10))
        l.addView(numpad(
            onDigit = { c ->
                if (pinBuffer.length < 6) {
                    pinBuffer.append(c); refreshDots()
                    if (pinBuffer.length == 6) {
                        if (phase == 0) {
                            first = pinBuffer.toString(); pinBuffer.clear(); refreshDots(); phase = 1
                            status.text = "Confirm your security PIN"
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
        setContentView(l)
    }

    private fun showPinEnter() {
        pinBuffer.clear()
        val status = tv("Enter your 6-digit security PIN", 13f, MUTED)
        val l = root()
        l.addView(brand("SECURE ACCESS"))
        l.addView(space(18)); l.addView(dotRow()); l.addView(status); l.addView(space(10))
        l.addView(numpad(
            onDigit = { c ->
                if (pinBuffer.length < 6) {
                    pinBuffer.append(c); refreshDots()
                    if (pinBuffer.length == 6) {
                        if (pin.verify(pinBuffer.toString())) openVault()
                        else {
                            pinBuffer.clear(); refreshDots()
                            status.text = "Incorrect PIN"; status.setTextColor(RED)
                        }
                    }
                }
            },
            onDelete = { if (pinBuffer.isNotEmpty()) { pinBuffer.deleteCharAt(pinBuffer.lastIndex); refreshDots() } }
        ))
        setContentView(l)
    }

    private fun openVault() {
        unlocked = true; last = SystemClock.elapsedRealtime()
        requestPerms(); requestBatteryExemption(); ScheduleHelper.schedule(this)

        val store = CallStore(this)
        val mins = ScheduleHelper.minutesUntilNext()
        val next = ScheduleHelper.nextSlotLabel()
        val pending = store.readSince(store.getLastCheckTime()).size

        val l = root(false).apply { setPadding(22.dp, 34.dp, 22.dp, 26.dp) }

        l.addView(LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(ImageView(context).apply {
                setImageResource(R.drawable.ic_callvault_logo)
                layoutParams = LinearLayout.LayoutParams(48.dp, 48.dp)
            })
            addView(tv("CALLVAULT", 18f, WHITE).also {
                it.typeface = Typeface.create("sans-serif", Typeface.BOLD)
                it.letterSpacing = .14f
                it.layoutParams = LinearLayout.LayoutParams(0, -2, 1f).also { p -> p.setMargins(12.dp, 0, 0, 0) }
            })
            addView(tv("SECURED", 9f, ACCENT).also { it.letterSpacing = .16f })
        })
        l.addView(divider())

        l.addView(tv("SECURE VAULT", 10f, ACCENT).also {
            it.letterSpacing = .2f; it.setPadding(0, 6.dp, 0, 10.dp)
        })
        l.addView(android.widget.TextClock(this).apply {
            format12Hour = null; format24Hour = "HH:mm"
            textSize = 58f; typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            setTextColor(WHITE); gravity = Gravity.CENTER
        })
        l.addView(android.widget.TextClock(this).apply {
            format12Hour = null; format24Hour = "EEEE, d MMM yyyy"
            textSize = 12f; setTextColor(MUTED); gravity = Gravity.CENTER
        })

        l.addView(space(28))
        l.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER
            addView(infoCard("NEXT CHECK", next, "in ${mins / 60}h ${mins % 60}m"))
            addView(space(10))
            addView(infoCard("PENDING", "$pending", "calls to send"))
        })
        l.addView(space(18))
        l.addView(tv("AUTOMATED BACKUP", 9f, MUTED).also { it.letterSpacing = .16f })
        l.addView(tv("Every 4 hours  •  00:00  04:00  08:00  12:00  16:00  20:00", 11f, WHITE)
            .also { it.setPadding(0, 8.dp, 0, 0) })
        l.addView(space(30))

        val lock = tv("  LOCK VAULT  ", 11f, ACCENT).apply {
            letterSpacing = .18f; gravity = Gravity.CENTER
            background = ContextCompat.getDrawable(context, R.drawable.card_bg)
            setPadding(20.dp, 14.dp, 20.dp, 14.dp)
            setOnClickListener { unlocked = false; showPinEnter() }
        }
        l.addView(lock, LinearLayout.LayoutParams(-1, -2))
        l.addView(space(8))
        l.addView(tv("Your call records stay protected on this device.", 9f, MUTED))

        setContentView(l)
    }

    private fun tv(text: String, size: Float, color: Int) = TextView(this).apply {
        this.text = text; textSize = size; setTextColor(color); gravity = Gravity.CENTER
    }

    private fun infoCard(label: String, value: String, sub: String) =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
            background = ContextCompat.getDrawable(context, R.drawable.card_bg)
            setPadding(16.dp, 16.dp, 16.dp, 16.dp)
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            addView(tv(label, 9f, MUTED).also { it.letterSpacing = .14f })
            addView(tv(value, 23f, ACCENT).also {
                it.typeface = Typeface.create("sans-serif", Typeface.BOLD)
                it.setPadding(0, 5.dp, 0, 0)
            })
            addView(tv(sub, 10f, MUTED).also { it.setPadding(0, 2.dp, 0, 0) })
        }

    private fun space(dp: Int) = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(-1, dp.dp)
    }

    private fun divider() = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(-1, 1).also { it.setMargins(0, 14.dp, 0, 22.dp) }
        setBackgroundColor(Color.parseColor("#242735"))
    }

    private val Int.dp get() = (this * resources.displayMetrics.density).toInt()

    private fun requestPerms() {
        val needed = mutableListOf(
            Manifest.permission.READ_CALL_LOG, Manifest.permission.READ_PHONE_STATE
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

    override fun onUserInteraction() {
        super.onUserInteraction(); if (unlocked) last = SystemClock.elapsedRealtime()
    }

    override fun onResume() {
        super.onResume()
        if (unlocked && SystemClock.elapsedRealtime() - last >= 30_000) {
            unlocked = false; showPinEnter()
        }
    }
}
