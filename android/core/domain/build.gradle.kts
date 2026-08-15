plugins {
    id("oryareach.jvm.library")
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.kotlinx.datetime)
}
