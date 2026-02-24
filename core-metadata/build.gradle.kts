plugins {
    id("shinku.library")
    kotlin("android")
    kotlin("plugin.serialization")
}

android {
    namespace = "com.shinku.reader.core.metadata"

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }
}

dependencies {
    implementation(projects.sourceApi)

    implementation(libs.bundles.serialization)
}
