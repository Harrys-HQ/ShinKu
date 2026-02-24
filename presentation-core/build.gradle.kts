plugins {
    id("shinku.library")
    id("shinku.library.compose")
    kotlin("android")
}

android {
    namespace = "com.shinku.reader.presentation.core"

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=androidx.compose.animation.ExperimentalAnimationApi",
            "-opt-in=androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.ui.ExperimentalComposeUiApi",
            "-opt-in=kotlinx.coroutines.FlowPreview",
        )
    }
}

dependencies {
    api(projects.core.common)
    api(projects.i18n)
    // SY -->
    api(projects.i18nSy)
    // SY <--

    // Compose
    implementation(libs.activity)
    implementation(libs.foundation)
    implementation(libs.material3.core)
    implementation(libs.material.icons)
    implementation(libs.animation)
    implementation(libs.animation.graphics)
    debugImplementation(libs.ui.tooling)
    implementation(libs.ui.tooling.preview)
    implementation(libs.ui.util)

    implementation(libs.paging.runtime)
    implementation(libs.paging.compose)
    implementation(libs.immutables)
}
