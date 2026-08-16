package com.oryareach.app.notifications

import android.content.Context
import com.oryareach.core.settings.ReminderScheduler

class WorkManagerReminderScheduler(private val context: Context) : ReminderScheduler {
    override fun schedule() = ReminderWorker.schedule(context)
    override fun cancel() = ReminderWorker.cancel(context)
}
