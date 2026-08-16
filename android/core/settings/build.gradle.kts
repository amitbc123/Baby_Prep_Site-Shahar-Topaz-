plugins {
    id("oryareach.android.library")
}

android {
    namespace = "com.oryareach.core.settings"
}

dependencies {
    implementation(libs.androidx.datastore.preferences)
}
