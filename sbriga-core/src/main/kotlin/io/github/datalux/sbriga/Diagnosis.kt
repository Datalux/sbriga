package io.github.datalux.sbriga

public enum class Confidence { LOW, MEDIUM, HIGH }

/** Machine-readable cause; a UI maps these to localized text. */
public enum class FindingCode {
    USER_FORCE_STOPPED,
    APP_CRASHES,
    LOW_MEMORY_KILLS,
    SUSPECTED_VENDOR_KILLS,
    BATTERY_OPTIMIZATION_ACTIVE,
    BACKGROUND_RESTRICTED,
    STANDBY_BUCKET_LIMITED,
}

public data class Finding(
    val code: FindingCode,
    val confidence: Confidence,
    /** How many past exits support this finding; 0 when it comes from current device state. */
    val evidenceCount: Int = 0,
)

/** What the app can reasonably ask the user to do. */
public enum class Action {
    REMOVE_BACKGROUND_RESTRICTION,
    DISABLE_BATTERY_OPTIMIZATION,
    ALLOW_VENDOR_AUTOSTART,
}

/** Why a part of the picture could not be assessed. */
public enum class Limitation {
    /** Exit history needs API 30. */
    EXIT_HISTORY_UNAVAILABLE,

    /** The OS never says which OEM component killed the process, so vendor kills are inferred. */
    VENDOR_CAUSE_NOT_REPORTED,
}

public data class Diagnosis(
    val vendor: Vendor,
    val findings: List<Finding>,
    val suggestedActions: List<Action>,
    val limitations: List<Limitation>,
) {
    /** True when nothing suggests that the background work is being limited. */
    public val isHealthy: Boolean get() = findings.isEmpty()

    public fun has(code: FindingCode): Boolean = findings.any { it.code == code }
}
