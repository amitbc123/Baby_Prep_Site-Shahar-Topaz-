package com.oryareach.core.sync

import com.oryareach.core.common.AppError
import com.oryareach.core.common.AppResult
import com.oryareach.core.crypto.WorkspaceKey
import com.oryareach.core.model.EntityType
import io.kotest.matchers.shouldBe
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordCodecTest {

    private val key = WorkspaceKey.generate()
    private val codec = RecordCodec(keys = { key })
    private val json = """{"title":"לקבוע תור למיילדת","done":false}"""

    @Test
    fun `round trips a payload`() {
        val blob = codec.encode(EntityType.TASK, "task-1", json).shouldBeSuccess()

        codec.decode(EntityType.TASK, "task-1", blob).shouldBeSuccess() shouldBe json
    }

    @Test
    fun `the encoded blob does not contain the plaintext`() {
        val blob = codec.encode(EntityType.TASK, "task-1", json).shouldBeSuccess()

        String(blob, Charsets.ISO_8859_1).contains("מיילדת") shouldBe false
        blob.indexOfSlice(json.toByteArray()) shouldBe -1
    }

    @Test
    fun `a blob cannot be replayed under a different record id`() {
        val blob = codec.encode(EntityType.TASK, "task-1", json).shouldBeSuccess()

        codec.decode(EntityType.TASK, "task-2", blob) shouldBe
            AppResult.Failure(AppError.Crypto.DecryptionFailed)
    }

    @Test
    fun `a blob cannot be reinterpreted as a different entity type`() {
        // Without this binding, a task payload could be fed back as a cycle record.
        val blob = codec.encode(EntityType.TASK, "rec-1", json).shouldBeSuccess()

        codec.decode(EntityType.CYCLE, "rec-1", blob) shouldBe
            AppResult.Failure(AppError.Crypto.DecryptionFailed)
    }

    @Test
    fun `a different workspace key cannot decode`() {
        val blob = codec.encode(EntityType.TASK, "task-1", json).shouldBeSuccess()
        val other = RecordCodec(keys = { WorkspaceKey.generate() })

        other.decode(EntityType.TASK, "task-1", blob) shouldBe
            AppResult.Failure(AppError.Crypto.DecryptionFailed)
    }

    @Test
    fun `a locked workspace reports a missing key rather than failing obscurely`() {
        val locked = RecordCodec(keys = { null })

        locked.encode(EntityType.TASK, "task-1", json) shouldBe
            AppResult.Failure(AppError.Crypto.KeyUnavailable)
        locked.decode(EntityType.TASK, "task-1", byteArrayOf(1, 2, 3)) shouldBe
            AppResult.Failure(AppError.Crypto.KeyUnavailable)
    }

    @Test
    fun `survives a round trip through the server's base64 transport`() {
        val blob = codec.encode(EntityType.CYCLE, "cycle-9", json).shouldBeSuccess()

        val encoded = java.util.Base64.getEncoder().encodeToString(blob)
        val decoded = java.util.Base64.getDecoder().decode(encoded)

        codec.decode(EntityType.CYCLE, "cycle-9", decoded).shouldBeSuccess() shouldBe json
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
