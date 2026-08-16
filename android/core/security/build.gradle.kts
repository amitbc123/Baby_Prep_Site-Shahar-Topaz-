plugins {
    id("oryareach.android.library")
}

android {
    namespace = "com.oryareach.core.security"
}

dependencies {
    api(project(":core:crypto"))
    implementation(project(":core:common"))
    implementation(project(":core:database"))
    implementation(libs.androidx.core.ktx)
}
