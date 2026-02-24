package com.shinku.reader.util.system

import com.shinku.reader.BuildConfig
import com.shinku.reader.exh.syDebugVersion

val isDevFlavor: Boolean
    get() = BuildConfig.FLAVOR == "dev"

val isPreviewBuildType: Boolean
    get() = BuildConfig.BUILD_TYPE == "release" /* SY --> */ && syDebugVersion != "0" /* SY <-- */

val isReleaseBuildType: Boolean
    get() = BuildConfig.BUILD_TYPE == "release" /* SY --> */ && syDebugVersion == "0" /* SY <-- */
