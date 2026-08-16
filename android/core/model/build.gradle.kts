plugins {
    id("oryareach.jvm.library")
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(libs.kotlinx.datetime)
    api(libs.kotlinx.serialization.json)
}
