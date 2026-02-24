import shinku.buildlogic.configureAndroid
import shinku.buildlogic.configureTest

plugins {
    id("com.android.test")
    kotlin("android")

    id("shinku.code.lint")
}

android {
    configureAndroid(this)
    configureTest()
}
