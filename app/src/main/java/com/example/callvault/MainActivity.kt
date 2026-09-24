package com.example.callvault

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.SystemClock
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.Window
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {

    private lateinit var pin: SecurePinStore
    private var unlocked = false
    private var last = 0L

    private val bg = Color.rgb(8, 10, 18)
    private val surface = Color.rgb(17, 20, 32)
    private val surface2 = Color.rgb(23, 27, 43)
    private val primary = Color.rgb(124, 92, 255)
    private val cyan = Color.rgb(61, 220, 255)
    private val green = Color.rgb(64, 220, 151)
    private val muted = Color.rgb(157, 166, 188)
    private val white = Color.rgb(244, 246, 252)

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        pin = SecurePinStore(this)
        setupWindow()
        if (pin.hasPin()) unlockScreen() else createScreen()
    }

    private fun setupWindow() {
        window.statusBarColor = bg
        window.navigationBarColor = bg
        window.decorView.systemUiVisibility = 0
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).roundToInt()

    private fun rootScroll(): ScrollView = ScrollView(this).apply {
        setBackgroundColor(bg)
        isFillViewport = true
        clipToPadding = false
        setPadding(dp(22), dp(18), dp(22), dp(28))
    }

    private fun content(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        layoutParams = LinearLayout.LayoutParams(-1, -2)
    }

    private fun logo(size: Int = 76): ImageView = ImageView(this).apply {
        setImageResource(com.example.callvault.R.drawable.ic_callvault_logo)
        layoutParams = LinearLayout.LayoutParams(dp(size), dp(size)).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            bottomMargin = dp(14)
        }
    }

    private fun title(text: String, size: Float = 28f): TextView = TextView(this).apply {
        this.text = text
        textSize = size
        setTextColor(white)
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
        gravity = Gravity.CENTER
        setPadding(0, 0, 0, dp(6))
    }

    private fun subtitle(text: String): TextView = TextView(this).apply {
        this.text = text
        textSize = 14f
        setTextColor(muted)
        gravity = Gravity.CENTER
        setLineSpacing(0f, 1.2f)
        setPadding(dp(10), 0, dp(10), dp(20))
    }

    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(20), dp(20), dp(20), dp(20))
        background = GradientDrawable().apply {
            setColor(surface)
            cornerRadius = dp(22).toFloat()
            setStroke(dp(1), Color.rgb(42, 47, 68))
        }
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = dp(14)
        }
    }

    private fun pinField(hint: String): EditText = EditText(this).apply {
        this.hint = hint
        inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        setTextColor(white)
        setHintTextColor(Color.rgb(105, 113, 136))
        textSize = 17f
        gravity = Gravity.CENTER
        maxEms = 6
        letterSpacing = 0.18f
        setPadding(dp(16), 0, dp(16), 0)
        background = GradientDrawable().apply {
            setColor(surface2)
            cornerRadius = dp(16).toFloat()
            setStroke(dp(1), Color.rgb(55, 61, 86))
        }
        layoutParams = LinearLayout.LayoutParams(-1, dp(56)).apply {
            bottomMargin = dp(12)
        }
    }

    private fun primaryButton(text: String): Button = Button(this).apply {
        this.text = text
        this.textSize = 15f
        setTextColor(Color.WHITE)
        typeface = Typeface.DEFAULT_BOLD
        isAllCaps = false
        stateListAnimator = null
        background = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(Color.rgb(102, 72, 235), Color.rgb(44, 173, 255))
        ).apply { cornerRadius = dp(16).toFloat() }
        layoutParams = LinearLayout.LayoutParams(-1, dp(54)).apply {
            topMargin = dp(4)
            bottomMargin = dp(8)
        }
    }

    private fun secondaryButton(text: String): Button = Button(this).apply {
        this.text = text
        this.textSize = 14f
        setTextColor(white)
        isAllCaps = false
        stateListAnimator = null
        background = GradientDrawable().apply {
            setColor(surface2)
            cornerRadius = dp(16).toFloat()
            setStroke(dp(1), Color.rgb(53, 60, 84))
        }
        layoutParams = LinearLayout.LayoutParams(-1, dp(52)).apply {
            bottomMargin = dp(8)
        }
    }

    private fun label(text: String): TextView = TextView(this).apply {
        this.text = text
        textSize = 12f
        setTextColor(muted)
        typeface = Typeface.DEFAULT_BOLD
        letterSpacing = 0.08f
        setPadding(0, 0, 0, dp(7))
    }

    private fun createScreen() {
        val scroll = rootScroll()
        val l = content()

        l.addView(logo(82))
        l.addView(title("Create your vault"))
        l.addView(subtitle("Protect your call history with a private 6-digit PIN."))

        val c = card()
        c.addView(label("NEW PIN"))
        val a = pinField("Enter 6-digit PIN")
        c.addView(a)
        c.addView(label("CONFIRM PIN"))
        val b = pinField("Re-enter your PIN")
        c.addView(b)
        val x = primaryButton("Create Secure Vault  〉")
        c.addView(x)
        l.addView(c)

        l.addView(TextView(this).apply {
            text = "🔒  Your vault stays locked when you leave the app"
            textSize = 12f
            setTextColor(muted)
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(4), dp(10), 0)
        })

        scroll.addView(l)
        setContentView(scroll)

        x.setOnClickListener {
            if (a.text.length == 6 && a.text.toString() == b.text.toString()) {
                pin.setPin(a.text.toString())
                openVault()
            } else toast("Enter matching 6-digit PIN")
        }
    }

    private fun unlockScreen() {
        val scroll = rootScroll()
        val l = content()

        l.addView(Space(this), LinearLayout.LayoutParams(1, dp(42)))
        l.addView(logo(94))
        l.addView(title("Welcome back"))
        l.addView(subtitle("Your CallVault is locked.\nEnter your PIN to continue."))

        val c = card()
        c.addView(label("SECURE PIN"))
        val a = pinField("6-digit PIN")
        c.addView(a)
        val x = primaryButton("Unlock Vault  〉")
        c.addView(x)
        l.addView(c)

        l.addView(TextView(this).apply {
            text = "Auto-lock enabled  •  30 seconds"
            textSize = 12f
            setTextColor(muted)
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, 0)
        })

        scroll.addView(l)
        setContentView(scroll)

        x.setOnClickListener {
            if (pin.verify(a.text.toString())) openVault() else toast("Incorrect PIN")
        }
        a.setOnEditorActionListener { _, _, _ -> x.performClick(); true }
    }

    private fun openVault() {
        unlocked = true
        last = SystemClock.elapsedRealtime()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.READ_CALL_LOG), 10
            )
        }

        ScheduleHelper.scheduleDailyAt5AM(this)
        val hours = ScheduleHelper.hoursUntil5AM()
        toast("Auto-backup scheduled • next run in ${hours}h")

        val scroll = rootScroll()
        val l = content()

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(-1, dp(74)).apply {
                bottomMargin = dp(12)
            }
        }
        val miniLogo = ImageView(this).apply {
            setImageResource(R.drawable.ic_callvault_logo)
            layoutParams = LinearLayout.LayoutParams(dp(52), dp(52))
        }
        header.addView(miniLogo)
        header.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
            addView(TextView(context).apply {
                text = "CALLVAULT"
                textSize = 20f
                setTextColor(white)
                typeface = Typeface.DEFAULT_BOLD
                letterSpacing = 0.08f
            })
            addView(TextView(context).apply {
                text = "PRIVATE CALL LOG VAULT"
                textSize = 10f
                setTextColor(cyan)
                letterSpacing = 0.12f
            })
        }, LinearLayout.LayoutParams(0, -2, 1f))
        val lock = TextView(this).apply {
            text = "⌁"
            textSize = 24f
            setTextColor(muted)
            gravity = Gravity.CENTER
            setOnClickListener { unlocked = false; unlockScreen() }
        }
        header.addView(lock, LinearLayout.LayoutParams(dp(42), dp(42)))
        l.addView(header)

        val hero = card()
        hero.background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(Color.rgb(28, 25, 58), Color.rgb(16, 31, 48))
        ).apply {
            cornerRadius = dp(24).toFloat()
            setStroke(dp(1), Color.rgb(65, 72, 110))
        }
        hero.addView(TextView(this).apply {
            text = "SECURE"
            textSize = 11f
            setTextColor(green)
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.15f
        })
        hero.addView(TextView(this).apply {
            text = "Your vault is protected"
            textSize = 22f
            setTextColor(white)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(5), 0, dp(5))
        })
        hero.addView(TextView(this).apply {
            text = "Automatic backup runs every day at 5:00 AM."
            textSize = 13f
            setTextColor(muted)
        })
        l.addView(hero)

        val stats = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(-1, dp(92)).apply {
                bottomMargin = dp(14)
            }
        }
        val stat1 = statCard("AUTO BACKUP", "05:00 AM", cyan)
        val stat2 = statCard("AUTO LOCK", "30 SEC", primary)
        stats.addView(stat1, LinearLayout.LayoutParams(0, -1, 1f).apply { marginEnd = dp(6) })
        stats.addView(stat2, LinearLayout.LayoutParams(0, -1, 1f).apply { marginStart = dp(6) })
        l.addView(stats)

        val callCard = card()
        callCard.addView(TextView(this).apply {
            text = "CALL HISTORY"
            textSize = 12f
            setTextColor(muted)
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.1f
        })
        callCard.addView(TextView(this).apply {
            text = "Check stored entries"
            textSize = 17f
            setTextColor(white)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(5), 0, dp(10))
        })
        val r = primaryButton("View Call Log Count")
        callCard.addView(r)
        l.addView(callCard)

        val z = secondaryButton("Lock Vault")
        l.addView(z)
        l.addView(TextView(this).apply {
            text = "CallVault • Privacy first"
            textSize = 11f
            setTextColor(Color.rgb(100, 108, 130))
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, 0)
        })

        scroll.addView(l)
        setContentView(scroll)

        r.setOnClickListener {
            last = SystemClock.elapsedRealtime()
            val count = CallLogReader(this).read().size
            showCount(count)
        }
        z.setOnClickListener { unlocked = false; unlockScreen() }
    }

    private fun statCard(caption: String, value: String, accent: Int): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(10), dp(16), dp(10))
            background = GradientDrawable().apply {
                setColor(surface)
                cornerRadius = dp(18).toFloat()
                setStroke(dp(1), Color.rgb(42, 47, 68))
            }
            addView(TextView(context).apply {
                text = caption
                textSize = 9f
                setTextColor(muted)
                typeface = Typeface.DEFAULT_BOLD
                letterSpacing = 0.1f
            })
            addView(TextView(context).apply {
                text = value
                textSize = 16f
                setTextColor(accent)
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, dp(5), 0, 0)
            })
        }

    private fun showCount(count: Int) {
        val d = android.app.Dialog(this)
        val box = card()
        box.setPadding(dp(24), dp(24), dp(24), dp(20))
        box.addView(TextView(this).apply {
            text = "CALL LOG"
            textSize = 11f
            setTextColor(cyan)
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.14f
        })
        box.addView(TextView(this).apply {
            text = count.toString()
            textSize = 46f
            setTextColor(white)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, dp(10), 0, dp(4))
        })
        box.addView(TextView(this).apply {
            text = "entries found on this device"
            textSize = 13f
            setTextColor(muted)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(16))
        })
        val ok = primaryButton("Done")
        box.addView(ok)
        d.setContentView(box)
        d.window?.setBackgroundDrawableResource(android.R.color.transparent)
        d.show()
        ok.setOnClickListener { d.dismiss() }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 10 &&
            grantResults.firstOrNull() != PackageManager.PERMISSION_GRANTED
        ) {
            toast("Call log permission denied • backup may be empty")
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
            unlockScreen()
        }
    }
}
