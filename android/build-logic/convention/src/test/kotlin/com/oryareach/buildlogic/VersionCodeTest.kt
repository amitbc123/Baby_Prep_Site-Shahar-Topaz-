package com.oryareach.buildlogic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class VersionCodeTest {

    @Test
    fun `maps semantic versions to ordered codes`() {
        assertEquals(10000, versionNameToCode("1.0.0"))
        assertEquals(10400, versionNameToCode("1.4.0"))
        assertEquals(10401, versionNameToCode("1.4.1"))
        assertEquals(20000, versionNameToCode("2.0.0"))
    }

    @Test
    fun `orders 1_10_0 above 1_9_0`() {
        assert(versionNameToCode("1.10.0") > versionNameToCode("1.9.0"))
    }

    @Test
    fun `ignores the prerelease suffix`() {
        assertEquals(10300, versionNameToCode("1.3.0-rc.1"))
    }

    @Test
    fun `falls back to 1 for an unparseable version`() {
        assertEquals(1, versionNameToCode(FALLBACK_VERSION_NAME))
        assertEquals(1, versionNameToCode("nonsense"))
    }

    @Test
    fun `rejects versions that would overflow the scheme`() {
        assertThrows(IllegalArgumentException::class.java) { versionNameToCode("1.100.0") }
        assertThrows(IllegalArgumentException::class.java) { versionNameToCode("1.0.100") }
    }
}
