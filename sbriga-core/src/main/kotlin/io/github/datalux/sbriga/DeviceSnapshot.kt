package io.github.datalux.sbriga

/**
 * Everything the [Diagnoser] needs, collected up front so that the diagnosis itself is a pure function.
 * Nullable fields are `null` when the OS version does not expose that signal.
 */
public data class DeviceSnapshot(
    val vendor: Vendor,
    val sdkInt: Int,
    /** `UsageStatsManager.STANDBY_BUCKET_*` (API 28+). */
    val standbyBucket: Int?,
    /** `PowerManager.isIgnoringBatteryOptimizations` (API 23+). */
    val ignoringBatteryOptimizations: Boolean?,
    /** The user chose "Restricted" for the app (API 28+). */
    val backgroundRestricted: Boolean?,
    /** Past exits, newest first. `null` below API 30. */
    val exits: List<ExitRecord>?,
)

internal object StandbyBucket {
    const val WORKING_SET = 20
    const val RARE = 40
    const val RESTRICTED = 45
}
