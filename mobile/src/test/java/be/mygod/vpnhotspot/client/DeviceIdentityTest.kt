package be.mygod.vpnhotspot.client

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeviceIdentityTest {
    private val vendors = mapOf("001122" to "Apple", "021122" to "Samsung")

    @Test fun randomMacDoesNotPretendToIdentifyManufacturer() {
        assertNull(DeviceIdentity.guess("02:11:22:33:44:55", emptyList(), vendors))
        assertNull(DeviceIdentity.guess("01:11:22:33:44:55", emptyList(), vendors))
    }
    @Test fun hostnameCanIdentifyPhoneDespiteRandomMac() {
        assertEquals("Samsung SM-G991B", DeviceIdentity.guess("02:11:22:33:44:55", listOf("SM-G991B"), vendors))
        assertEquals("Apple iPhone", DeviceIdentity.guess("02:11:22:33:44:55", listOf("iPhone-Ali.local"), vendors))
    }
    @Test fun brandPrefixDoesNotClaimAnExactModelOrMatchUnrelatedWords() {
        assertEquals("Google Pixel", DeviceIdentity.guess("02:11:22:33:44:55", listOf("Pixel-8"), vendors))
        assertNull(DeviceIdentity.guess("02:11:22:33:44:55", listOf("pixelart-workstation"), vendors))
        assertEquals("Apple", DeviceIdentity.guess("00:11:22:33:44:55", listOf("Ali"), vendors))
    }
    @Test fun malformedMacIsIgnoredAndTableParserHandlesInvalidRows() {
        assertNull(DeviceIdentity.guess("bad", emptyList(), vendors))
        assertEquals(mapOf("001122" to "Apple"), DeviceIdentity.readManufacturers(
            "001122\tApple\ninvalid\n".byteInputStream()))
    }
}
