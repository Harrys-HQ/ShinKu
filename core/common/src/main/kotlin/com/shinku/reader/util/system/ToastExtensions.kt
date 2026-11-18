package com.shinku.reader.util.system

import android.content.Context
import android.widget.Toast
import dev.icerock.moko.resources.StringResource
import com.shinku.reader.core.common.i18n.stringResource

/**
 * Display a toast in this context.
 *
 * @param resource the text resource.
 * @param duration the duration of the toast. Defaults to short.
 */
fun Context.toast(
    resource: StringResource,
    duration: Int = Toast.LENGTH_SHORT,
    block: (Toast) -> Unit = {},
): Toast {
    return toast(stringResource(resource), duration, block)
}

/**
 * Display a toast in this context, formatting the resource with arguments.
 *
 * Needed by messages that quote a number the code owns: without it every translation has to
 * hard-code the limit, and 15 locales were advertising 10 sources while the code enforces 25.
 *
 * The arguments come as a [List] and NOT as `vararg args: Any` on purpose. Two existing call sites
 * pass a duration positionally - `context.toast(res, Toast.LENGTH_LONG)` in MigrationListScreen and
 * in SettingsEhScreen - and a vararg `Any` overload would make those calls ambiguous, because an
 * Int matches both `duration: Int` and `vararg args: Any`. A List cannot be mistaken for an Int, so
 * every existing call site keeps resolving to the overload it resolves to today.
 *
 * @param resource the text resource.
 * @param formatArgs the arguments for the resource's format string.
 * @param duration the duration of the toast. Defaults to short.
 */
fun Context.toast(
    resource: StringResource,
    formatArgs: List<Any>,
    duration: Int = Toast.LENGTH_SHORT,
    block: (Toast) -> Unit = {},
): Toast {
    return toast(stringResource(resource, *formatArgs.toTypedArray()), duration, block)
}

/**
 * Display a toast in this context.
 *
 * @param text the text to display.
 * @param duration the duration of the toast. Defaults to short.
 */
fun Context.toast(
    text: String?,
    duration: Int = Toast.LENGTH_SHORT,
    block: (Toast) -> Unit = {},
): Toast {
    return Toast.makeText(applicationContext, text.orEmpty(), duration).also {
        block(it)
        it.show()
    }
}
