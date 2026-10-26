package io.github.datalux.sbriga

/** Why a process of the app ended, mirroring `ApplicationExitInfo.REASON_*` (API 30+). */
public enum class ExitReason {
    UNKNOWN,
    EXIT_SELF,
    SIGNALED,
    LOW_MEMORY,
    CRASH,
    CRASH_NATIVE,
    ANR,
    INITIALIZATION_FAILURE,
    PERMISSION_CHANGE,
    EXCESSIVE_RESOURCE_USAGE,
    USER_REQUESTED,
    USER_STOPPED,
    DEPENDENCY_DIED,
    OTHER,
    FREEZER,
    PACKAGE_STATE_CHANGE,
    PACKAGE_UPDATED,
    ;

    /** The app's own fault: nothing a vendor setting can fix. */
    internal val isAppFault: Boolean
        get() = this == CRASH || this == CRASH_NATIVE || this == ANR || this == INITIALIZATION_FAILURE

    /** Killed by the user, on purpose. */
    internal val isUserAction: Boolean
        get() = this == USER_REQUESTED || this == USER_STOPPED

    /** Ended by something outside the app and the user, without a clear cause. */
    internal val isSuspectedSystemKill: Boolean
        get() = this == SIGNALED || this == OTHER || this == UNKNOWN || this == FREEZER ||
            this == EXCESSIVE_RESOURCE_USAGE

    internal companion object {
        // Values of ApplicationExitInfo.REASON_*; kept as literals so the mapping is testable on the JVM.
        fun fromSystemCode(code: Int): ExitReason = when (code) {
            0 -> UNKNOWN
            1 -> EXIT_SELF
            2 -> SIGNALED
            3 -> LOW_MEMORY
            4 -> CRASH
            5 -> CRASH_NATIVE
            6 -> ANR
            7 -> INITIALIZATION_FAILURE
            8 -> PERMISSION_CHANGE
            9 -> EXCESSIVE_RESOURCE_USAGE
            10 -> USER_REQUESTED
            11 -> USER_STOPPED
            12 -> DEPENDENCY_DIED
            13 -> OTHER
            14 -> FREEZER
            15 -> PACKAGE_STATE_CHANGE
            16 -> PACKAGE_UPDATED
            else -> UNKNOWN
        }
    }
}

/** One past process exit of the app. */
public data class ExitRecord(
    val reason: ExitReason,
    val timestampMillis: Long,
    /** `RunningAppProcessInfo.IMPORTANCE_*` at the moment of death. */
    val importance: Int,
)
