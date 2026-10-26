package io.github.datalux.sbriga

import android.app.ActivityManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.PowerManager

/** Reads the signals the OS exposes about this app's background standing. Never throws. */
internal class SnapshotCollector(private val context: Context) {

    fun collect(maxExits: Int = MAX_EXITS): DeviceSnapshot = DeviceSnapshot(
        vendor = Vendor.detect(Build.MANUFACTURER, Build.BRAND),
        sdkInt = Build.VERSION.SDK_INT,
        standbyBucket = standbyBucket(),
        ignoringBatteryOptimizations = ignoringBatteryOptimizations(),
        backgroundRestricted = backgroundRestricted(),
        exits = exits(maxExits),
    )

    private fun standbyBucket(): Int? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
        return runCatching {
            val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            manager.appStandbyBucket
        }.getOrNull()
    }

    private fun ignoringBatteryOptimizations(): Boolean? = runCatching {
        val manager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        manager.isIgnoringBatteryOptimizations(context.packageName)
    }.getOrNull()

    private fun backgroundRestricted(): Boolean? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
        return runCatching {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            manager.isBackgroundRestricted
        }.getOrNull()
    }

    private fun exits(max: Int): List<ExitRecord>? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        return runCatching {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            manager.getHistoricalProcessExitReasons(context.packageName, 0, max).map {
                ExitRecord(ExitReason.fromSystemCode(it.reason), it.timestamp, it.importance)
            }
        }.getOrNull()
    }

    private companion object {
        const val MAX_EXITS = 30
    }
}
