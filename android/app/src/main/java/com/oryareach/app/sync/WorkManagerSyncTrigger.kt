package com.oryareach.app.sync

import android.content.Context
import com.oryareach.core.sync.SyncTrigger

class WorkManagerSyncTrigger(private val context: Context) : SyncTrigger {
    override fun syncNow() = SyncWorker.syncNow(context)
}
