import com.android.build.api.dsl.ApplicationExtension
import com.oryareach.buildlogic.configureAndroidCommon
import com.oryareach.buildlogic.configureKotlinAndroid
import com.oryareach.buildlogic.gitVersionName
import com.oryareach.buildlogic.intVersion
import com.oryareach.buildlogic.versionNameToCode
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.application")

        val appVersion = gitVersionName().get()

        extensions.configure<ApplicationExtension> {
            configureAndroidCommon(this@with)
            defaultConfig {
                targetSdk = intVersion("targetSdk")
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                versionName = appVersion
                versionCode = versionNameToCode(appVersion)
            }
            buildFeatures.buildConfig = true
        }
        configureKotlinAndroid()
    }
}
