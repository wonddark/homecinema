import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
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
        val desktopMain by getting {
            dependencies {
                implementation(project(":shared"))
                implementation(compose.desktop.currentOs)
                implementation(compose.material3)
                implementation(libs.kotlinx.coroutines.core)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.homecinema.desktop.MainKt"

        // No hard-coded javaHome: run/package with the JDK Gradle itself runs on (JAVA_HOME, JDK 21).

        nativeDistributions {
            targetFormats(TargetFormat.Rpm)
            packageName = "homecinema"
            packageVersion = "1.0.0"
            description = "GrabThatVideo – multiplatform video downloader"
            vendor = "GrabThatVideo"

            linux {
                packageName = "homecinema"
                menuGroup = "Multimedia"
                appCategory = "Video"
            }
        }
    }
}
