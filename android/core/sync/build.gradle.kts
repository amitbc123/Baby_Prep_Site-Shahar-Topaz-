plugins {
    id("oryareach.jvm.library")
}

dependencies {
    api(project(":core:model"))
    api(project(":core:common"))
    implementation(project(":core:crypto"))
}
