package com.oryareach.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

const val JVM_TOOLCHAIN = 21

val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

fun Project.version(alias: String): String = libs.findVersion(alias).get().requiredVersion

fun Project.intVersion(alias: String): Int = version(alias).toInt()

fun CommonExtension.configureAndroidCommon(project: Project) {
    compileSdk = project.intVersion("compileSdk")
    defaultConfig.minSdk = project.intVersion("minSdk")

    lint.apply {
        abortOnError = true
        // Dependency freshness is a reviewed decision, not something that should fail a build.
        disable += setOf("GradleDependency", "AndroidGradlePluginVersion", "NewerVersionAvailable")
    }
}

fun Project.configureKotlinAndroid() {
    extensions.getByType<KotlinAndroidProjectExtension>().apply {
        jvmToolchain(JVM_TOOLCHAIN)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
            allWarningsAsErrors.set(true)
            freeCompilerArgs.addAll("-Xconsistent-data-class-copy-visibility")
        }
    }
}

fun Project.configureKotlinJvm() {
    extensions.getByType<JavaPluginExtension>().apply {
        toolchain.languageVersion.set(JavaLanguageVersion.of(JVM_TOOLCHAIN))
    }
}
