package com.oryareach.core.crypto

import com.oryareach.core.common.AppError
import com.oryareach.core.common.AppResult
import io.kotest.matchers.shouldBe
import org.junit.Assert.assertTrue
import org.junit.Test

class RecoveryPhraseTest {

    /**
     * Official BIP-39 English test vectors for 256-bit entropy, from
     * https://github.com/trezor/python-mnemonic/blob/master/vectors.json
     */
    @Test
    fun `matches the official BIP-39 vectors`() {
        val vectors = listOf(
            "00".repeat(32) to
                ("abandon ".repeat(23) + "art"),
            "7f".repeat(32) to
                "legal winner thank year wave sausage worth useful legal winner thank year " +
                "wave sausage worth useful legal winner thank year wave sausage worth title",
            "80".repeat(32) to
                "letter advice cage absurd amount doctor acoustic avoid letter advice cage " +
                "absurd amount doctor acoustic avoid letter advice cage absurd amount doctor " +
                "acoustic bless",
            "ff".repeat(32) to
                ("zoo ".repeat(23) + "vote"),
        )

        for ((hex, expected) in vectors) {
            val key = WorkspaceKey(hex.hexToByteArray())

            RecoveryPhrase.encode(key).joinToString(" ") shouldBe expected
            RecoveryPhrase.decode(expected).shouldBeSuccess()
                .bytes().contentEquals(hex.hexToByteArray()) shouldBe true
        }
    }

    @Test
    fun `round trips a generated key`() {
        val key = WorkspaceKey.generate()

        val phrase = RecoveryPhrase.encode(key)
        phrase.size shouldBe RecoveryPhrase.WORD_COUNT

        RecoveryPhrase.decode(phrase).shouldBeSuccess()
            .bytes().contentEquals(key.bytes()) shouldBe true
    }

    @Test
    fun `tolerates casing and irregular whitespace`() {
        val key = WorkspaceKey.generate()
        val phrase = RecoveryPhrase.encode(key).joinToString(" ")

        val messy = "  ${phrase.uppercase().replace(" ", "\n  ")}  "

        RecoveryPhrase.decode(messy).shouldBeSuccess()
            .bytes().contentEquals(key.bytes()) shouldBe true
    }

    @Test
    fun `rejects a phrase with a swapped word via the checksum`() {
        val phrase = RecoveryPhrase.encode(WorkspaceKey.generate()).toMutableList()
        // Swap one word for a different valid word: the words are all legal, only the
        // checksum reveals that the phrase as a whole is wrong.
        phrase[3] = if (phrase[3] == "zoo") "abandon" else "zoo"

        RecoveryPhrase.decode(phrase) shouldBe AppResult.Failure(AppError.Crypto.KeyUnavailable)
    }

    @Test
    fun `rejects a word that is not in the list`() {
        val phrase = RecoveryPhrase.encode(WorkspaceKey.generate()).toMutableList()
        phrase[10] = "notaword"

        RecoveryPhrase.decode(phrase) shouldBe AppResult.Failure(AppError.Crypto.KeyUnavailable)
    }

    @Test
    fun `rejects a phrase of the wrong length`() {
        val phrase = RecoveryPhrase.encode(WorkspaceKey.generate())

        RecoveryPhrase.decode(phrase.dropLast(1)) shouldBe
            AppResult.Failure(AppError.Crypto.KeyUnavailable)
        RecoveryPhrase.decode(phrase + "zoo") shouldBe
            AppResult.Failure(AppError.Crypto.KeyUnavailable)
        RecoveryPhrase.decode("") shouldBe
            AppResult.Failure(AppError.Crypto.KeyUnavailable)
    }

    @Test
    fun `a recovered key decrypts records written by the original`() {
        val cipher = RecordCipher()
        val original = WorkspaceKey.generate()
        val payload = """{"cycle":"2026-08-15"}""".toByteArray()
        val record = cipher.encrypt(original, payload)

        val recovered = RecoveryPhrase.decode(RecoveryPhrase.encode(original)).shouldBeSuccess()

        cipher.decrypt(recovered, record).shouldBeSuccess().contentEquals(payload) shouldBe true
    }

    @Test
    fun `distinct keys produce distinct phrases`() {
        val a = RecoveryPhrase.encode(WorkspaceKey.generate())
        val b = RecoveryPhrase.encode(WorkspaceKey.generate())

        (a == b) shouldBe false
    }

    private fun <T> AppResult<T>.shouldBeSuccess(): T {
        assertTrue("expected success but was $this", this is AppResult.Success)
        return (this as AppResult.Success).data
    }

    private fun String.hexToByteArray(): ByteArray =
        chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}
