package io.github.datalux.sbriga

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExitReasonTest {
    @Test fun systemCodesMapToMatchingReason() {
        assertEquals(ExitReason.SIGNALED, ExitReason.fromSystemCode(2))
        assertEquals(ExitReason.LOW_MEMORY, ExitReason.fromSystemCode(3))
        assertEquals(ExitReason.USER_STOPPED, ExitReason.fromSystemCode(11))
        assertEquals(ExitReason.FREEZER, ExitReason.fromSystemCode(14))
        assertEquals(ExitReason.PACKAGE_UPDATED, ExitReason.fromSystemCode(16))
    }

    @Test fun everyReasonRoundTripsByOrdinal() {
        ExitReason.entries.forEach { assertEquals(it, ExitReason.fromSystemCode(it.ordinal)) }
    }

    @Test fun unknownCodesBecomeUnknown() {
        assertEquals(ExitReason.UNKNOWN, ExitReason.fromSystemCode(999))
        assertEquals(ExitReason.UNKNOWN, ExitReason.fromSystemCode(-1))
    }

    @Test fun categoriesDoNotOverlap() {
        ExitReason.entries.forEach {
            val flags = listOf(it.isAppFault, it.isUserAction, it.isSuspectedSystemKill).count { f -> f }
            assertTrue("$it is in $flags categories", flags <= 1)
        }
        assertFalse(ExitReason.LOW_MEMORY.isSuspectedSystemKill)
    }
}
