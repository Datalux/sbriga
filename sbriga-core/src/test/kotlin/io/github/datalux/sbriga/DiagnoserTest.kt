package io.github.datalux.sbriga

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnoserTest {
    private fun snapshot(
        vendor: Vendor = Vendor.OTHER,
        bucket: Int? = StandbyBucket.WORKING_SET,
        ignoring: Boolean? = true,
        restricted: Boolean? = false,
        exits: List<ExitRecord>? = emptyList(),
    ) = DeviceSnapshot(vendor, 34, bucket, ignoring, restricted, exits)

    private fun exits(reason: ExitReason, count: Int) =
        List(count) { ExitRecord(reason, 1_000L * it, 100) }

    @Test fun cleanDeviceIsHealthy() {
        val diagnosis = Diagnoser.diagnose(snapshot())
        assertTrue(diagnosis.isHealthy)
        assertTrue(diagnosis.suggestedActions.isEmpty())
    }

    @Test fun batteryOptimizationSuggestsExemption() {
        val diagnosis = Diagnoser.diagnose(snapshot(ignoring = false))
        assertTrue(diagnosis.has(FindingCode.BATTERY_OPTIMIZATION_ACTIVE))
        assertEquals(listOf(Action.DISABLE_BATTERY_OPTIMIZATION), diagnosis.suggestedActions)
    }

    @Test fun backgroundRestrictionIsHighConfidence() {
        val diagnosis = Diagnoser.diagnose(snapshot(restricted = true))
        val finding = diagnosis.findings.single { it.code == FindingCode.BACKGROUND_RESTRICTED }
        assertEquals(Confidence.HIGH, finding.confidence)
        assertTrue(Action.REMOVE_BACKGROUND_RESTRICTION in diagnosis.suggestedActions)
    }

    @Test fun standbyBucketSeverityDependsOnBucket() {
        assertEquals(
            Confidence.HIGH,
            Diagnoser.diagnose(snapshot(bucket = StandbyBucket.RESTRICTED)).findings.single().confidence,
        )
        assertEquals(
            Confidence.MEDIUM,
            Diagnoser.diagnose(snapshot(bucket = StandbyBucket.RARE)).findings.single().confidence,
        )
    }

    @Test fun belowThresholdSystemKillsAreIgnored() {
        val diagnosis = Diagnoser.diagnose(snapshot(vendor = Vendor.XIAOMI, exits = exits(ExitReason.SIGNALED, 1)))
        assertFalse(diagnosis.has(FindingCode.SUSPECTED_VENDOR_KILLS))
    }

    @Test fun repeatedKillsOnAggressiveVendorGetMediumConfidenceAndVendorAction() {
        val diagnosis = Diagnoser.diagnose(snapshot(vendor = Vendor.XIAOMI, exits = exits(ExitReason.SIGNALED, 4)))
        val finding = diagnosis.findings.single { it.code == FindingCode.SUSPECTED_VENDOR_KILLS }
        assertEquals(Confidence.MEDIUM, finding.confidence)
        assertEquals(4, finding.evidenceCount)
        assertTrue(Action.ALLOW_VENDOR_AUTOSTART in diagnosis.suggestedActions)
        assertTrue(Limitation.VENDOR_CAUSE_NOT_REPORTED in diagnosis.limitations)
    }

    @Test fun killsOnUnknownVendorStayLowConfidenceWithoutVendorAction() {
        val diagnosis = Diagnoser.diagnose(snapshot(vendor = Vendor.OTHER, exits = exits(ExitReason.SIGNALED, 5)))
        val finding = diagnosis.findings.single { it.code == FindingCode.SUSPECTED_VENDOR_KILLS }
        assertEquals(Confidence.LOW, finding.confidence)
        assertFalse(Action.ALLOW_VENDOR_AUTOSTART in diagnosis.suggestedActions)
    }

    @Test fun crashesAndAnrsAreAppFaultNotVendorKills() {
        val diagnosis = Diagnoser.diagnose(
            snapshot(vendor = Vendor.XIAOMI, exits = exits(ExitReason.CRASH, 3) + exits(ExitReason.ANR, 2)),
        )
        assertEquals(5, diagnosis.findings.single { it.code == FindingCode.APP_CRASHES }.evidenceCount)
        assertFalse(diagnosis.has(FindingCode.SUSPECTED_VENDOR_KILLS))
        assertTrue(diagnosis.suggestedActions.isEmpty())
    }

    @Test fun lowMemoryIsReportedSeparately() {
        val diagnosis = Diagnoser.diagnose(snapshot(exits = exits(ExitReason.LOW_MEMORY, 2)))
        assertTrue(diagnosis.has(FindingCode.LOW_MEMORY_KILLS))
        assertFalse(diagnosis.has(FindingCode.SUSPECTED_VENDOR_KILLS))
    }

    @Test fun userForceStopIsReported() {
        val diagnosis = Diagnoser.diagnose(snapshot(exits = exits(ExitReason.USER_STOPPED, 1)))
        assertTrue(diagnosis.has(FindingCode.USER_FORCE_STOPPED))
    }

    @Test fun missingExitHistoryIsALimitationNotAFailure() {
        val diagnosis = Diagnoser.diagnose(snapshot(exits = null, bucket = null, restricted = null))
        assertTrue(Limitation.EXIT_HISTORY_UNAVAILABLE in diagnosis.limitations)
        assertTrue(diagnosis.isHealthy)
    }
}
