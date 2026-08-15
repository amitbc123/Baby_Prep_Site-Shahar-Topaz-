plugins {
    id("oryareach.jvm.library")
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:common"))
    implementation(libs.bouncycastle)
}
