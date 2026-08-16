plugins {
    id("oryareach.android.feature")
}

android {
    namespace = "com.oryareach.feature.search"
}

dependencies {
    implementation(project(":core:database"))
    implementation(libs.compose.material.icons.extended)
}
