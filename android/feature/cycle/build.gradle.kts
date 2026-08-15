plugins {
    id("oryareach.android.feature")
}

android {
    namespace = "com.oryareach.feature.cycle"
}

dependencies {
    implementation(project(":core:database"))
    implementation(project(":core:network"))
    implementation(project(":core:sync"))
    implementation(project(":core:domain"))
    implementation(project(":core:scanner"))
    implementation(libs.compose.material.icons.extended)
}
