plugins {
    id("oryareach.android.feature")
}

android {
    namespace = "com.oryareach.feature.pairing"
}

dependencies {
    implementation(project(":core:network"))
    implementation(project(":core:crypto"))
    implementation(project(":core:security"))
}
