package io.github.datalux.sbriga

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/** What [SettingsLauncher.open] ended up showing. */
public enum class LaunchOutcome {
    /** A vendor-specific screen was opened. */
    VENDOR_SCREEN,

    /** The standard battery optimization list was opened. */
    BATTERY_OPTIMIZATION_LIST,

    /** Last resort: the app's own details page. */
    APP_DETAILS,

    /** Nothing could be started. */
    FAILED,
}

/**
 * Result of a launch attempt. [component] is the vendor component that opened (`package/class`),
 * only set for [LaunchOutcome.VENDOR_SCREEN]; it is what a vendor-table report needs.
 */
public data class LaunchResult(val outcome: LaunchOutcome, val component: String? = null)

/**
 * Opens the best available settings screen. Vendor components are undocumented, so each is attempted
 * and failures are expected: the standard screens are the guaranteed fallback.
 */
public class SettingsLauncher internal constructor(
    private val context: Context,
    private val rules: VendorRules,
    private val vendor: Vendor,
) {
    public fun open(target: SettingsTarget): LaunchResult {
        for (candidate in rules.candidates(vendor, target)) {
            if (open(candidate)) return LaunchResult(LaunchOutcome.VENDOR_SCREEN, candidate.component)
        }
        if (start(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))) {
            return LaunchResult(LaunchOutcome.BATTERY_OPTIMIZATION_LIST)
        }
        val details = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
        return LaunchResult(if (start(details)) LaunchOutcome.APP_DETAILS else LaunchOutcome.FAILED)
    }

    /** Tries exactly one vendor candidate, with no fallback. Returns whether a screen was opened. */
    public fun open(candidate: SettingsCandidate): Boolean =
        start(Intent().setComponent(ComponentName(candidate.packageName, candidate.className)))

    private fun start(intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}
