package com.oryareach.core.crypto

import java.security.SecureRandom

/**
 * The single symmetric key shared by the couple's devices. Every record and file in the
 * workspace is encrypted under it, and it never leaves a device in unwrapped form.
 */
class WorkspaceKey(bytes: ByteArray) {
    init {
        require(bytes.size == SIZE_BYTES) { "workspace key must be $SIZE_BYTES bytes, was ${bytes.size}" }
    }

    private val material: ByteArray = bytes.copyOf()

    /** Copy, so a caller wiping its array cannot blank the key held here. */
    fun bytes(): ByteArray = material.copyOf()

    /** Overwrite the key material once it is no longer needed. */
    fun destroy() {
        material.fill(0)
    }

    // Deliberately not data class / no toString override with content: keeps key bytes out of
    // logs, crash reports and debugger string dumps.
    override fun toString(): String = "WorkspaceKey(redacted)"

    companion object {
        const val SIZE_BYTES = 32

        fun generate(random: SecureRandom = SecureRandom()): WorkspaceKey =
            WorkspaceKey(ByteArray(SIZE_BYTES).also(random::nextBytes))
    }
}
