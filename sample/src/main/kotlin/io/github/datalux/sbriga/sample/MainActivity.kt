package io.github.datalux.sbriga.sample

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import io.github.datalux.sbriga.Action
import io.github.datalux.sbriga.Diagnosis
import io.github.datalux.sbriga.FindingCode
import io.github.datalux.sbriga.Limitation
import io.github.datalux.sbriga.LaunchOutcome
import io.github.datalux.sbriga.Sbriga
import io.github.datalux.sbriga.SettingsTarget

/** Shows the diagnosis and lets you try every vendor screen one by one, then copy a report. */
class MainActivity : Activity() {
    private lateinit var sbriga: Sbriga
    private lateinit var content: LinearLayout
    private var diagnosis: Diagnosis? = null
    private val attempts = mutableMapOf<String, Boolean>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sbriga = Sbriga(this)
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(this).apply {
            addView(content, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            val gutter = dp(16)
            @Suppress("DEPRECATION")
            setOnApplyWindowInsetsListener { view, insets ->
                view.setPadding(
                    gutter + insets.systemWindowInsetLeft,
                    gutter + insets.systemWindowInsetTop,
                    gutter + insets.systemWindowInsetRight,
                    gutter + insets.systemWindowInsetBottom,
                )
                insets
            }
        }
        setContentView(scroll)
        // Light background: keep the status bar icons dark so they stay visible.
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
    }

    override fun onResume() {
        super.onResume()
        // Coming back from a settings screen: the state may have changed.
        render()
    }

    private fun render() {
        val result = sbriga.diagnose()
        diagnosis = result
        content.removeAllViews()

        content.add(text("sbriga demo", size = 24f, bold = true))
        content.add(text("Vendor: ${result.vendor}", size = 16f))

        content.add(heading("Findings"))
        if (result.isHealthy) content.add(text("Nothing suggests background work is being limited."))
        result.findings.forEach {
            content.add(text("• ${describe(it.code)} [${it.confidence}]" + evidence(it.evidenceCount)))
        }
        result.limitations.forEach { content.add(text("• Limit: ${describe(it)}")) }

        content.add(heading("Suggested actions"))
        if (result.suggestedActions.isEmpty()) content.add(text("None"))
        result.suggestedActions.forEach { content.add(text("• ${describe(it)}")) }

        content.add(heading("Open settings"))
        content.add(button("Autostart (with fallbacks)") { open(SettingsTarget.AUTOSTART) })
        content.add(button("Battery (with fallbacks)") { open(SettingsTarget.BATTERY) })

        val candidates = SettingsTarget.entries.flatMap { sbriga.rules.candidates(result.vendor, it) }
        content.add(heading("Try each vendor screen"))
        if (candidates.isEmpty()) content.add(text("No candidates for this vendor."))
        candidates.forEach { candidate ->
            val state = when (attempts[candidate.component]) {
                true -> " ✓ opened"
                false -> " ✗ did not open"
                null -> ""
            }
            content.add(button("${candidate.target}: ${candidate.className.substringAfterLast('.')}$state") {
                attempts[candidate.component] = sbriga.settingsLauncher().open(candidate)
                render()
            })
        }

        content.add(heading("Report"))
        content.add(text("Copy this and paste it in a GitHub issue to improve the vendor table."))
        content.add(button("Copy report") { copyReport(candidates) })
        content.add(button("Refresh") { render() })
    }

    private fun open(target: SettingsTarget) {
        val result = sbriga.settingsLauncher().open(target)
        val message = when (result.outcome) {
            LaunchOutcome.VENDOR_SCREEN -> "Vendor screen: ${result.component}"
            LaunchOutcome.BATTERY_OPTIMIZATION_LIST -> "Fallback: battery optimization list"
            LaunchOutcome.APP_DETAILS -> "Fallback: app details"
            LaunchOutcome.FAILED -> "Nothing could be opened"
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun copyReport(candidates: List<io.github.datalux.sbriga.SettingsCandidate>) {
        val current = diagnosis ?: return
        val report = ReportBuilder.build(current, candidates, attempts)
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("sbriga report", report))
        Toast.makeText(this, "Report copied", Toast.LENGTH_SHORT).show()
    }

    private fun evidence(count: Int) = if (count > 0) " ×$count" else ""

    private fun describe(code: FindingCode) = when (code) {
        FindingCode.USER_FORCE_STOPPED -> "The user force-stopped the app"
        FindingCode.APP_CRASHES -> "The app crashed or hit an ANR"
        FindingCode.LOW_MEMORY_KILLS -> "Killed for low memory"
        FindingCode.SUSPECTED_VENDOR_KILLS -> "Repeated unexplained kills (possible vendor battery manager)"
        FindingCode.BATTERY_OPTIMIZATION_ACTIVE -> "Battery optimization is active"
        FindingCode.BACKGROUND_RESTRICTED -> "Background use is restricted"
        FindingCode.STANDBY_BUCKET_LIMITED -> "App standby bucket is limiting background work"
    }

    private fun describe(action: Action) = when (action) {
        Action.REMOVE_BACKGROUND_RESTRICTION -> "Remove the background restriction"
        Action.DISABLE_BATTERY_OPTIMIZATION -> "Exempt the app from battery optimization"
        Action.ALLOW_VENDOR_AUTOSTART -> "Allow autostart in the vendor's settings"
    }

    private fun describe(limitation: Limitation) = when (limitation) {
        Limitation.EXIT_HISTORY_UNAVAILABLE -> "exit history needs Android 11 or later"
        Limitation.VENDOR_CAUSE_NOT_REPORTED -> "the OS does not say which vendor component killed the app"
    }

    private fun LinearLayout.add(view: View) = addView(view)

    private fun heading(label: String) = text(label, size = 18f, bold = true).apply { setPadding(0, dp(20), 0, dp(4)) }

    private fun text(value: String, size: Float = 14f, bold: Boolean = false) = TextView(this).apply {
        text = value
        textSize = size
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
        setPadding(0, dp(2), 0, dp(2))
    }

    private fun button(label: String, onClick: () -> Unit) = Button(this).apply {
        text = label
        isAllCaps = false
        setOnClickListener { onClick() }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
