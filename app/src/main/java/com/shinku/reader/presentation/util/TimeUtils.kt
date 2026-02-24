package com.shinku.reader.presentation.util

import android.content.Context
import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.shinku.reader.core.common.i18n.stringResource
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.core.i18n.stringResource
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

fun Duration.toDurationString(context: Context, fallback: String): String {
    val totalMinutes = inWholeMinutes
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60

    return buildList(2) {
        if (hours != 0L) add(context.stringResource(MR.strings.hour_short, hours))
        if (minutes != 0L || hours == 0L) add(context.stringResource(MR.strings.minute_short, minutes))
    }.joinToString(" ").ifBlank { fallback }
}

@Composable
@ReadOnlyComposable
fun relativeTimeSpanString(epochMillis: Long): String {
    val now = Instant.now().toEpochMilli()
    return when {
        epochMillis <= 0L -> stringResource(MR.strings.relative_time_span_never)
        now - epochMillis < 1.minutes.inWholeMilliseconds -> stringResource(
            MR.strings.updates_last_update_info_just_now,
        )
        else -> DateUtils.getRelativeTimeSpanString(epochMillis, now, DateUtils.MINUTE_IN_MILLIS).toString()
    }
}
