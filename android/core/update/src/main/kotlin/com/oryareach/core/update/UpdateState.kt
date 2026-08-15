package com.oryareach.core.update

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.updateDataStore: DataStore<Preferences> by preferencesDataStore(name = "update-state")

/**
 * Deliberately outside the encrypted workspace: it must be readable before the user unlocks
 * anything, and none of it (a timestamp, a version string) is sensitive.
 */
class UpdateState(private val context: Context) {

    private object Keys {
        val lastCheckedAt = longPreferencesKey("last_checked_at")
        val lastNotifiedVersion = stringPreferencesKey("last_notified_version")
        val skippedVersion = stringPreferencesKey("skipped_version")
    }

    val lastCheckedAt: Flow<Long?> = context.updateDataStore.data.map { it[Keys.lastCheckedAt] }
    val skippedVersion: Flow<String?> = context.updateDataStore.data.map { it[Keys.skippedVersion] }

    suspend fun recordCheck(atEpochMillis: Long) {
        context.updateDataStore.edit { it[Keys.lastCheckedAt] = atEpochMillis }
    }

    suspend fun recordNotified(version: String) {
        context.updateDataStore.edit { it[Keys.lastNotifiedVersion] = version }
    }

    suspend fun lastNotifiedVersion(): String? =
        context.updateDataStore.data.map { it[Keys.lastNotifiedVersion] }.first()

    suspend fun skip(version: String) {
        context.updateDataStore.edit { it[Keys.skippedVersion] = version }
    }

    suspend fun isSkipped(version: String): Boolean =
        context.updateDataStore.data.map { it[Keys.skippedVersion] }.first() == version
}
