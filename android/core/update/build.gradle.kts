plugins {
    id("oryareach.android.library")
    alias(libs.plugins.kotlin.serialization)
}

/**
 * Not secrets — the repo is public and these just name where to look for releases. Still
 * overridable via a Gradle property (`-PgithubOwner=...`) rather than hardcoded at call
 * sites, so a fork doesn't have to edit source to point at its own releases.
 */
fun repoSetting(name: String, default: String): String =
    providers.gradleProperty(name).getOrElse(default)

android {
    namespace = "com.oryareach.core.update"

    defaultConfig {
        buildConfigField("String", "GITHUB_OWNER", "\"${repoSetting("githubOwner", "shaharco99")}\"")
        buildConfigField("String", "GITHUB_REPOSITORY", "\"${repoSetting("githubRepository", "Baby_app")}\"")
        buildConfigField("String", "UPDATE_CHANNEL", "\"${repoSetting("updateChannel", "stable")}\"")
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core:common"))

    implementation(libs.ktor.client.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.datastore.preferences)
}
