package com.oryareach.app.di

import android.content.Context
import android.content.Intent
import com.oryareach.core.database.OrYareachDatabase
import com.oryareach.core.security.LocalDataWiper

/**
 * Closes the open Room connection, deletes the encrypted database file (and its `-wal`/`-shm`
 * companions, via [Context.deleteDatabase]), then kills and relaunches the process so every
 * Koin singleton holding a reference to the now-deleted database is rebuilt from scratch —
 * see [LocalDataWiper]'s doc comment for why an in-place swap isn't safe here.
 */
class RoomLocalDataWiper(
    private val context: Context,
    private val database: OrYareachDatabase,
) : LocalDataWiper {

    override fun wipeAndRestart() {
        database.close()
        context.deleteDatabase(OrYareachDatabase.NAME)

        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        checkNotNull(launchIntent) { "No launch intent for ${context.packageName}" }
        val restartIntent = Intent.makeRestartActivityTask(launchIntent.component)
        context.startActivity(restartIntent)

        Runtime.getRuntime().exit(0)
    }
}
