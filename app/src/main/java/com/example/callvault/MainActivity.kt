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

    private val LED   = Color.parseColor("#00E676")
    private val BG    = Color.parseColor("#050505")
    private val MUTED = Color.parseColor("#3A3A3A")
    private val WHITE = Color.parseColor("#EEEEEE")
    private val RED   = Color.parseColor("#FF5252")

    private val pinBuffer = StringBuilder()
    private val dots = mutableListOf<View>()

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        if (Build.VERSION.SDK_INT >= 21) window.statusBarColor = BG
        NotificationHelper.createChannels(this)
        pin = SecurePinStore(this)
        if (pin.hasPin()) showPinEnter() else showPinCreate()
    }

    // ── Root ──────────────────────────────────────────────────────
    private fun root(center: Boolean = true) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity     = if (center) Gravity.CENTER else Gravity.TOP or Gravity.CENTER_HORIZONTAL
        setBackgroundColor(BG)
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
    }

    private fun logo(sub: String = "") = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
        addView(tv("JEEVI", 30f, WHITE).also {
            it.letterSpacing = 0.35f
            it.typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        })
        if (sub.isNotEmpty()) addView(tv(sub, 10f, MUTED).also {
            it.letterSpacing = 0.2f; it.setPadding(0, 4.dp, 0, 0)
        })
    }

    // ── PIN dots ──────────────────────────────────────────────────
    private fun dotRow(): LinearLayout {
        dots.clear()
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER
            setPadding(0, 32.dp, 0, 32.dp)
            repeat(6) {
                val d = View(context).apply {
                    layoutParams = LinearLayout.LayoutParams(14.dp, 14.dp)
                        .also { it.setMargins(8.dp, 0, 8.dp, 0) }
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

    // ── Numpad ────────────────────────────────────────────────────
    private fun numpad(onDigit: (Char) -> Unit, onDelete: () -> Unit): GridLayout {
        return GridLayout(this).apply {
            columnCount = 3; rowCount = 4; setPadding(16.dp, 0, 16.dp, 0)
            listOf("1","2","3","4","5","6","7","8","9","","0","⌫").forEachIndexed { i, label ->
                addView(tv(label, 26f, if (label == "⌫") MUTED else LED).also { t ->
                    t.typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
                    t.layoutParams = GridLayout.LayoutParams().apply {
                        width  = 72.dp; height = 72.dp; setMargins(10.dp, 10.dp, 10.dp, 10.dp)
                        columnSpec = GridLayout.spec(i % 3); rowSpec = GridLayout.spec(i / 3)
                    }
                    if (label.isNotEmpty()) {
                        t.background = ContextCompat.getDrawable(context, R.drawable.key_bg)
                        t.isClickable = true; t.isFocusable = true
                        t.setOnClickListener { if (label == "⌫") onDelete() else onDigit(label[0]) }
                    }
                })
            }
        }
    }

    // ── PIN create ────────────────────────────────────────────────
    private fun showPinCreate() {
        pinBuffer.clear(); var first = ""; var phase = 0
        val status = tv("Create a 6-digit PIN", 13f, MUTED)
        val l = root(); l.addView(logo("SECURE BACKUP")); l.addView(space(24))
        l.addView(dotRow()); l.addView(status); l.addView(space(16))
        l.addView(numpad(
            onDigit = { c ->
                if (pinBuffer.length < 6) {
                    pinBuffer.append(c); refreshDots()
                    if (pinBuffer.length == 6) {
                        if (phase == 0) {
                            first = pinBuffer.toString()
                            pinBuffer.clear(); refreshDots(); phase = 1
                            status.text = "Confirm your PIN"; status.setTextColor(MUTED)
                        } else {
                            if (pinBuffer.toString() == first) { pin.setPin(pinBuffer.toString()); openVault() }
                            else { pinBuffer.clear(); first = ""; phase = 0; refreshDots()
                                status.text = "Mismatch — try again"; status.setTextColor(RED) }
                        }
                    }
                }
            },
            onDelete = { if (pinBuffer.isNotEmpty()) { pinBuffer.deleteCharAt(pinBuffer.lastIndex); refreshDots() } }
        ))
        setContentView(l)
    }

    // ── PIN enter ─────────────────────────────────────────────────
    private fun showPinEnter() {
        pinBuffer.clear()
        val status = tv("Enter your PIN", 13f, MUTED)
        val l = root(); l.addView(logo("SECURE BACKUP")); l.addView(space(24))
        l.addView(dotRow()); l.addView(status); l.addView(space(16))
        l.addView(numpad(
            onDigit = { c ->
                if (pinBuffer.length < 6) {
                    pinBuffer.append(c); refreshDots()
                    if (pinBuffer.length == 6) {
                        if (pin.verify(pinBuffer.toString())) openVault()
                        else { pinBuffer.clear(); refreshDots()
                            status.text = "Incorrect PIN"; status.setTextColor(RED) }
                    }
                }
            },
            onDelete = { if (pinBuffer.isNotEmpty()) { pinBuffer.deleteCharAt(pinBuffer.lastIndex); refreshDots() } }
        ))
        setContentView(l)
    }

    // ── Vault / Dashboard ─────────────────────────────────────────
    private fun openVault() {
        unlocked = true; last = SystemClock.elapsedRealtime()
        requestPerms(); requestBatteryExemption()
        ScheduleHelper.schedule(this)

        val store = CallStore(this)
        val mins  = ScheduleHelper.minutesUntilNext()
        val next  = ScheduleHelper.nextSlotLabel()
        val pending = store.readSince(store.getLastCheckTime()).size

        val l = root(center = false).also { it.setPadding(24.dp, 48.dp, 24.dp, 32.dp) }

        // Header
        l.addView(logo()); l.addView(divider())

        // Live digital clock
        l.addView(android.widget.TextClock(this).apply {
            format12Hour = null; format24Hour = "HH:mm:ss"
            textSize = 60f; typeface = Typeface.MONOSPACE
            setTextColor(LED); gravity = Gravity.CENTER
        })
        l.addView(android.widget.TextClock(this).apply {
            format12Hour = null; format24Hour = "EEEE, d MMM yyyy"
            textSize = 12f; setTextColor(MUTED); gravity = Gravity.CENTER
        })

        l.addView(space(32))

        // Info cards
        l.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER
            addView(infoCard("NEXT CHECK", next, "in ${mins / 60}h ${mins % 60}m"))
            addView(space(12))
            addView(infoCard("PENDING", "$pending", "calls to send"))
        })

        l.addView(space(16))

        // Schedule info text
        l.addView(tv("Checks every 4h  ·  00:00  04:00  08:00  12:00  16:00  20:00", 10f, MUTED).also {
            it.setPadding(0, 0, 0, 0)
        })

        l.addView(space(48))
        l.addView(tv("LOCK", 11f, MUTED).also { lk ->
            lk.letterSpacing = 0.25f; lk.setPadding(32.dp, 16.dp, 32.dp, 16.dp)
            lk.setOnClickListener { unlocked = false; showPinEnter() }
        })

        setContentView(l)
    }

    // ── Widgets ───────────────────────────────────────────────────
    private fun tv(text: String, size: Float, color: Int) = TextView(this).apply {
        this.text = text; textSize = size; setTextColor(color); gravity = Gravity.CENTER
    }

    private fun infoCard(label: String, value: String, sub: String) =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
            background  = ContextCompat.getDrawable(context, R.drawable.card_bg)
            setPadding(20.dp, 18.dp, 20.dp, 18.dp)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            addView(tv(label, 9f, MUTED).also { it.letterSpacing = 0.15f })
            addView(tv(value, 26f, LED).also { it.typeface = Typeface.MONOSPACE; it.setPadding(0, 4.dp, 0, 0) })
            addView(tv(sub, 10f, MUTED).also { it.setPadding(0, 2.dp, 0, 0) })
        }

    private fun space(dp: Int) = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp.dp)
    }

    private fun divider() = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
            .also { it.setMargins(0, 14.dp, 0, 22.dp) }
        setBackgroundColor(Color.parseColor("#1C1C1C"))
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
            != PackageManager.PERMISSION_GRANTED)
            needed.add(Manifest.permission.POST_NOTIFICATIONS)
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
        if (unlocked && SystemClock.elapsedRealtime() - last >= 30_000) { unlocked = false; showPinEnter() }
    }
}
