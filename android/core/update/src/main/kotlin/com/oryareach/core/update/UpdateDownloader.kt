package com.oryareach.core.update

import android.content.Context
import com.oryareach.core.common.AppError
import com.oryareach.core.common.AppResult
import io.ktor.client.HttpClient
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.isSuccess
import io.ktor.utils.io.jvm.javaio.copyTo
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface DownloadProgress {
    data class InProgress(val bytesDownloaded: Long, val totalBytes: Long) : DownloadProgress
    data class Done(val file: File) : DownloadProgress
    data class Failed(val error: AppError) : DownloadProgress
}

/** Downloads the release APK into the app's cache dir, reporting progress as it streams. */
class UpdateDownloader(
    private val context: Context,
    private val client: HttpClient = ReleaseChecker.defaultClient(),
) {

    private val _progress = MutableStateFlow<DownloadProgress?>(null)
    val progress: StateFlow<DownloadProgress?> = _progress.asStateFlow()

    suspend fun download(manifest: ReleaseManifest): AppResult<File> {
        val target = targetFile(manifest.version)

        return try {
            val response = client.get(manifest.asset.url) {
                onDownload { bytesSentTotal, contentLength ->
                    _progress.value = DownloadProgress.InProgress(
                        bytesDownloaded = bytesSentTotal,
                        totalBytes = contentLength ?: manifest.asset.sizeBytes,
                    )
                }
            }
            if (!response.status.isSuccess()) {
                val error = AppError.Network.Server(response.status.value)
                _progress.value = DownloadProgress.Failed(error)
                return AppResult.Failure(error)
            }

            target.outputStream().use { output -> response.bodyAsChannel().copyTo(output) }

            if (!IntegrityVerifier.matches(target, manifest.asset.sha256)) {
                target.delete()
                val error = AppError.Unexpected("downloaded APK does not match its published checksum")
                _progress.value = DownloadProgress.Failed(error)
                return AppResult.Failure(error)
            }

            _progress.value = DownloadProgress.Done(target)
            AppResult.Success(target)
        } catch (e: Exception) {
            target.delete()
            val error = e.toUpdateError()
            _progress.value = DownloadProgress.Failed(error)
            AppResult.Failure(error)
        }
    }

    private fun targetFile(version: String): File {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        return File(dir, "or-yareach-$version.apk")
    }
}
