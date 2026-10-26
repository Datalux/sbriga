package io.github.datalux.sbriga

/** Turns a [DeviceSnapshot] into a [Diagnosis]. Pure: no Android classes, no I/O. */
public object Diagnoser {
    /** Suspected kills needed before a finding is reported. */
    internal const val SUSPECT_THRESHOLD = 2

    /** Suspected kills needed on an aggressive vendor to raise confidence to [Confidence.MEDIUM]. */
    internal const val AGGRESSIVE_VENDOR_THRESHOLD = 3

    public fun diagnose(snapshot: DeviceSnapshot): Diagnosis {
        val findings = mutableListOf<Finding>()
        val actions = linkedSetOf<Action>()
        val limitations = mutableListOf<Limitation>()

        if (snapshot.backgroundRestricted == true) {
            findings += Finding(FindingCode.BACKGROUND_RESTRICTED, Confidence.HIGH)
            actions += Action.REMOVE_BACKGROUND_RESTRICTION
        }

        when (snapshot.standbyBucket) {
            StandbyBucket.RESTRICTED -> findings += Finding(FindingCode.STANDBY_BUCKET_LIMITED, Confidence.HIGH)
            StandbyBucket.RARE -> findings += Finding(FindingCode.STANDBY_BUCKET_LIMITED, Confidence.MEDIUM)
        }

        if (snapshot.ignoringBatteryOptimizations == false) {
            findings += Finding(FindingCode.BATTERY_OPTIMIZATION_ACTIVE, Confidence.HIGH)
            actions += Action.DISABLE_BATTERY_OPTIMIZATION
        }

        val exits = snapshot.exits
        if (exits == null) {
            limitations += Limitation.EXIT_HISTORY_UNAVAILABLE
        } else {
            analyzeExits(snapshot.vendor, exits, findings, actions)
            if (snapshot.vendor.isAggressive) limitations += Limitation.VENDOR_CAUSE_NOT_REPORTED
        }

        return Diagnosis(snapshot.vendor, findings, actions.toList(), limitations)
    }

    private fun analyzeExits(
        vendor: Vendor,
        exits: List<ExitRecord>,
        findings: MutableList<Finding>,
        actions: MutableSet<Action>,
    ) {
        val forceStops = exits.count { it.reason == ExitReason.USER_STOPPED }
        if (forceStops > 0) findings += Finding(FindingCode.USER_FORCE_STOPPED, Confidence.HIGH, forceStops)

        val appFaults = exits.count { it.reason.isAppFault }
        if (appFaults > 0) findings += Finding(FindingCode.APP_CRASHES, Confidence.HIGH, appFaults)

        val lowMemory = exits.count { it.reason == ExitReason.LOW_MEMORY }
        if (lowMemory > 0) findings += Finding(FindingCode.LOW_MEMORY_KILLS, Confidence.HIGH, lowMemory)

        val suspected = exits.count { it.reason.isSuspectedSystemKill }
        if (suspected >= SUSPECT_THRESHOLD) {
            val confidence = when {
                vendor.isAggressive && suspected >= AGGRESSIVE_VENDOR_THRESHOLD -> Confidence.MEDIUM
                else -> Confidence.LOW
            }
            findings += Finding(FindingCode.SUSPECTED_VENDOR_KILLS, confidence, suspected)
            if (vendor != Vendor.OTHER) actions += Action.ALLOW_VENDOR_AUTOSTART
        }
    }
}
