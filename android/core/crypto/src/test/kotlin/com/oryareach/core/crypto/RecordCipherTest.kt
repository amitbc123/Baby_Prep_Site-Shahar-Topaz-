package com.oryareach.core.crypto

import com.oryareach.core.common.AppError
import com.oryareach.core.common.AppResult
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordCipherTest {

    private val cipher = RecordCipher()
    private val key = WorkspaceKey.generate()
    private val plaintext = """{"title":"קניית עגלה","done":false}""".toByteArray()

    @Test
    fun `round trips a payload`() {
        val envelope = cipher.encrypt(key, plaintext)
        val decrypted = cipher.decrypt(key, envelope)

        decrypted.shouldBeSuccess().contentEquals(plaintext) shouldBe true
    }

    @Test
    fun `round trips an empty payload`() {
        val envelope = cipher.encrypt(key, ByteArray(0))

        cipher.decrypt(key, envelope).shouldBeSuccess().size shouldBe 0
    }

    @Test
    fun `ciphertext never contains the plaintext`() {
        val envelope = cipher.encrypt(key, plaintext)

        envelope.indexOfSlice(plaintext) shouldBe -1
    }

    @Test
    fun `encrypting the same payload twice gives different ciphertexts`() {
        val first = cipher.encrypt(key, plaintext)
        val second = cipher.encrypt(key, plaintext)

        first.contentEquals(second) shouldBe false
    }

    @Test
    fun `a flipped ciphertext byte fails authentication rather than decrypting`() {
        val envelope = cipher.encrypt(key, plaintext)
        envelope[envelope.size - 1] = (envelope[envelope.size - 1].toInt() xor 0x01).toByte()

        cipher.decrypt(key, envelope) shouldBe AppResult.Failure(AppError.Crypto.DecryptionFailed)
    }

    @Test
    fun `a flipped salt byte fails authentication`() {
        val envelope = cipher.encrypt(key, plaintext)
        envelope[1] = (envelope[1].toInt() xor 0x01).toByte()

        cipher.decrypt(key, envelope) shouldBe AppResult.Failure(AppError.Crypto.DecryptionFailed)
    }

    @Test
    fun `a truncated envelope fails rather than throwing`() {
        val envelope = cipher.encrypt(key, plaintext)

        cipher.decrypt(key, envelope.copyOf(envelope.size - 4)) shouldBe
            AppResult.Failure(AppError.Crypto.DecryptionFailed)
        cipher.decrypt(key, ByteArray(0)) shouldBe
            AppResult.Failure(AppError.Crypto.DecryptionFailed)
    }

    @Test
    fun `an unknown envelope version is reported distinctly`() {
        val envelope = cipher.encrypt(key, plaintext)
        envelope[0] = 99

        cipher.decrypt(key, envelope) shouldBe
            AppResult.Failure(AppError.Crypto.UnsupportedEnvelopeVersion)
    }

    @Test
    fun `a different workspace key cannot decrypt`() {
        val envelope = cipher.encrypt(key, plaintext)

        cipher.decrypt(WorkspaceKey.generate(), envelope) shouldBe
            AppResult.Failure(AppError.Crypto.DecryptionFailed)
    }

    @Test
    fun `associated data is bound to the ciphertext`() {
        val envelope = cipher.encrypt(key, plaintext, aad = "record-a:v3".toByteArray())

        cipher.decrypt(key, envelope, aad = "record-a:v3".toByteArray())
            .shouldBeSuccess().contentEquals(plaintext) shouldBe true
        cipher.decrypt(key, envelope, aad = "record-b:v3".toByteArray()) shouldBe
            AppResult.Failure(AppError.Crypto.DecryptionFailed)
        cipher.decrypt(key, envelope) shouldBe
            AppResult.Failure(AppError.Crypto.DecryptionFailed)
    }

    @Test
    fun `destroying a key zeroes its material`() {
        val victim = WorkspaceKey.generate()
        victim.bytes().any { it != 0.toByte() } shouldBe true

        victim.destroy()

        victim.bytes().all { it == 0.toByte() } shouldBe true
    }

    @Test
    fun `toString does not leak key material`() {
        WorkspaceKey.generate().toString() shouldBe "WorkspaceKey(redacted)"
    }

    @Test
    fun `rejects a key of the wrong length`() {
        val thrown = runCatching { WorkspaceKey(ByteArray(16)) }.exceptionOrNull()

        thrown shouldNotBe null
        assertTrue(thrown is IllegalArgumentException)
    }

    private fun AppResult<ByteArray>.shouldBeSuccess(): ByteArray {
        assertTrue("expected success but was $this", this is AppResult.Success)
        return (this as AppResult.Success).data
    }

    private fun ByteArray.indexOfSlice(needle: ByteArray): Int {
        if (needle.isEmpty() || needle.size > size) return -1
        outer@ for (start in 0..size - needle.size) {
            for (i in needle.indices) if (this[start + i] != needle[i]) continue@outer
            return start
        }
        return -1
    }
}
