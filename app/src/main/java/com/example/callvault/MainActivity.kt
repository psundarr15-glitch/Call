package com.example.callvault

import android.Manifest
import android.app.TimePickerDialog
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

    private val BG = Color.parseColor("#070812")
    private val CARD = Color.parseColor("#111528")
    private val CYAN = Color.parseColor("#22D3EE")
    private val PURPLE = Color.parseColor("#8B5CF6")
    private val GREEN = Color.parseColor("#34D399")
    private val AMBER = Color.parseColor("#FBBF24")
    private val RED = Color.parseColor("#FB7185")
    private val WHITE = Color.parseColor("#F8FAFC")
    private val SECONDARY = Color.parseColor("#A7B0C5")
    private val MUTED = Color.parseColor("#667085")
    private val DIVIDER = Color.parseColor("#252B43")

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

    private fun root(scroll: Boolean = false): LinearLayout {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(22.dp, 20.dp, 22.dp, 28.dp)
            setBackgroundColor(BG)
        }
        if (!scroll) {
            content.layoutParams = LinearLayout.LayoutParams(-1, -1)
            return content
        }
        val scrollView = ScrollView(this).apply {
            setBackgroundColor(BG)
            isFillViewport = true
            addView(content, ScrollView.LayoutParams(-1, -2))
        }
        val wrapper = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(BG)
            addView(scrollView, LinearLayout.LayoutParams(-1, -1))
        }
        // The wrapper exposes the scroll view through a tag so callers can add content.
        wrapper.tag = content
        return wrapper
    }

    private fun contentOf(wrapper: LinearLayout): LinearLayout = (wrapper.tag as? LinearLayout) ?: wrapper

    private fun brand(subtitle: String = "") = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        addView(ImageView(context).apply {
            setImageResource(R.drawable.ic_callvault_logo)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            layoutParams = LinearLayout.LayoutParams(76.dp, 76.dp)
        })
        addView(TextView(context).apply {
            text = "Jeevi"
            textSize = 30f
            letterSpacing = 0.08f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setTextColor(WHITE)
            gravity = Gravity.CENTER
        })
        if (subtitle.isNotEmpty()) addView(TextView(context).apply {
            text = subtitle.uppercase()
            textSize = 10f
            letterSpacing = 0.18f
            setTextColor(MUTED)
            gravity = Gravity.CENTER
            setPadding(0, 5.dp, 0, 0)
        })
    }

    private fun sectionTitle(title: String, subtitle: String = "") = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        addView(TextView(context).apply {
            text = title
            textSize = 22f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setTextColor(WHITE)
        })
        if (subtitle.isNotEmpty()) addView(TextView(context).apply {
            text = subtitle
            textSize = 12f
            setTextColor(SECONDARY)
            setPadding(0, 4.dp, 0, 0)
        })
    }

    private fun dotRow(): LinearLayout {
        dots.clear()
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 22.dp, 0, 22.dp)
            repeat(6) {
                val d = View(context).apply {
                    layoutParams = LinearLayout.LayoutParams(13.dp, 13.dp).also { it.setMargins(7.dp, 0, 7.dp, 0) }
                    background = ContextCompat.getDrawable(context, R.drawable.dot_empty)
                }
                dots.add(d)
                addView(d)
            }
        }
    }

    private fun refreshDots() = dots.forEachIndexed { i, v ->
        v.background = ContextCompat.getDrawable(this, if (i < pinBuffer.length) R.drawable.dot_filled else R.drawable.dot_empty)
    }

    private fun numpad(onDigit: (Char) -> Unit, onDelete: () -> Unit): GridLayout {
        val keys = listOf("1","2","3","4","5","6","7","8","9","","0","⌫")
        return GridLayout(this).apply {
            columnCount = 3
            rowCount = 4
            setPadding(4.dp, 0, 4.dp, 0)
            keys.forEachIndexed { i, label ->
                addView(TextView(context).apply {
                    text = label
                    textSize = if (label == "⌫") 22f else 22f
                    typeface = Typeface.create("sans-serif", Typeface.BOLD)
                    setTextColor(if (label == "⌫") SECONDARY else WHITE)
                    gravity = Gravity.CENTER
                    layoutParams = GridLayout.LayoutParams().apply {
                        width = 84.dp
                        height = 66.dp
                        setMargins(7.dp, 6.dp, 7.dp, 6.dp)
                        columnSpec = GridLayout.spec(i % 3)
                        rowSpec = GridLayout.spec(i / 3)
                    }
                    if (label.isNotEmpty()) {
                        background = ContextCompat.getDrawable(context, R.drawable.key_bg)
                        isClickable = true
                        isFocusable = true
                        setOnClickListener { if (label == "⌫") onDelete() else onDigit(label[0]) }
                    }
                })
            }
        }
    }

    private fun pinPanel(title: String, hint: TextView, keypad: GridLayout): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        background = ContextCompat.getDrawable(context, R.drawable.card_bg)
        setPadding(14.dp, 20.dp, 14.dp, 18.dp)
        addView(TextView(context).apply {
            text = title
            textSize = 18f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setTextColor(WHITE)
            gravity = Gravity.CENTER
        })
        addView(dotRow())
        addView(hint)
        addView(keypad)
    }

    private fun showPinCreate() {
        pinBuffer.clear()
        var first = ""
        var phase = 0
        val status = tv("Choose a 6-digit PIN", 12f, SECONDARY)
        val keypad = numpad(
            onDigit = { c ->
                if (pinBuffer.length < 6) {
                    pinBuffer.append(c); refreshDots()
                    if (pinBuffer.length == 6) {
                        if (phase == 0) {
                            first = pinBuffer.toString(); pinBuffer.clear(); refreshDots(); phase = 1
                            status.text = "Confirm your PIN"
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
        )
        val l = root()
        l.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        l.addView(space(18))
        l.addView(brand("Private call backup"))
        l.addView(space(22))
        l.addView(pinPanel("Create your PIN", status, keypad))
        l.addView(space(16))
        l.addView(tv("Your PIN protects the call vault on this device.", 11f, MUTED))
        setContentView(l)
    }

    private fun showPinEnter() {
        pinBuffer.clear()
        val status = tv("Enter your 6-digit PIN", 12f, SECONDARY)
        val keypad = numpad(
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
        )
        val l = root()
        l.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        l.addView(space(18))
        l.addView(brand("Secure call vault"))
        l.addView(space(22))
        l.addView(pinPanel("Welcome back", status, keypad))
        l.addView(space(16))
        l.addView(tv("Your vault is locked.", 11f, MUTED))
        setContentView(l)
    }

    private fun openVault() {
        unlocked = true
        last = SystemClock.elapsedRealtime()
        requestPerms()
        requestBatteryExemption()
        ScheduleHelper.schedule(this)
        val alarmStore = AlarmTimeStore(this)
        val mins = ScheduleHelper.minutesUntil(this)

        val wrapper = root(scroll = true)
        val l = contentOf(wrapper)
        l.gravity = Gravity.TOP

        l.addView(topBar())
        l.addView(space(20))
        l.addView(greeting())
        l.addView(space(18))
        l.addView(clockCard(alarmStore))
        l.addView(space(14))

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        row.addView(infoCard("NEXT BACKUP", alarmStore.label(), "in ${mins / 60}h ${mins % 60}m", AMBER))
        row.addView(space(10))
        row.addView(infoCard("CALLS SAVED", "${CallStore(this).readAll().size}", "stored securely", CYAN))
        l.addView(row)
        l.addView(space(14))
        l.addView(statusCard())
        l.addView(space(20))
        l.addView(lockButton())
        l.addView(space(8))
        l.addView(tv("Jeevi • private by design", 10f, MUTED))

        setContentView(wrapper)
    }

    private fun topBar() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        addView(ImageView(context).apply {
            setImageResource(R.drawable.ic_callvault_logo)
            layoutParams = LinearLayout.LayoutParams(42.dp, 42.dp)
        })
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(10.dp, 0, 0, 0)
            addView(TextView(context).apply {
                text = "Jeevi"
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(WHITE)
            })
            addView(TextView(context).apply {
                text = "CALL VAULT"
                textSize = 9f
                letterSpacing = 0.18f
                setTextColor(MUTED)
            })
        }, LinearLayout.LayoutParams(0, -2, 1f))
        addView(pill("●  SECURE", GREEN))
    }

    private fun greeting() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        addView(TextView(context).apply {
            text = "Your vault is ready"
            textSize = 25f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setTextColor(WHITE)
        })
        addView(TextView(context).apply {
            text = "Automatic backup is scheduled and protected."
            textSize = 12f
            setTextColor(SECONDARY)
            setPadding(0, 5.dp, 0, 0)
        })
    }

    private fun clockCard(alarmStore: AlarmTimeStore) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        background = ContextCompat.getDrawable(context, R.drawable.card_bg)
        setPadding(18.dp, 20.dp, 18.dp, 20.dp)
        addView(tv("LOCAL TIME", 9f, MUTED).also { it.letterSpacing = 0.2f })
        addView(TextClock(context).apply {
            format12Hour = null
            format24Hour = "HH:mm:ss"
            textSize = 50f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setTextColor(CYAN)
            gravity = Gravity.CENTER
            setPadding(0, 2.dp, 0, 0)
        })
        addView(TextClock(context).apply {
            format12Hour = null
            format24Hour = "EEEE, d MMM yyyy"
            textSize = 11f
            setTextColor(SECONDARY)
            gravity = Gravity.CENTER
        })
        addView(space(12))
        val alarm = TextView(context).apply {
            text = "⏰  ${alarmStore.label()}"
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(AMBER)
            gravity = Gravity.CENTER
            setPadding(16.dp, 10.dp, 16.dp, 10.dp)
            background = ContextCompat.getDrawable(context, R.drawable.key_bg)
        }
        val hint = tv("Tap to change backup time", 10f, MUTED)
        alarm.setOnClickListener {
            last = SystemClock.elapsedRealtime()
            TimePickerDialog(this@MainActivity, { _, h, m ->
                alarmStore.set(h, m)
                alarm.text = "⏰  ${alarmStore.label()}"
                ScheduleHelper.scheduleAt(this@MainActivity, h, m)
                val mins = ScheduleHelper.minutesUntil(this@MainActivity)
                hint.text = "Backup scheduled • ${mins / 60}h ${mins % 60}m from now"
                hint.setTextColor(GREEN)
                toast("Backup alarm set for ${alarmStore.label()}")
            }, alarmStore.getHour(), alarmStore.getMinute(), true).show()
        }
        addView(alarm)
        addView(hint)
    }

    private fun statusCard() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        background = ContextCompat.getDrawable(context, R.drawable.card_bg)
        setPadding(16.dp, 15.dp, 16.dp, 15.dp)
        addView(View(context).apply {
            background = ContextCompat.getDrawable(context, R.drawable.dot_filled)
            layoutParams = LinearLayout.LayoutParams(12.dp, 12.dp)
        })
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12.dp, 0, 0, 0)
            addView(TextView(context).apply {
                text = "Backup protection is active"
                textSize = 13f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(WHITE)
            })
            addView(TextView(context).apply {
                text = "Call data stays on-device until your scheduled backup runs."
                textSize = 10f
                setTextColor(SECONDARY)
                setPadding(0, 3.dp, 0, 0)
            })
        })
    }

    private fun lockButton() = TextView(this).apply {
        text = "LOCK VAULT"
        textSize = 11f
        letterSpacing = 0.16f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(SECONDARY)
        gravity = Gravity.CENTER
        background = ContextCompat.getDrawable(context, R.drawable.key_bg)
        setPadding(16.dp, 14.dp, 16.dp, 14.dp)
        isClickable = true
        setOnClickListener { unlocked = false; showPinEnter() }
    }

    private fun infoCard(label: String, value: String, sub: String, accent: Int) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_VERTICAL
        background = ContextCompat.getDrawable(context, R.drawable.card_bg)
        setPadding(15.dp, 16.dp, 15.dp, 16.dp)
        layoutParams = LinearLayout.LayoutParams(0, 112.dp, 1f)
        addView(tv(label, 8f, MUTED).also { it.gravity = Gravity.START; it.letterSpacing = 0.12f })
        addView(tv(value, 19f, accent).also { it.typeface = Typeface.DEFAULT_BOLD; it.gravity = Gravity.START; it.setPadding(0, 8.dp, 0, 0) })
        addView(tv(sub, 9f, SECONDARY).also { it.gravity = Gravity.START; it.setPadding(0, 3.dp, 0, 0) })
    }

    private fun pill(text: String, color: Int) = TextView(this).apply {
        this.text = text
        textSize = 9f
        letterSpacing = 0.08f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(color)
        gravity = Gravity.CENTER
        setPadding(11.dp, 8.dp, 11.dp, 8.dp)
        background = ContextCompat.getDrawable(context, R.drawable.key_bg)
    }

    private fun tv(text: String, size: Float, color: Int) = TextView(this).apply {
        this.text = text
        textSize = size
        setTextColor(color)
        gravity = Gravity.CENTER
    }

    private fun space(dp: Int) = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp.dp)
    }

    private val Int.dp get() = (this * resources.displayMetrics.density).toInt()

    private fun requestPerms() {
        val needed = mutableListOf(
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_PHONE_STATE
        ).filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }.toMutableList()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            needed.add(Manifest.permission.POST_NOTIFICATIONS)
        if (needed.isNotEmpty()) ActivityCompat.requestPermissions(this, needed.toTypedArray(), 10)
    }

    private fun requestBatteryExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(PowerManager::class.java)
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply { data = Uri.parse("package:$packageName") })
                } catch (_: Exception) { }
            }
        }
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_LONG).show()

    override fun onUserInteraction() {
        super.onUserInteraction()
        if (unlocked) last = SystemClock.elapsedRealtime()
    }

    override fun onResume() {
        super.onResume()
        if (unlocked && SystemClock.elapsedRealtime() - last >= 30_000) {
            unlocked = false
            showPinEnter()
        }
    }
}
