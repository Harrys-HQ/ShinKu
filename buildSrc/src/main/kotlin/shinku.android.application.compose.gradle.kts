import shinku.buildlogic.configureCompose

plugins {
    id("com.android.application")
    kotlin("android")

    id("shinku.code.lint")
}

android {
    configureCompose(this)
}
