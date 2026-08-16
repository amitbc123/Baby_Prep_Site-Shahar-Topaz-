package com.oryareach.core.security

import io.kotest.matchers.shouldBe
import org.junit.Test

class InvitationTokenTest {

    @Test
    fun `generates a well formed token`() {
        repeat(50) {
            val token = InvitationToken.generate()
            token.length shouldBe 20
            InvitationToken.isWellFormed(token) shouldBe true
        }
    }

    @Test
    fun `tokens do not repeat`() {
        val tokens = List(500) { InvitationToken.generate() }

        tokens.toSet().size shouldBe tokens.size
    }

    @Test
    fun `excludes characters that are misread`() {
        val generated = List(200) { InvitationToken.generate() }.joinToString("")

        // 0/O, 1/I/L and U/V are the pairs people confuse when reading a code aloud.
        generated.none { it in "01ILOUV" } shouldBe true
    }

    @Test
    fun `display grouping is reversible`() {
        val token = InvitationToken.generate()

        val displayed = InvitationToken.forDisplay(token)
        displayed shouldBe "${token.take(5)}-${token.drop(5).take(5)}-" +
            "${token.drop(10).take(5)}-${token.drop(15).take(5)}"

        InvitationToken.normalize(displayed) shouldBe token
    }

    @Test
    fun `normalize accepts what a user actually types`() {
        val token = InvitationToken.generate()
        val messy = "  " + InvitationToken.forDisplay(token).lowercase().replace("-", " ") + "  "

        InvitationToken.normalize(messy) shouldBe token
    }

    @Test
    fun `rejects a token of the wrong shape`() {
        InvitationToken.isWellFormed("") shouldBe false
        InvitationToken.isWellFormed("ABCDE") shouldBe false
        InvitationToken.isWellFormed("A".repeat(21)) shouldBe false
        // Contains an excluded character.
        InvitationToken.isWellFormed("ABCDEFGHJKMNPQRSTWX0") shouldBe false
    }
}
