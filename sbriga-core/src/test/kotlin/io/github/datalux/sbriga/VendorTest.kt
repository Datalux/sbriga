package io.github.datalux.sbriga

import org.junit.Assert.assertEquals
import org.junit.Test

class VendorTest {
    @Test fun detectsFromManufacturer() {
        assertEquals(Vendor.XIAOMI, Vendor.detect("Xiaomi"))
        assertEquals(Vendor.SAMSUNG, Vendor.detect("samsung"))
        assertEquals(Vendor.HUAWEI, Vendor.detect("HUAWEI"))
    }

    @Test fun subBrandsMapToParentFamily() {
        assertEquals(Vendor.XIAOMI, Vendor.detect("Xiaomi", "POCO"))
        assertEquals(Vendor.XIAOMI, Vendor.detect("unknown", "Redmi"))
        assertEquals(Vendor.HUAWEI, Vendor.detect("HONOR"))
        assertEquals(Vendor.OPPO, Vendor.detect("realme"))
        assertEquals(Vendor.VIVO, Vendor.detect("vivo", "iQOO"))
    }

    @Test fun unknownAndNullFallBackToOther() {
        assertEquals(Vendor.OTHER, Vendor.detect("Google", "google"))
        assertEquals(Vendor.OTHER, Vendor.detect(null, null))
    }

    @Test fun samsungIsNotTreatedAsAggressive() {
        assert(!Vendor.SAMSUNG.isAggressive)
        assert(!Vendor.OTHER.isAggressive)
        assert(Vendor.XIAOMI.isAggressive)
    }
}
