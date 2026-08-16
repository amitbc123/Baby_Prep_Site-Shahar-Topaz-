package com.oryareach.core.security

import java.security.SecureRandom

/**
 * The code one partner reads out to the other.
 *
 * 20 characters from a 32-symbol alphabet is 100 bits of entropy — far beyond guessing, while
 * still short enough to type. The alphabet drops the characters that get misread aloud or on
 * screen (0/O, 1/I/L, U/V), so a code that sounds right is the code that was meant.
 */
object InvitationToken {

    private const val ALPHABET = "ABCDEFGHJKMNPQRSTWXYZ23456789"
    private const val LENGTH = 20
    private const val GROUP = 5

    fun generate(random: SecureRandom = SecureRandom()): String =
        (0 until LENGTH)
            .map { ALPHABET[random.nextInt(ALPHABET.length)] }
            .joinToString("")

    /** Grouped for reading aloud: ABCDE-FGHJK-MNPQR-STWXY */
    fun forDisplay(token: String): String = token.chunked(GROUP).joinToString("-")

    /** Accepts whatever the user typed: spaces, dashes, lowercase. */
    fun normalize(input: String): String =
        input.uppercase().filter { it in ALPHABET }

    fun isWellFormed(token: String): Boolean =
        token.length == LENGTH && token.all { it in ALPHABET }
}
