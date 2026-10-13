package dev.oneshot.probe

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Entry point for the M0 device capability probe.
 *
 * Plain Views, not Compose: the probe ships alone, is only ever driven over adb, and a
 * Compose toolchain in a throwaway module would bury the thing we actually want (the JSON
 * report) under UI plumbing.
 *
 * Usage:
 *   adb install -r probe-debug.apk
 *   adb shell am start -n dev.oneshot.probe/.ProbeActivity --es auto true
 *   adb pull /sdcard/Android/data/dev.oneshot.probe/files/probe-latest.json docs/probe/
 */
class ProbeActivity : ComponentActivity() {

    private lateinit var status: TextView
    private var running = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildUi())
        // A screen timeout mid-probe would destroy the activity and unbind the camera, so the
        // bind phases would report failures that have nothing to do with the HAL.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)


        if (!hasPermissions()) {
            requestPermissions()
            return
        }
        // Only auto-run on a genuinely fresh launch, not after a configuration change,
        // or rotating the phone would restart the whole probe.
        if (savedInstanceState == null && intent?.getStringExtra(EXTRA_AUTO)?.toBooleanStrictOrNull() == true) {
            runProbe()
        }
    }

    private fun buildUi() = ScrollView(this).apply {
        setBackgroundColor(Color.BLACK)
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
            addView(Button(context).apply {
                text = "RUN PROBE"
                setOnClickListener { runProbe() }
            })
            status = TextView(context).apply {
                setTextColor(Color.LTGRAY)
                typeface = Typeface.MONOSPACE
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                gravity = Gravity.TOP or Gravity.START
            }
            addView(status)
        })
    }

    private fun runProbe() {
        if (running) return
        if (!hasPermissions()) {
            requestPermissions()
            return
        }
        running = true
        append("Starting probe on ${Build.MODEL} (API ${Build.VERSION.SDK_INT})")
        append("Output: ${reportFile().absolutePath}")

        val runner = ProbeRunner(applicationContext) { line -> runOnUiThread { append(line) } }
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                runCatching { runner.runStaticPhases() }
                    .onFailure { append("ABORTED in A+C: ${it.javaClass.name}: ${it.message}") }
                runCatching { runner.runBindPhases(this@ProbeActivity, reportFile().parentFile) }
                    .onFailure { append("ABORTED in B+D: ${it.javaClass.name}: ${it.message}") }
            }
            append("Done. Phases A, C, B, D written to ${reportFile().absolutePath}")
            running = false
        }
    }

    private fun reportFile(): File = File(getExternalFilesDir(null) ?: filesDir, "probe-latest.json")

    private fun append(line: String) {
        status.append("\n$line")
    }

    private fun hasPermissions(): Boolean = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSION_REQUEST)
        append("Permissions requested. Once granted, tap RUN PROBE (or relaunch with --es auto true).")
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val PERMISSION_REQUEST = 1001
        const val EXTRA_AUTO = "auto"
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    }
}
