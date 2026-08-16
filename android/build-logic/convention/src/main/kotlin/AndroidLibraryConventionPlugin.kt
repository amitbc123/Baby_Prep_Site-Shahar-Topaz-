import com.android.build.api.dsl.LibraryExtension
import com.oryareach.buildlogic.configureAndroidCommon
import com.oryareach.buildlogic.configureKotlinAndroid
import com.oryareach.buildlogic.intVersion
import com.oryareach.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.library")

        extensions.configure<LibraryExtension> {
            configureAndroidCommon(this@with)
            // AGP 9 dropped targetSdk from library modules; it is set by the consuming app.
            // consumerProguardFiles is left to individual modules: declaring it here would
            // force every module to carry a file whether or not it has rules to contribute.
            defaultConfig {
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            }
        }
        configureKotlinAndroid()

        dependencies {
            add("implementation", libs.findLibrary("kotlinx-coroutines-core").get())
            add("testImplementation", libs.findLibrary("junit").get())
            add("testImplementation", libs.findLibrary("kotest-assertions").get())
            add("testImplementation", libs.findLibrary("kotlinx-coroutines-test").get())
            add("testImplementation", libs.findLibrary("turbine").get())
        }
    }
}
