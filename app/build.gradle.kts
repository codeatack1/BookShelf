plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val webDistDir = rootProject.file("web/dist")
val webAssetsDir = file("src/main/assets/web")

tasks.register("syncWebDist") {
    doLast {
        if (webDistDir.exists()) {
            webAssetsDir.deleteRecursively()
            webDistDir.copyRecursively(webAssetsDir)
            File(webAssetsDir, ".gitkeep").writeText("")
            logger.lifecycle("syncWebDist: copied web/dist -> ${webAssetsDir.path} (${webAssetsDir.listFiles()?.size ?: 0} items)")
        } else {
            logger.lifecycle("syncWebDist: ${webDistDir.path} does not exist, assets/web left untouched")
        }
    }
}

tasks.named("preBuild") {
    dependsOn("syncWebDist")
}

android {
    namespace = "com.bookshelf"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.bookshelf.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    signingConfigs {
        getByName("debug") {
            storeFile = rootProject.file("keystore/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".dev"
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/io.netty.versions.properties"
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    implementation("androidx.core:core-splashscreen:1.2.0")

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)

    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
}