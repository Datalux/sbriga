package io.github.datalux.sbriga

import android.content.Context

/** Entry point: find out whether this app's background work is being limited, and why. */
public class Sbriga(context: Context, rules: VendorRules? = null) {
    private val appContext = context.applicationContext

    /** The vendor table in use: the one passed in, or the bundled one. */
    public val rules: VendorRules = rules ?: loadBundledRules(appContext)

    /** Collects the current device state and diagnoses it. Cheap; safe to call at app start. */
    public fun diagnose(): Diagnosis = Diagnoser.diagnose(SnapshotCollector(appContext).collect())

    /** A launcher for the settings screens that can fix what [diagnose] found. */
    public fun settingsLauncher(): SettingsLauncher =
        SettingsLauncher(appContext, rules, Vendor.detect(android.os.Build.MANUFACTURER, android.os.Build.BRAND))

    private companion object {
        const val RULES_ASSET = "sbriga-vendors.json"

        fun loadBundledRules(context: Context): VendorRules = runCatching {
            context.assets.open(RULES_ASSET).bufferedReader().use { VendorRules.fromJson(it.readText()) }
        }.getOrDefault(VendorRules.EMPTY)
    }
}
