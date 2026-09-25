package com.example.callvault

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
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

    private fun pinField(hint: String) = EditText(this).apply {
        this.hint = hint
        inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        maxEms = 6
    }

    private fun createScreen() {
        val l = box()
        l.addView(TextView(this).apply { text = "JEEVI\nCreate 6-digit PIN"; textSize = 24f })
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
        l.addView(TextView(this).apply { text = "JEEVI\nEnter PIN"; textSize = 24f })
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
        requestPermissions()
        requestBatteryOptimizationExemption()

        // Schedule 5 AM alarm (AlarmManager — reliable even in Doze mode)
        ScheduleHelper.scheduleDailyAt5AM(this)
        val hours = ScheduleHelper.hoursUntil5AM()
        toast("Auto-backup scheduled — next in ${hours}h at 5:00 AM")

        val l = box()
        l.addView(TextView(this).apply {
            text = "JEEVI\nAuto-backup: daily 5:00 AM"
            textSize = 22f
        })
        val r = Button(this).apply { text = "View Call Log Count" }
        val z = Button(this).apply { text = "Lock" }
        l.addView(r); l.addView(z)
        setContentView(l)

        r.setOnClickListener {
            last = SystemClock.elapsedRealtime()
            val stored = CallStore(this).readAll().size
            val system = CallLogReader(this).read().size
            toast("Local store: $stored | System log: $system")
        }

        z.setOnClickListener { unlocked = false; unlockScreen() }
    }

    private fun requestPermissions() {
        val needed = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG)
            != PackageManager.PERMISSION_GRANTED) needed.add(Manifest.permission.READ_CALL_LOG)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED) needed.add(Manifest.permission.READ_PHONE_STATE)
        if (needed.isNotEmpty())
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), 10)
    }

    // Ask user to exempt app from battery optimization
    // Without this Android kills background tasks after ~2 hours
    private fun requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(PowerManager::class.java)
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                startActivity(
                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 10) {
            val denied = permissions.zip(grantResults.toList())
                .filter { it.second != PackageManager.PERMISSION_GRANTED }
                .map { it.first.substringAfterLast(".") }
            if (denied.isNotEmpty()) toast("Permission denied: ${denied.joinToString()}")
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
            unlocked = false; unlockScreen()
        }
    }
}
