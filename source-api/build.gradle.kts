import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    id("shinku.library")
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.github.ben-manes.versions")
}

kotlin {
    androidTarget()
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(libs.serialization.json)
                api(libs.injekt)
                api(libs.rxjava)
                api(libs.jsoup)

                implementation(project.dependencies.platform(libs.bom))
                implementation(libs.runtime)

                // SY -->
                api(projects.i18n)
                api(projects.i18nSy)
                api(libs.reflect)
                // SY <--
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(projects.core.common)
                api(libs.preferencektx)

                // Workaround for https://youtrack.jetbrains.com/issue/KT-57605
                implementation(libs.coroutines.android)
                implementation(project.dependencies.platform(libs.coroutines.bom))
            }
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}

android {
    namespace = "eu.kanade.tachiyomi.source"

    defaultConfig {
        consumerProguardFile("consumer-proguard.pro")
    }
}
