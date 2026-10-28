package com.shinku.reader.presentation.more.stats.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.core.content.FileProvider
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
        primaryColor = Color(0xFF7C4DFF),
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
    readingPersona: String,
    level: Int,
    totalXp: Long,
    levelProgress: Float,
    readDurationText: String,
    chaptersRead: Int,
    streakDays: Int,
    unlockedBadgesCount: Int,
    topGenres: List<String>,
    primaryColor: Int,
    secondaryColor: Int,
    surfaceColor: Int,
    onSurfaceColor: Int,
): Uri? = withContext(Dispatchers.IO) {
    try {
        val width = 960
        val height = 560
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, width.toFloat(), height.toFloat(), secondaryColor, primaryColor, Shader.TileMode.CLAMP)
        }
        canvas.drawRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), 48f, 48f, bgPaint)

        val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 16f, 0f, height - 16f, secondaryColor, surfaceColor, Shader.TileMode.CLAMP)
            alpha = 242
        }
        canvas.drawRoundRect(RectF(12f, 12f, width - 12f, height - 12f), 40f, 40f, innerPaint)

        val avatarRect = RectF(56f, 56f, 216f, 216f)
        val avatarBitmap = if (avatarUriString.isNotBlank()) {
            context.contentResolver.openInputStream(Uri.parse(avatarUriString))?.use { BitmapFactory.decodeStream(it) }
        } else null

        if (avatarBitmap != null) {
            val saveCount = canvas.save()
            val clipPath = Path().apply { addRoundRect(avatarRect, 32f, 32f, Path.Direction.CW) }
            canvas.clipPath(clipPath)
            canvas.drawBitmap(avatarBitmap, null, avatarRect, Paint(Paint.FILTER_BITMAP_FLAG))
            canvas.restoreToCount(saveCount)
        } else {
            val crestPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = primaryColor }
            canvas.drawRoundRect(avatarRect, 32f, 32f, crestPaint)
        }

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = onSurfaceColor
            textSize = 46f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryColor
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        canvas.drawText("SHINKU CHRONICLES • ${readingPersona.uppercase()}", 248f, 92f, subPaint.apply { textSize = 21f })
        canvas.drawText(nickname, 248f, 148f, titlePaint)
        canvas.drawText("Lv.$level • $activeTitle ($totalXp XP)", 248f, 196f, subPaint.apply { textSize = 28f })

        val trackRect = RectF(56f, 256f, width - 56f, 276f)
        val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryColor
            alpha = 55
        }
        canvas.drawRoundRect(trackRect, 10f, 10f, barPaint)
        barPaint.alpha = 255
        val fillWidth = 56f + (width - 112f) * levelProgress.coerceIn(0.05f, 1f)
        canvas.drawRoundRect(RectF(56f, 256f, fillWidth, 276f), 10f, 10f, barPaint)

        val metrics = listOf(
            "TIME READ" to readDurationText,
            "CHAPTERS" to chaptersRead.toString(),
            "STREAK" to "${streakDays}d",
            "BADGES" to unlockedBadgesCount.toString(),
        )
        val colWidth = (width - 112f) / 4f
        metrics.forEachIndexed { idx, (label, value) ->
            val x = 56f + idx * colWidth
            canvas.drawText(value, x, 368f, titlePaint.apply { textSize = 40f })
            canvas.drawText(label, x, 406f, subPaint.apply { textSize = 20f })
        }

        if (topGenres.isNotEmpty()) {
            canvas.drawText(
                "Top Affinities: ${topGenres.joinToString(" • ")}",
                56f,
                488f,
                subPaint.apply { textSize = 24f },
            )
        }

        val outFile = File(context.cacheDir, "sanctuary_chronicles_card.png")
        FileOutputStream(outFile).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
        FileProvider.getUriForFile(context, "${context.packageName}.provider", outFile)
    } catch (_: Exception) {
        null
    }
}
