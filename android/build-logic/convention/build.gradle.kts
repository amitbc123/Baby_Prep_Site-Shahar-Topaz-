plugins {
    `kotlin-dsl`
}

group = "com.oryareach.buildlogic"

kotlin {
    jvmToolchain(21)
}

dependencies {
    compileOnly(libs.plugin.agp)
    compileOnly(libs.plugin.agp.common)
    compileOnly(libs.plugin.kotlin)
    compileOnly(libs.plugin.ksp)
    compileOnly(libs.plugin.room)
    compileOnly(libs.plugin.compose)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "oryareach.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "oryareach.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "oryareach.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
        register("androidFeature") {
            id = "oryareach.android.feature"
            implementationClass = "AndroidFeatureConventionPlugin"
        }
        register("androidRoom") {
            id = "oryareach.android.room"
            implementationClass = "AndroidRoomConventionPlugin"
        }
        register("jvmLibrary") {
            id = "oryareach.jvm.library"
            implementationClass = "JvmLibraryConventionPlugin"
        }
    }
}
