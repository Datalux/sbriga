package io.github.datalux.sbriga.sample

import android.os.Build
import io.github.datalux.sbriga.Diagnosis
import io.github.datalux.sbriga.SettingsCandidate

/** Builds the Markdown report a user pastes into a GitHub issue to improve the vendor table. */
internal object ReportBuilder {
    /** What happened when the user tapped a candidate: `true` opened a screen, `false` did nothing. */
    fun build(diagnosis: Diagnosis, candidates: List<SettingsCandidate>, attempts: Map<String, Boolean>): String =
        buildString {
            appendLine("### Device")
            appendLine("- Manufacturer: ${Build.MANUFACTURER}")
            appendLine("- Brand: ${Build.BRAND}")
            appendLine("- Model: ${Build.MODEL}")
            appendLine("- Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("- Build display: ${Build.DISPLAY}")
            appendLine("- Detected vendor: ${diagnosis.vendor}")
            appendLine()
            appendLine("### Diagnosis")
            if (diagnosis.findings.isEmpty()) appendLine("- No findings")
            diagnosis.findings.forEach {
                appendLine("- ${it.code} (${it.confidence}, evidence ${it.evidenceCount})")
            }
            diagnosis.limitations.forEach { appendLine("- Limitation: $it") }
            appendLine()
            appendLine("### Vendor screens tried")
            if (candidates.isEmpty()) appendLine("- No candidates for this vendor")
            candidates.forEach {
                val result = when (attempts[it.component]) {
                    true -> "opened"
                    false -> "did not open"
                    null -> "not tried"
                }
                appendLine("- `${it.component}` (${it.target}): $result")
            }
            appendLine()
            appendLine("### Notes")
            appendLine("<!-- Say what screen opened, and whether it was the right one. -->")
        }
}
