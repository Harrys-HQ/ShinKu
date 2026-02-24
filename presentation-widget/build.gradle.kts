plugins {
    id("shinku.library")
    id("shinku.library.compose")
    kotlin("android")
}

android {
    namespace = "com.shinku.reader.presentation.widget"

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.domain)
    implementation(projects.presentationCore)
    api(projects.i18n)

    implementation(libs.glance)
    implementation(libs.material)

    implementation(libs.immutables)

    implementation(platform(libs.coil.bom))
    implementation(libs.coil.core)

    // SY -->
    implementation(libs.material)
    // SY <--

    api(libs.injekt)
}
