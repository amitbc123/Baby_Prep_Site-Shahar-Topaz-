package com.oryareach.core.sync

/**
 * Kicks off a sync pass after a local write. Implemented over WorkManager in `:app`, kept as
 * an interface here so `:core:database` can call it without depending on Android's work API.
 */
fun interface SyncTrigger {
    fun syncNow()
}
