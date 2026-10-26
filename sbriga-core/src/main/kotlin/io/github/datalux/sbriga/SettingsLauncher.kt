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
 * Opens the best available settings screen. Vendor components are undocumented, so each is attempted
 * and failures are expected: the standard screens are the guaranteed fallback.
 */
public class SettingsLauncher internal constructor(
    private val context: Context,
    private val rules: VendorRules,
    private val vendor: Vendor,
) {
    public fun open(target: SettingsTarget): LaunchOutcome {
        for (candidate in rules.candidates(vendor, target)) {
            val intent = Intent().setComponent(ComponentName(candidate.packageName, candidate.className))
            if (start(intent)) return LaunchOutcome.VENDOR_SCREEN
        }
        if (start(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))) {
            return LaunchOutcome.BATTERY_OPTIMIZATION_LIST
        }
        val details = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
        return if (start(details)) LaunchOutcome.APP_DETAILS else LaunchOutcome.FAILED
    }

    private fun start(intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}
