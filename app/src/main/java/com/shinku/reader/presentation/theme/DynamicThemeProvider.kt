package com.shinku.reader.presentation.theme

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.LruCache
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil3.asDrawable
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.bitmapConfig
import coil3.size.Size
import com.materialkolor.dynamicColorScheme
import com.shinku.reader.domain.manga.model.Manga
import com.shinku.reader.domain.ui.UiPreferences
import com.shinku.reader.exh.source.ShinKuPreferences
import com.shinku.reader.presentation.core.util.collectAsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

private val coverSeedColorCache = LruCache<Long, Int>(64)

/**
 * Crash-safe, off-main-thread cover seed color extraction.
 * Downscales bitmaps to 64x64 and safely copies HARDWARE bitmaps before running Palette.
 */
suspend fun extractSeedColorSafe(drawable: Drawable?): Int? = withContext(Dispatchers.Default) {
    runCatching {
        val bitmapDrawable = drawable as? BitmapDrawable ?: return@withContext null
        val sourceBitmap = bitmapDrawable.bitmap ?: return@withContext null
        if (sourceBitmap.isRecycled || sourceBitmap.width <= 0 || sourceBitmap.height <= 0) {
            return@withContext null
        }

        val softwareBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            sourceBitmap.config == Bitmap.Config.HARDWARE
        ) {
            sourceBitmap.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            sourceBitmap
        } ?: return@withContext null

        val scaledBitmap = if (softwareBitmap.width > 64 || softwareBitmap.height > 64) {
            Bitmap.createScaledBitmap(softwareBitmap, 64, 64, true)
        } else {
            softwareBitmap
        }

        val palette = Palette.from(scaledBitmap)
            .maximumColorCount(16)
            .generate()

        if (scaledBitmap !== softwareBitmap && scaledBitmap !== sourceBitmap && !scaledBitmap.isRecycled) {
            scaledBitmap.recycle()
        }
        if (softwareBitmap !== sourceBitmap && !softwareBitmap.isRecycled) {
            softwareBitmap.recycle()
        }

        val extracted = palette.getVibrantColor(
            palette.getMutedColor(
                palette.getDominantColor(0),
            ),
        )
        extracted.takeIf { it != 0 }
    }.getOrNull()
}

/**
 * Synchronizes the app-wide Dynamic Library Sanctuary Theme with the active/featured Library manga.
 */
@Composable
fun SyncLibraryThemeEffect(manga: Manga?) {
    val shinkuPreferences = remember { Injekt.get<ShinKuPreferences>() }
    val dynamicEnabled by shinkuPreferences.dynamicLibraryTheme().collectAsState()
    val context = LocalContext.current

    LaunchedEffect(manga?.id, manga?.coverLastModified, dynamicEnabled) {
        if (!dynamicEnabled || manga == null) return@LaunchedEffect

        val cached = coverSeedColorCache.get(manga.id)
        if (cached != null && cached != 0) {
            if (shinkuPreferences.dynamicLibrarySeedColor().get() != cached) {
                shinkuPreferences.dynamicLibrarySeedColor().set(cached)
            }
            return@LaunchedEffect
        }

        val extractedColor = withContext(Dispatchers.IO) {
            runCatching {
                val request = ImageRequest.Builder(context)
                    .data(manga)
                    .size(Size(128, 128))
                    .bitmapConfig(Bitmap.Config.ARGB_8888)
                    .build()
                val result = context.imageLoader.execute(request)
                if (result is SuccessResult) {
                    val drawable = result.image.asDrawable(context.resources)
                    extractSeedColorSafe(drawable)
                } else {
                    null
                }
            }.getOrNull()
        }

        if (extractedColor != null && extractedColor != 0) {
            coverSeedColorCache.put(manga.id, extractedColor)
            if (shinkuPreferences.dynamicLibrarySeedColor().get() != extractedColor) {
                shinkuPreferences.dynamicLibrarySeedColor().set(extractedColor)
            }
        }
    }
}

@Composable
fun DynamicThemeProvider(
    cover: Drawable?,
    content: @Composable () -> Unit,
) {
    val uiPreferences = remember { Injekt.get<UiPreferences>() }
    val isAmoled = uiPreferences.themeDarkAmoled().get()
    val darkTheme = isSystemInDarkTheme()
    var seedColor by remember { mutableStateOf<Color?>(null) }

    LaunchedEffect(cover) {
        val colorInt = extractSeedColorSafe(cover)
        if (colorInt != null) {
            seedColor = Color(colorInt)
        }
    }

    val overrideColorScheme = if (seedColor != null) {
        dynamicColorScheme(
            seedColor = seedColor!!,
            isDark = darkTheme,
            isAmoled = isAmoled,
        )
    } else {
        null
    }

    TachiyomiTheme(overrideColorScheme = overrideColorScheme) {
        content()
    }
}

/**
 * A simpler version that just extracts the color safely off the main thread.
 */
@Composable
fun rememberCoverColor(cover: Drawable?): Color? {
    var color by remember { mutableStateOf<Color?>(null) }
    LaunchedEffect(cover) {
        val extracted = extractSeedColorSafe(cover)
        if (extracted != null) {
            color = Color(extracted)
        }
    }
    return color
}
