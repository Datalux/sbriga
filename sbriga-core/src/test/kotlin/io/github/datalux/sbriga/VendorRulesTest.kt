package io.github.datalux.sbriga

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class VendorRulesTest {
    @Test fun parsesCandidatesInOrderAndFiltersByTarget() {
        val rules = VendorRules.fromJson(
            """{"vendors":{"huawei":[
                {"target":"AUTOSTART","package":"a","class":"a.One"},
                {"target":"BATTERY","package":"b","class":"b.Two"},
                {"target":"AUTOSTART","package":"c","class":"c.Three","verified":true}
            ]}}""",
        )
        val autostart = rules.candidates(Vendor.HUAWEI, SettingsTarget.AUTOSTART)
        assertEquals(listOf("a.One", "c.Three"), autostart.map { it.className })
        assertEquals(listOf(false, true), autostart.map { it.verified })
        assertEquals(1, rules.candidates(Vendor.HUAWEI, SettingsTarget.BATTERY).size)
    }

    @Test fun skipsMalformedEntriesAndUnknownVendors() {
        val rules = VendorRules.fromJson(
            """{"vendors":{"nokia":[{"target":"AUTOSTART","package":"a","class":"b"}],
                "xiaomi":[{"target":"NOPE","package":"a","class":"b"},{"target":"AUTOSTART","package":"","class":"b"}]}}""",
        )
        assertTrue(rules.candidates(Vendor.XIAOMI, SettingsTarget.AUTOSTART).isEmpty())
    }

    @Test fun invalidJsonYieldsEmptyRules() {
        assertTrue(VendorRules.fromJson("not json").candidates(Vendor.XIAOMI, SettingsTarget.AUTOSTART).isEmpty())
    }

    @Test fun bundledAssetParsesAndCoversMainVendors() {
        val asset = File("src/main/assets/sbriga-vendors.json")
        val rules = VendorRules.fromJson(asset.readText())
        for (vendor in listOf(Vendor.XIAOMI, Vendor.HUAWEI, Vendor.OPPO, Vendor.VIVO)) {
            assertTrue("$vendor has no autostart candidate", rules.candidates(vendor, SettingsTarget.AUTOSTART).isNotEmpty())
        }
        assertTrue(rules.candidates(Vendor.SAMSUNG, SettingsTarget.BATTERY).isNotEmpty())
    }
}
