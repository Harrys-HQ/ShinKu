package com.shinku.reader.presentation.category

import android.content.Context
import androidx.compose.runtime.Composable
import com.shinku.reader.core.common.i18n.stringResource
import com.shinku.reader.domain.category.model.Category
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.core.i18n.stringResource

val Category.visualName: String
    @Composable
    get() = when {
        isSystemCategory -> stringResource(MR.strings.label_default)
        else -> name
    }

fun Category.visualName(context: Context): String =
    when {
        isSystemCategory -> context.stringResource(MR.strings.label_default)
        else -> name
    }
