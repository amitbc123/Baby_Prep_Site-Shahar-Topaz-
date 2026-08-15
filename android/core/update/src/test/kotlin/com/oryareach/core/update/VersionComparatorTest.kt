package com.oryareach.core.update

import io.kotest.matchers.shouldBe
import org.junit.Test

class VersionComparatorTest {

    @Test
    fun `numeric components compare numerically, not lexicographically`() {
        VersionComparator.isNewer("1.10.0", "1.9.0") shouldBe true
        VersionComparator.isNewer("1.9.0", "1.10.0") shouldBe false
    }

    @Test
    fun `equal versions are neither newer`() {
        VersionComparator.compare("1.4.0", "1.4.0") shouldBe 0
    }

    @Test
    fun `a release outranks its own prerelease`() {
        VersionComparator.isNewer("1.4.0", "1.4.0-rc.1") shouldBe true
        VersionComparator.isNewer("1.4.0-rc.1", "1.4.0") shouldBe false
    }

    @Test
    fun `prerelease identifiers compare numeric before alphanumeric`() {
        VersionComparator.isNewer("1.4.0-rc.2", "1.4.0-rc.1") shouldBe true
        VersionComparator.isNewer("1.4.0-beta", "1.4.0-alpha") shouldBe true
    }

    @Test
    fun `patch and minor bumps are detected`() {
        VersionComparator.isNewer("1.4.1", "1.4.0") shouldBe true
        VersionComparator.isNewer("2.0.0", "1.99.99") shouldBe true
    }
}
