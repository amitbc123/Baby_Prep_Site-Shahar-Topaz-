package com.oryareach.core.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The subset of GitHub's release response this module reads. */
@Serializable
internal data class GithubReleaseDto(
    @SerialName("tag_name") val tagName: String,
    val assets: List<GithubReleaseAssetDto> = emptyList(),
)

@Serializable
internal data class GithubReleaseAssetDto(
    val name: String,
    @SerialName("browser_download_url") val browserDownloadUrl: String,
)
