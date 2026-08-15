package com.oryareach.core.update

/**
 * Semantic-version compare for `major.minor.patch[-prerelease]`. Numeric components compare
 * numerically (`1.9.0 < 1.10.0`, not lexicographically), and a prerelease sorts below its own
 * release (`1.4.0-rc.1 < 1.4.0`), per semver precedence rules.
 */
object VersionComparator {

    fun compare(a: String, b: String): Int {
        val (coreA, preA) = a.split('-', limit = 2).let { it[0] to it.getOrNull(1) }
        val (coreB, preB) = b.split('-', limit = 2).let { it[0] to it.getOrNull(1) }

        val core = compareCores(coreA, coreB)
        if (core != 0) return core

        return when {
            preA == null && preB == null -> 0
            preA == null -> 1
            preB == null -> -1
            else -> comparePrerelease(preA, preB)
        }
    }

    fun isNewer(candidate: String, than: String): Boolean = compare(candidate, than) > 0

    private fun compareCores(a: String, b: String): Int {
        val partsA = a.split('.').map { it.toIntOrNull() ?: 0 }
        val partsB = b.split('.').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(partsA.size, partsB.size)) {
            val diff = (partsA.getOrElse(i) { 0 }).compareTo(partsB.getOrElse(i) { 0 })
            if (diff != 0) return diff
        }
        return 0
    }

    private fun comparePrerelease(a: String, b: String): Int {
        val idsA = a.split('.')
        val idsB = b.split('.')
        for (i in 0 until maxOf(idsA.size, idsB.size)) {
            val idA = idsA.getOrNull(i) ?: return -1
            val idB = idsB.getOrNull(i) ?: return 1

            val numA = idA.toIntOrNull()
            val numB = idB.toIntOrNull()
            val diff = when {
                numA != null && numB != null -> numA.compareTo(numB)
                numA != null -> -1 // numeric identifiers sort before alphanumeric ones
                numB != null -> 1
                else -> idA.compareTo(idB)
            }
            if (diff != 0) return diff
        }
        return 0
    }
}
