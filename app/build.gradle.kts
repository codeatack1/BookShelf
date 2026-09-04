plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val webDistDir = rootProject.file("web/dist")
val webAssetsDir = file("src/main/assets/web")

tasks.register("syncWebDist") {
    doLast {
        webAssetsDir.mkdirs()
        if (webDistDir.exists()) {
            webAssetsDir.deleteRecursively()
            webAssetsDir.mkdirs()
            webDistDir.copyRecursively(webAssetsDir)
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

    buildTypes {
        debug {
            applicationIdSuffix = ".dev"
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
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)

    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
}