import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    androidTarget {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_11)
                }
            }
        }
    }

    jvm("desktop") {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_21)
                }
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(compose.components.resources)
            // api: KtorThumbnailSaver's public constructor takes an HttpClient.
            api(libs.ktor.client.core)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }

        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.youtubedl.android)
            implementation(libs.youtubedl.android.ffmpeg)
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.okhttp)
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(compose.components.uiToolingPreview)
                implementation(libs.ktor.client.java)
            }
        }
    }
}

// Opt-in desktopTest checks (each is skipped unless its property is passed):
// -PhistoryRenderDir=<dir> / -PhomeRenderDir=<dir> / -PthemeRenderDir=<dir> write render PNGs;
// -Pe2e=true runs DesktopE2ETest against the real yt-dlp, ffmpeg and network (-Pe2eDir=<dir> picks its folder).
tasks.withType<Test>().configureEach {
    listOf("historyRenderDir", "homeRenderDir", "themeRenderDir", "renderLocale", "e2e", "e2eDir").forEach { name ->
        providers.gradleProperty(name).orNull?.let { systemProperty(name, it) }
    }
}

android {
    namespace = "com.homecinema.shared"
    compileSdk =
        libs.versions.android.compileSdk
            .get()
            .toInt()

    defaultConfig {
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
