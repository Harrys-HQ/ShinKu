import shinku.buildlogic.AndroidConfig
import shinku.buildlogic.configureAndroid
import shinku.buildlogic.configureTest

plugins {
    id("com.android.application")
    kotlin("android")

    id("shinku.code.lint")
}

android {
    defaultConfig {
        targetSdk = AndroidConfig.TARGET_SDK
    }
    configureAndroid(this)
    configureTest()
}
