package com.oryareach.core.settings

/**
 * Schedules or cancels the local daily reminder notification. Kept as an interface, same seam
 * as `SyncTrigger`: the WorkManager-backed implementation lives in `:app`, so this module (and
 * anything that depends on it, like `:feature:settings`) never needs to depend on WorkManager.
 */
interface ReminderScheduler {
    fun schedule()
    fun cancel()
}
