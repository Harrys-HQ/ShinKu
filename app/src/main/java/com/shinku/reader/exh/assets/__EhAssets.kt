package com.shinku.reader.exh.assets

import androidx.compose.ui.graphics.vector.ImageVector
import com.shinku.reader.exh.assets.ehassets.AllAssets
import com.shinku.reader.exh.assets.ehassets.EhLogo
import com.shinku.reader.exh.assets.ehassets.Exh
import com.shinku.reader.exh.assets.ehassets.MangadexLogo
import kotlin.collections.List as ____KtList

public object EhAssets

@Suppress("ObjectPropertyName", "ktlint:standard:backing-property-naming")
private var __AllAssets: ____KtList<ImageVector>? = null

public val EhAssets.AllAssets: ____KtList<ImageVector>
    get() {
        if (__AllAssets != null) {
            return __AllAssets!!
        }
        __AllAssets = Exh.AllAssets + listOf(EhLogo, MangadexLogo)
        return __AllAssets!!
    }
