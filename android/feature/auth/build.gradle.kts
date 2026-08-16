plugins {
    id("oryareach.android.feature")
}

android {
    namespace = "com.oryareach.feature.auth"
}

dependencies {
    implementation(project(":core:network"))
    implementation(project(":core:crypto"))
    implementation(project(":core:security"))
}
