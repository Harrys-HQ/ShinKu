plugins {
    id("shinku.library")
    kotlin("android")
    kotlin("plugin.serialization")
}

android {
    namespace = "com.shinku.reader.domain"

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi")
    }
}

dependencies {
    implementation(projects.sourceApi)
    implementation(projects.core.common)

    implementation(platform(libs.coroutines.bom))
    implementation(libs.bundles.coroutines)
    implementation(libs.bundles.serialization)

    implementation(libs.unifile)

    api(libs.sqldelight.android.paging)

    compileOnly(libs.runtime.annotation)

    // SY -->
    implementation(libs.injekt)
    // SY <--

    testImplementation(libs.bundles.test)
    testImplementation(libs.coroutines.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}
