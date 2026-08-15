plugins {
    id("oryareach.android.feature")
}

android {
    namespace = "com.oryareach.feature.tasks"
}

dependencies {
    implementation(project(":core:database"))
    implementation(project(":core:network"))
    implementation(project(":core:sync"))
    implementation(libs.compose.material.icons.extended)
}
