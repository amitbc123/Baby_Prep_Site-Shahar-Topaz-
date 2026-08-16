package com.oryareach.core.update

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import com.oryareach.core.common.AppError
import java.io.File
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

sealed interface InstallOutcome {
    data object Success : InstallOutcome
    /** The system needs the user to confirm the install; launch [intent] to show that prompt. */
    data class PendingUserAction(val intent: Intent) : InstallOutcome
    data class Failed(val message: String) : InstallOutcome
}

/**
 * Installs a downloaded APK through the session-based `PackageInstaller` API. This must be
 * signed with the same key as the running app — Android silently rejects a mismatched signer,
 * which is why the release keystore staying constant matters more than anything else here.
 */
class UpdateInstaller(private val context: Context) {

    fun install(apk: File): kotlinx.coroutines.flow.Flow<InstallOutcome> = callbackFlow {
        val installer = context.packageManager.packageInstaller
        val action = "${context.packageName}.UPDATE_INSTALL_STATUS"

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receivedContext: Context, intent: Intent) {
                when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
                    PackageInstaller.STATUS_SUCCESS -> trySend(InstallOutcome.Success)

                    PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                        @Suppress("DEPRECATION")
                        val confirmIntent = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                        if (confirmIntent != null) {
                            trySend(InstallOutcome.PendingUserAction(confirmIntent))
                        } else {
                            trySend(InstallOutcome.Failed("no confirmation intent for pending user action"))
                        }
                    }

                    else -> {
                        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                            ?: "install failed with status $status"
                        trySend(InstallOutcome.Failed(message))
                        close()
                    }
                }
            }
        }

        context.registerReceiver(receiver, IntentFilter(action), Context.RECEIVER_NOT_EXPORTED)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(action).setPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )

        val sessionParams = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        val sessionId = installer.createSession(sessionParams)
        val session = installer.openSession(sessionId)

        try {
            session.openWrite("update", 0, apk.length()).use { out ->
                apk.inputStream().use { it.copyTo(out) }
                session.fsync(out)
            }
            session.commit(pendingIntent.intentSender)
        } catch (e: Exception) {
            trySend(InstallOutcome.Failed(e.message ?: "install session failed"))
            close()
        } finally {
            session.close()
        }

        awaitClose { context.unregisterReceiver(receiver) }
    }

    /** Whether this app is currently allowed to install packages, without prompting. */
    fun canRequestInstalls(): Boolean = context.packageManager.canRequestPackageInstalls()
}

internal fun InstallOutcome.Failed.toAppError(): AppError = AppError.Unexpected(message)
