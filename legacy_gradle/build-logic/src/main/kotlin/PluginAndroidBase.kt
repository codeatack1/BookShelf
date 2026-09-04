import com.android.build.api.dsl.ApplicationDefaultConfig
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.CompileOptions
import com.android.build.api.dsl.DefaultConfig
import com.bookshelf.gradle.configurations.configureKotlin
import com.bookshelf.gradle.extensions.android
import com.bookshelf.gradle.extensions.configureTest
import com.bookshelf.gradle.extensions.coreLibraryDesugaring
import com.bookshelf.gradle.extensions.libs
import com.bookshelf.gradle.extensions.mihonx
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

@Suppress("UNUSED")
class PluginAndroidBase : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        configureKotlin()
        configureTest()

        android {
            defaultConfig {
                minSdk = mihonx.versions.android.sdk.min.get().toInt()
                if (this is ApplicationDefaultConfig) {
                    targetSdk = mihonx.versions.android.sdk.target.get().toInt()
                }

                ndkVersion = mihonx.versions.android.ndk.get()
            }

            compileSdk = mihonx.versions.android.sdk.compile.get().toInt()

            compileOptions {
                isCoreLibraryDesugaringEnabled = true
            }
        }

        dependencies {
            coreLibraryDesugaring(libs.android.desugar)
        }
    }
}

private fun CommonExtension.defaultConfig(block: DefaultConfig.() -> Unit) {
    defaultConfig.apply(block)
}

private fun CommonExtension.compileOptions(block: CompileOptions.() -> Unit) {
    compileOptions.apply(block)
}
