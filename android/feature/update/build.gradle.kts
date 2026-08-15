plugins {
    id("oryareach.android.feature")
}

android {
    namespace = "com.oryareach.feature.update"
}

dependencies {
    implementation(project(":core:update"))
}
