package com.oryareach.core.crypto

import com.oryareach.core.common.AppError
import com.oryareach.core.common.AppResult
import io.kotest.matchers.shouldBe
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyWrapTest {

    private val keyWrap = KeyWrap()

    @Test
    fun `a joining device recovers the workspace key`() {
        val workspaceKey = WorkspaceKey.generate()
        val joining = keyWrap.generateDeviceKeyPair()

        val blob = keyWrap.wrap(workspaceKey, joining.publicKey)
        val recovered = keyWrap.unwrap(blob, joining).shouldBeSuccess()

        recovered.bytes().contentEquals(workspaceKey.bytes()) shouldBe true
    }

    @Test
    fun `the wrapped blob never contains the raw key`() {
        val workspaceKey = WorkspaceKey.generate()
        val joining = keyWrap.generateDeviceKeyPair()

        val blob = keyWrap.wrap(workspaceKey, joining.publicKey)

        blob.indexOfSlice(workspaceKey.bytes()) shouldBe -1
    }

    @Test
    fun `wrapping twice produces different blobs`() {
        val workspaceKey = WorkspaceKey.generate()
        val joining = keyWrap.generateDeviceKeyPair()

        val first = keyWrap.wrap(workspaceKey, joining.publicKey)
        val second = keyWrap.wrap(workspaceKey, joining.publicKey)

        first.contentEquals(second) shouldBe false
    }

    @Test
    fun `another device cannot open the blob`() {
        val workspaceKey = WorkspaceKey.generate()
        val joining = keyWrap.generateDeviceKeyPair()
        val eavesdropper = keyWrap.generateDeviceKeyPair()

        val blob = keyWrap.wrap(workspaceKey, joining.publicKey)

        keyWrap.unwrap(blob, eavesdropper) shouldBe
            AppResult.Failure(AppError.Crypto.DecryptionFailed)
    }

    @Test
    fun `a tampered blob fails authentication`() {
        val workspaceKey = WorkspaceKey.generate()
        val joining = keyWrap.generateDeviceKeyPair()

        val blob = keyWrap.wrap(workspaceKey, joining.publicKey)
        blob[blob.size - 1] = (blob[blob.size - 1].toInt() xor 0x01).toByte()

        keyWrap.unwrap(blob, joining) shouldBe AppResult.Failure(AppError.Crypto.DecryptionFailed)
    }

    @Test
    fun `a tampered encapsulated key fails authentication`() {
        val workspaceKey = WorkspaceKey.generate()
        val joining = keyWrap.generateDeviceKeyPair()

        val blob = keyWrap.wrap(workspaceKey, joining.publicKey)
        blob[5] = (blob[5].toInt() xor 0x01).toByte()

        keyWrap.unwrap(blob, joining) shouldBe AppResult.Failure(AppError.Crypto.DecryptionFailed)
    }

    @Test
    fun `a truncated blob fails rather than throwing`() {
        val joining = keyWrap.generateDeviceKeyPair()

        keyWrap.unwrap(ByteArray(0), joining) shouldBe
            AppResult.Failure(AppError.Crypto.DecryptionFailed)
        keyWrap.unwrap(ByteArray(20), joining) shouldBe
            AppResult.Failure(AppError.Crypto.DecryptionFailed)
    }

    @Test
    fun `an unknown blob version is reported distinctly`() {
        val joining = keyWrap.generateDeviceKeyPair()
        val blob = keyWrap.wrap(WorkspaceKey.generate(), joining.publicKey)
        blob[0] = 99

        keyWrap.unwrap(blob, joining) shouldBe
            AppResult.Failure(AppError.Crypto.UnsupportedEnvelopeVersion)
    }

    @Test
    fun `generated device key pairs are distinct`() {
        val a = keyWrap.generateDeviceKeyPair()
        val b = keyWrap.generateDeviceKeyPair()

        a.publicKey.contentEquals(b.publicKey) shouldBe false
        a.publicKey.size shouldBe DeviceKeyPair.KEY_BYTES
    }

    @Test
    fun `a key transferred by pairing decrypts records written by the other device`() {
        // The end-to-end property that matters: A writes a record, B joins, B reads it.
        val cipher = RecordCipher()
        val workspaceKey = WorkspaceKey.generate()
        val payload = """{"title":"בדיקת דם"}""".toByteArray()
        val record = cipher.encrypt(workspaceKey, payload)

        val joining = keyWrap.generateDeviceKeyPair()
        val transferred = keyWrap.unwrap(
            keyWrap.wrap(workspaceKey, joining.publicKey),
            joining,
        ).shouldBeSuccess()

        cipher.decrypt(transferred, record).shouldBeSuccess().contentEquals(payload) shouldBe true
    }

    @Test
    fun `toString does not leak the private key`() {
        keyWrap.generateDeviceKeyPair().toString() shouldBe
            "DeviceKeyPair(public=32B, private=redacted)"
    }

    private fun <T> AppResult<T>.shouldBeSuccess(): T {
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
