package com.example.callvault

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.SystemClock
import android.text.InputType
import android.view.Gravity
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private lateinit var pin: SecurePinStore
    private var unlocked = false
    private var last = 0L

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        pin = SecurePinStore(this)
        if (pin.hasPin()) unlockScreen() else createScreen()
    }

    private fun box() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        setPadding(48, 48, 48, 48)
    }

    // Bug 1 fix: was inputType=2 (TYPE_CLASS_NUMBER) — digits showed in plaintext while typing
    // Now: TYPE_CLASS_NUMBER | TYPE_NUMBER_VARIATION_PASSWORD = 18 — digits are masked
    private fun pinField(hint: String) = EditText(this).apply {
        this.hint = hint
        inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        maxEms = 6
    }

    private fun createScreen() {
        val l = box()
        l.addView(TextView(this).apply { text = "CallVault\nCreate 6-digit PIN"; textSize = 24f })
        val a = pinField("PIN")
        val b = pinField("Confirm PIN")
        val x = Button(this).apply { text = "Set PIN" }
        l.addView(a); l.addView(b); l.addView(x)
        setContentView(l)
        x.setOnClickListener {
            if (a.text.length == 6 && a.text.toString() == b.text.toString()) {
                pin.setPin(a.text.toString()); openVault()
            } else {
                toast("Enter matching 6-digit PIN")
            }
        }
    }

    private fun unlockScreen() {
        val l = box()
        l.addView(TextView(this).apply { text = "CallVault\nEnter PIN"; textSize = 24f })
        val a = pinField("6-digit PIN")
        val x = Button(this).apply { text = "Unlock" }
        l.addView(a); l.addView(x)
        setContentView(l)
        x.setOnClickListener {
            if (pin.verify(a.text.toString())) openVault() else toast("Incorrect PIN")
        }
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
        val l = box()
        l.addView(TextView(this).apply { text = "CallVault\nEncrypted call-log vault"; textSize = 22f })
        val r = Button(this).apply { text = "Read Call Logs" }
        // Bug 2 fix: "Backup to Telegram" button — was completely missing
        val s = Button(this).apply { text = "Backup to Telegram" }
        val z = Button(this).apply { text = "Lock" }
        l.addView(r); l.addView(s); l.addView(z)
        setContentView(l)

        r.setOnClickListener {
            last = SystemClock.elapsedRealtime()
            toast("Found ${CallLogReader(this).read().size} entries")
        }

        s.setOnClickListener {
            last = SystemClock.elapsedRealtime()
            val token  = BuildConfig.TELEGRAM_BOT_TOKEN
            val chatId = BuildConfig.TELEGRAM_CHAT_ID
            if (token.isBlank() || chatId.isBlank()) {
                toast("Telegram not configured in build"); return@setOnClickListener
            }
            val entries = CallLogReader(this).read()
            if (entries.isEmpty()) { toast("No call log entries"); return@setOnClickListener }
            toast("Sending ${entries.size} entries…")
            // Bug 3 fix: network on background thread to avoid NetworkOnMainThreadException
            Thread {
                val err = TelegramSender.send(entries, token, chatId)
                runOnUiThread {
                    if (err == null) {
                        toast("✓ Backup sent to Telegram")
                    } else {
                        // Show Telegram's exact error so user knows what to fix
                        toast("✗ $err")
                        android.util.Log.e("CallVault", "Telegram error: $err")
                    }
                }
            }.start()
        }

        z.setOnClickListener { unlocked = false; unlockScreen() }
    }

    // Bug 6 fix: missing onRequestPermissionsResult — user got no feedback on denial
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 10 &&
            grantResults.firstOrNull() != PackageManager.PERMISSION_GRANTED
        ) {
            toast("Call log permission denied — backup will be empty")
        }
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()

    override fun onUserInteraction() {
        super.onUserInteraction()
        if (unlocked) last = SystemClock.elapsedRealtime()
    }

    override fun onResume() {
        super.onResume()
        if (unlocked && SystemClock.elapsedRealtime() - last >= 30_000) {
            unlocked = false; unlockScreen()
        }
    }
}
