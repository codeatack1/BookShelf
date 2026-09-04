import com.bookshelf.gradle.extensions.alias
import com.bookshelf.gradle.extensions.libs
import com.bookshelf.gradle.extensions.mihonx
import com.bookshelf.gradle.extensions.plugins
import org.gradle.api.Plugin
import org.gradle.api.Project

@Suppress("UNUSED")
class PluginAndroidApplication : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        plugins {
            alias(libs.plugins.android.application)
            alias(mihonx.plugins.android.base)
        }
    }
}
