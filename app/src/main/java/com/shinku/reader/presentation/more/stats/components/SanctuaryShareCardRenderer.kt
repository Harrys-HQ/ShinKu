package com.shinku.reader.presentation.more.stats.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.ui.graphics.Color
import androidx.core.content.FileProvider
import com.shinku.reader.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

internal enum class ShareCardTheme(
    val label: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val surfaceColor: Color,
    val onSurfaceColor: Color,
) {
    CRIMSON(
        label = "Crimson",
        primaryColor = Color(0xFFE53935),
        secondaryColor = Color(0xFF4A0E12),
        surfaceColor = Color(0xFF140B0D),
        onSurfaceColor = Color(0xFFF9ECEE),
    ),
    OBSIDIAN(
        label = "Obsidian",
        primaryColor = Color(0xFFB8A0FF),
        secondaryColor = Color(0xFF1F1638),
        surfaceColor = Color(0xFF0B0C12),
        onSurfaceColor = Color(0xFFECEFF4),
    ),
    ROYAL_GOLD(
        label = "Royal Gold",
        primaryColor = Color(0xFFFFB300),
        secondaryColor = Color(0xFF3E2C14),
        surfaceColor = Color(0xFF14110B),
        onSurfaceColor = Color(0xFFFFF8E1),
    ),
}

internal suspend fun renderAndSaveShareCard(
    context: Context,
    avatarUriString: String,
    nickname: String,
    activeTitle: String,
    content: ShareCardContent,
    primaryColor: Int,
    secondaryColor: Int,
    surfaceColor: Int,
    onSurfaceColor: Int,
): Uri? = withContext(Dispatchers.IO) {
    var bitmap: Bitmap? = null
    try {
        val width = 960
        val margin = 56f
        val bodyWidth = width - 112
        data class TextBlock(val layout: StaticLayout, val x: Float, val y: Float)
        val blocks = mutableListOf<TextBlock>()
        fun text(value: String, x: Float, y: Float, availableWidth: Int, size: Float, color: Int, bold: Boolean = false): Float {
            if (value.isBlank()) return y
            val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                textSize = size
                typeface = Typeface.create(Typeface.DEFAULT, if (bold) Typeface.BOLD else Typeface.NORMAL)
            }
            val layout = StaticLayout.Builder.obtain(value, 0, value.length, paint, availableWidth)
                .setIncludePad(false).setBreakStrategy(Layout.BREAK_STRATEGY_HIGH_QUALITY).build()
            blocks += TextBlock(layout, x, y)
            return y + layout.height
        }

        var headerY = text("ShinKu · ${context.getString(R.string.journey_share_title)}", 248f, 56f, 656, 22f, primaryColor, true) + 16f
        headerY = text(nickname, 248f, headerY, 656, 46f, onSurfaceColor, true) + 12f
        headerY = text(activeTitle, 248f, headerY, 656, 28f, primaryColor)
        var y = maxOf(216f, headerY) + 32f
        content.persona?.let { y = text(it, margin, y, bodyWidth, 28f, onSurfaceColor) + 16f }
        content.levelSummary?.let { y = text(it, margin, y, bodyWidth, 28f, primaryColor, true) + 16f }
        val progressY = y.takeIf { content.levelProgress != null }
        if (progressY != null) y += 44f
        val columnWidth = (bodyWidth - 24) / 2
        content.metrics.chunked(2).forEach { row ->
            val rowTop = y
            y = row.mapIndexed { index, metric ->
                val x = margin + index * (columnWidth + 24)
                val valueY = text(metric.label, x, rowTop, columnWidth, 24f, onSurfaceColor) + 8f
                text(metric.value, x, valueY, columnWidth, 38f, primaryColor, true)
            }.max() + 28f
        }
        if (content.genres.isNotEmpty()) {
            y = text(content.genres.joinToString(" • "), margin, y, bodyWidth, 28f, primaryColor) + 16f
        }
        val height = (y + margin).toInt()
        val rendered = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap = rendered
        val canvas = Canvas(rendered)
        val background = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, width.toFloat(), height.toFloat(), secondaryColor, primaryColor, Shader.TileMode.CLAMP)
        }
        canvas.drawRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), 48f, 48f, background)
        val inner = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 16f, 0f, height - 16f, secondaryColor, surfaceColor, Shader.TileMode.CLAMP)
        }
        canvas.drawRoundRect(RectF(12f, 12f, width - 12f, height - 12f), 40f, 40f, inner)

        val avatarRect = RectF(56f, 56f, 216f, 216f)
        val avatar = runCatching {
            if (avatarUriString.isBlank()) {
                null
            } else {
                val uri = Uri.parse(avatarUriString)
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                options.inJustDecodeBounds = false
                options.inSampleSize = 1
                while (minOf(options.outWidth, options.outHeight) / options.inSampleSize > 320) options.inSampleSize *= 2
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            }
        }.getOrNull()
        if (avatar != null) {
            val save = canvas.save()
            canvas.clipPath(Path().apply { addRoundRect(avatarRect, 32f, 32f, Path.Direction.CW) })
            val side = minOf(avatar.width, avatar.height)
            val left = (avatar.width - side) / 2
            val top = (avatar.height - side) / 2
            canvas.drawBitmap(avatar, Rect(left, top, left + side, top + side), avatarRect, Paint(Paint.FILTER_BITMAP_FLAG))
            canvas.restoreToCount(save)
            avatar.recycle()
        } else {
            canvas.drawRoundRect(
                avatarRect, 32f, 32f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = primaryColor
                    alpha = 51
                },
            )
            context.getDrawable(R.drawable.ic_shinku_logo)?.mutate()?.apply {
                setBounds(88, 88, 184, 184)
                draw(canvas)
            }
        }
        blocks.forEach { block ->
            val save = canvas.save()
            canvas.translate(block.x, block.y)
            block.layout.draw(canvas)
            canvas.restoreToCount(save)
        }
        progressY?.let { top ->
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = primaryColor
                alpha = 55
            }
            canvas.drawRoundRect(RectF(margin, top, width - margin, top + 16f), 8f, 8f, paint)
            val progress = content.levelProgress!!.coerceIn(0f, 1f)
            if (progress > 0f) {
                paint.alpha = 255
                canvas.drawRoundRect(RectF(margin, top, margin + bodyWidth * progress, top + 16f), 8f, 8f, paint)
            }
        }
        val outFile = File.createTempFile("sanctuary-journey-", ".png", context.cacheDir)
        FileOutputStream(outFile).use { out -> check(rendered.compress(Bitmap.CompressFormat.PNG, 100, out)) }
        FileProvider.getUriForFile(context, "${context.packageName}.provider", outFile)
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    } finally {
        bitmap?.recycle()
    }
}
