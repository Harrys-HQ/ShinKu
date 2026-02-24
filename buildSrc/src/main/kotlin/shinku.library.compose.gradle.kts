import shinku.buildlogic.configureCompose

plugins {
    id("com.android.library")

    id("shinku.code.lint")
}

android {
    configureCompose(this)
}
