package com.oryareach.core.update

import com.oryareach.core.common.AppError
import com.oryareach.core.common.AppResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json

sealed interface UpdateAvailability {
    data object UpToDate : UpdateAvailability
    data class Available(val manifest: ReleaseManifest) : UpdateAvailability
    data class Mandatory(val manifest: ReleaseManifest) : UpdateAvailability
}

/**
 * Fetches the newest GitHub release, reads its `manifest.json` asset, and compares it against
 * what is installed. `mandatory` on the manifest forces [UpdateAvailability.Mandatory]; so
 * does the installed version falling below the manifest's `minSupportedVersion`, even if the
 * release itself wasn't flagged mandatory — an old client may no longer speak to the backend.
 */
class ReleaseChecker(
    private val versionManager: VersionManager,
    private val client: HttpClient = defaultClient(),
    private val owner: String = BuildConfig.GITHUB_OWNER,
    private val repository: String = BuildConfig.GITHUB_REPOSITORY,
) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun check(): AppResult<UpdateAvailability> {
        val manifest = fetchManifest().let {
            when (it) {
                is AppResult.Failure -> return it
                is AppResult.Success -> it.data
            }
        }

        val current = versionManager.currentVersionName()
        val belowMinimum = manifest.minSupportedVersion?.let { min ->
            VersionComparator.compare(current, min) < 0
        } ?: false

        return AppResult.Success(
            when {
                !VersionComparator.isNewer(manifest.version, current) -> UpdateAvailability.UpToDate
                manifest.mandatory || belowMinimum -> UpdateAvailability.Mandatory(manifest)
                else -> UpdateAvailability.Available(manifest)
            },
        )
    }

    private suspend fun fetchManifest(): AppResult<ReleaseManifest> = try {
        val release = client.get("https://api.github.com/repos/$owner/$repository/releases/latest") {
            headers { append(HttpHeaders.Accept, "application/vnd.github+json") }
        }
        if (!release.status.isSuccess()) {
            return AppResult.Failure(AppError.Network.Server(release.status.value))
        }
        val dto = json.decodeFromString<GithubReleaseDto>(release.bodyAsText())
        val manifestAsset = dto.assets.firstOrNull { it.name == "manifest.json" }
            ?: return AppResult.Failure(AppError.Unexpected("release ${dto.tagName} has no manifest.json asset"))

        val manifestResponse = client.get(manifestAsset.browserDownloadUrl)
        if (!manifestResponse.status.isSuccess()) {
            return AppResult.Failure(AppError.Network.Server(manifestResponse.status.value))
        }
        AppResult.Success(json.decodeFromString<ReleaseManifest>(manifestResponse.bodyAsText()))
    } catch (e: Exception) {
        AppResult.Failure(e.toUpdateError())
    }

    companion object {
        fun defaultClient(): HttpClient = HttpClient(OkHttp) {
            install(HttpTimeout) {
                requestTimeoutMillis = 15_000
                connectTimeoutMillis = 10_000
            }
            expectSuccess = false
        }
    }
}

internal fun Exception.toUpdateError(): AppError = when {
    this is java.net.UnknownHostException -> AppError.Network.Offline
    this is java.net.SocketTimeoutException -> AppError.Network.Timeout
    else -> AppError.Unexpected(message ?: this::class.simpleName.orEmpty())
}
