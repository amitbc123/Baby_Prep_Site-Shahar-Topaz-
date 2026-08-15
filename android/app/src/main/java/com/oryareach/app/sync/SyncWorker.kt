package com.oryareach.app.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.oryareach.core.common.AppResult
import com.oryareach.core.sync.SyncEngine
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

/**
 * Runs one sync cycle in the background.
 *
 * Retries only for transient failures. A conflict is not retried: replaying the same write
 * would conflict again forever, and the record is already parked for a person to resolve.
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {

    private val engine: SyncEngine by inject()

    override suspend fun doWork(): Result = when (val outcome = engine.sync()) {
        is AppResult.Success ->
            if (outcome.data.shouldRetry) Result.retry() else Result.success()

        // A hard failure is still worth another attempt with backoff; the outbox is intact.
        is AppResult.Failure -> if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
    }

    companion object {
        private const val MAX_ATTEMPTS = 5
        private const val ONE_SHOT = "sync-now"
        private const val PERIODIC = "sync-periodic"

        private val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        /** Called after a local write, so a change reaches the other device promptly. */
        fun syncNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context)
                // KEEP, not REPLACE: a burst of edits should coalesce into one run rather
                // than each edit cancelling the upload the previous one just scheduled.
                .enqueueUniqueWork(ONE_SHOT, ExistingWorkPolicy.KEEP, request)
        }

        /** Safety net for changes made on the other device while this one was idle. */
        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
