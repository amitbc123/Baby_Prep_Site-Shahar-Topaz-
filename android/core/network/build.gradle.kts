plugins {
    id("oryareach.android.library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.oryareach.core.network"

    defaultConfig {
        buildConfigField("String", "SUPABASE_URL", "\"${providers.gradleProperty("supabaseUrl").getOrElse("")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${providers.gradleProperty("supabaseAnonKey").getOrElse("")}\"")
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    api(project(":core:sync"))
    implementation(project(":core:common"))

    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.auth)
    implementation(libs.supabase.postgrest)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.koin.android)
}
