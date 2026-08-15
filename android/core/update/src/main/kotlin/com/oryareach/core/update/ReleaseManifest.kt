package com.oryareach.core.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Mirrors the `manifest.json` asset the release workflow publishes alongside each APK. */
@Serializable
data class ReleaseManifest(
    val version: String,
    val versionCode: Int,
    @SerialName("release_date") val releaseDate: String,
    val mandatory: Boolean = false,
    @SerialName("minSupportedVersion") val minSupportedVersion: String? = null,
    @SerialName("release_url") val releaseUrl: String,
    val notes: List<String> = emptyList(),
    val asset: ReleaseAsset,
)

@Serializable
data class ReleaseAsset(
    val url: String,
    val sha256: String,
    val sizeBytes: Long,
)
