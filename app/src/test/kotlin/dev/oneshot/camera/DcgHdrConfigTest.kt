package dev.oneshot.camera

import org.junit.Assert.assertEquals
import org.junit.Test

class DcgHdrConfigTest {

    @Test
    fun testResolveDcgModeValue() {
        assertEquals(1, DcgHdrConfig.resolveDcgValue(enabled = true, supported = true))
        assertEquals(0, DcgHdrConfig.resolveDcgValue(enabled = false, supported = true))
        assertEquals(0, DcgHdrConfig.resolveDcgValue(enabled = true, supported = false))
        assertEquals(0, DcgHdrConfig.resolveDcgValue(enabled = false, supported = false))
    }

    @Test
    fun testDcgVendorKeyName() {
        assertEquals("org.codeaurora.qcamera3.sessionParameters.EnableHDRDCGMode", DcgHdrConfig.QUALCOMM_DCG_VENDOR_TAG)
    }
}
