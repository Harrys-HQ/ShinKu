package com.shinku.reader.presentation.reader.dialog

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CropFree
import androidx.compose.material.icons.outlined.FindInPage
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shinku.reader.presentation.core.components.SanctuaryOverlineBadge
import com.shinku.reader.presentation.core.theme.SanctuaryElevation
import com.shinku.reader.presentation.core.theme.SanctuaryShapes

private data class ImageStrip(val bitmap: ImageBitmap, val srcTop: Int, val height: Int)

@Composable
fun AreaOcrCropOverlay(
    bitmap: Bitmap,
    pageNumber: Int,
    onDismissRequest: () -> Unit,
    onConfirmOverlay: (cropRectOnBitmap: android.graphics.Rect) -> Unit,
    onConfirmInspect: ((cropRectOnBitmap: android.graphics.Rect) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val cornerColor = MaterialTheme.colorScheme.tertiary

    // Slice tall bitmaps into strips of <= 2048px to prevent GPU max texture clipping
    val strips = remember(bitmap) {
        val maxStripHeight = 2048
        if (bitmap.height <= maxStripHeight) {
            listOf(ImageStrip(bitmap.asImageBitmap(), 0, bitmap.height))
        } else {
            val list = mutableListOf<ImageStrip>()
            for (y in 0 until bitmap.height step maxStripHeight) {
                val h = minOf(maxStripHeight, bitmap.height - y)
                val slice = Bitmap.createBitmap(bitmap, 0, y, bitmap.width, h)
                list.add(ImageStrip(slice.asImageBitmap(), y, h))
            }
            list
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        val density = LocalDensity.current
        val pad24Px = with(density) { 24.dp.toPx() }
        val pad160Px = with(density) { 160.dp.toPx() }
        val topMargin = with(density) { 72.dp.toPx() }
        val bottomMargin = with(density) { 88.dp.toPx() }

        val containerWidth = constraints.maxWidth.toFloat()
        val containerHeight = constraints.maxHeight.toFloat()
        val bmpWidth = bitmap.width.toFloat()
        val bmpHeight = bitmap.height.toFloat()
        val isTall = (bmpHeight / bmpWidth) > 1.35f

        // Fit width for tall webtoon pages; fit container for normal manga pages
        val scale = if (isTall) {
            (containerWidth - pad24Px).coerceAtLeast(100f) / bmpWidth
        } else {
            minOf(
                (containerWidth - pad24Px).coerceAtLeast(100f) / bmpWidth,
                (containerHeight - pad160Px).coerceAtLeast(100f) / bmpHeight,
            )
        }

        val dstWidth = bmpWidth * scale
        val dstHeight = bmpHeight * scale
        val dstLeft = (containerWidth - dstWidth) / 2f
        val visibleHeight = containerHeight - topMargin - bottomMargin

        var scrollY by remember(bitmap) {
            mutableFloatStateOf(if (isTall) 0f else (visibleHeight - dstHeight).coerceAtLeast(0f) / 2f)
        }

        val maxScrollY = topMargin
        val minScrollY = if (dstHeight > visibleHeight) topMargin + visibleHeight - dstHeight else topMargin

        // Normalized crop box relative to full bitmap (0..1)
        var cropNorm by remember(bitmap) {
            val initialH = if (isTall) (visibleHeight * 0.22f / dstHeight).coerceIn(0.04f, 0.25f) else 0.25f
            val initialW = 0.60f
            mutableStateOf(Rect(0.20f, 0.20f, 0.20f + initialW, 0.20f + initialH))
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(dstLeft, dstWidth, dstHeight, scrollY) {
                    var dragMode = 0 // 0: scroll page, 1: move box, 2: resize handle
                    var startOffset = Offset.Zero
                    var startCropNorm = cropNorm

                    detectDragGestures(
                        onDragStart = { offset ->
                            startOffset = offset
                            startCropNorm = cropNorm

                            val sLeft = dstLeft + cropNorm.left * dstWidth
                            val sTop = scrollY + cropNorm.top * dstHeight
                            val sRight = dstLeft + cropNorm.right * dstWidth
                            val sBottom = scrollY + cropNorm.bottom * dstHeight

                            val handleTol = 44.dp.toPx()
                            val nearBR = (offset.x - sRight).let { it * it } + (offset.y - sBottom).let { it * it } < handleTol * handleTol
                            val inside = offset.x in sLeft..sRight && offset.y in sTop..sBottom

                            dragMode = when {
                                nearBR -> 2
                                inside -> 1
                                else -> 0
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            when (dragMode) {
                                1 -> {
                                    // Move crop box
                                    val dNormX = dragAmount.x / dstWidth
                                    val dNormY = dragAmount.y / dstHeight
                                    val w = cropNorm.width
                                    val h = cropNorm.height
                                    val nLeft = (cropNorm.left + dNormX).coerceIn(0f, 1f - w)
                                    val nTop = (cropNorm.top + dNormY).coerceIn(0f, 1f - h)
                                    cropNorm = Rect(nLeft, nTop, nLeft + w, nTop + h)
                                }
                                2 -> {
                                    // Resize handle (bottom right)
                                    val currentR = dstLeft + cropNorm.right * dstWidth + dragAmount.x
                                    val currentB = scrollY + cropNorm.bottom * dstHeight + dragAmount.y
                                    val nR = ((currentR - dstLeft) / dstWidth).coerceIn(cropNorm.left + 0.05f, 1f)
                                    val nB = ((currentB - scrollY) / dstHeight).coerceIn(cropNorm.top + 0.02f, 1f)
                                    cropNorm = Rect(cropNorm.left, cropNorm.top, nR, nB)
                                }
                                else -> {
                                    // Scroll tall page vertically
                                    if (dstHeight > visibleHeight) {
                                        scrollY = (scrollY + dragAmount.y).coerceIn(minScrollY, maxScrollY)
                                    }
                                }
                            }
                        },
                    )
                },
        ) {
            // 1. Draw sliced bitmap strips (GPU-safe, never truncated at 4096px)
            for (strip in strips) {
                val stripDstTop = scrollY + (strip.srcTop * scale)
                val stripDstHeight = strip.height * scale
                if (stripDstTop + stripDstHeight >= 0 && stripDstTop <= containerHeight) {
                    drawImage(
                        image = strip.bitmap,
                        dstOffset = IntOffset(dstLeft.toInt(), stripDstTop.toInt()),
                        dstSize = IntSize(dstWidth.toInt(), stripDstHeight.toInt()),
                    )
                }
            }

            // 2. Compute on-screen crop box coordinates
            val cropScreenLeft = dstLeft + cropNorm.left * dstWidth
            val cropScreenTop = scrollY + cropNorm.top * dstHeight
            val cropScreenWidth = cropNorm.width * dstWidth
            val cropScreenHeight = cropNorm.height * dstHeight

            // 3. Draw dimmed scrim outside selection box
            val cropRectPath = Path().apply {
                addRect(Rect(cropScreenLeft, cropScreenTop, cropScreenLeft + cropScreenWidth, cropScreenTop + cropScreenHeight))
            }
            clipPath(cropRectPath, clipOp = ClipOp.Difference) {
                drawRect(Color.Black.copy(alpha = 0.65f))
            }

            // 4. Draw bounding box border
            drawRect(
                color = primaryColor,
                topLeft = Offset(cropScreenLeft, cropScreenTop),
                size = Size(cropScreenWidth, cropScreenHeight),
                style = Stroke(width = 2.dp.toPx()),
            )

            // 5. Draw corner indicators for resize grab
            val cLen = 18.dp.toPx()
            val cStroke = 4.dp.toPx()
            val r = cropScreenLeft + cropScreenWidth
            val b = cropScreenTop + cropScreenHeight

            drawLine(cornerColor, Offset(r - cLen, b), Offset(r, b), strokeWidth = cStroke)
            drawLine(cornerColor, Offset(r, b - cLen), Offset(r, b), strokeWidth = cStroke)
            drawLine(primaryColor, Offset(cropScreenLeft, cropScreenTop), Offset(cropScreenLeft + cLen, cropScreenTop), strokeWidth = cStroke)
            drawLine(primaryColor, Offset(cropScreenLeft, cropScreenTop), Offset(cropScreenLeft, cropScreenTop + cLen), strokeWidth = cStroke)
        }

        // Top bar
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
            shape = SanctuaryShapes.card,
            tonalElevation = SanctuaryElevation.card,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CropFree,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Column {
                        SanctuaryOverlineBadge(
                            text = "SHINKU AI • PAGE $pageNumber",
                            compact = true,
                        )
                        Text(
                            text = if (isTall) "Select Area (Drag to Scroll)" else "Select Bubble or Panel Area",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                IconButton(onClick = onDismissRequest) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close",
                    )
                }
            }
        }

        // Bottom floating action bar
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            shape = SanctuaryShapes.hero,
            tonalElevation = SanctuaryElevation.hero,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                FilledTonalButton(
                    onClick = {
                        val initialH = if (isTall) (visibleHeight * 0.22f / dstHeight).coerceIn(0.04f, 0.25f) else 0.25f
                        cropNorm = Rect(0.20f, 0.20f, 0.80f, 0.20f + initialH)
                    },
                    shape = SanctuaryShapes.pill,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Reset", fontSize = 12.sp)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (onConfirmInspect != null) {
                        FilledTonalButton(
                            onClick = {
                                val sLeft = (cropNorm.left * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
                                val sTop = (cropNorm.top * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
                                val sRight = (cropNorm.right * bitmap.width).toInt().coerceIn(sLeft + 1, bitmap.width)
                                val sBottom = (cropNorm.bottom * bitmap.height).toInt().coerceIn(sTop + 1, bitmap.height)
                                onConfirmInspect(android.graphics.Rect(sLeft, sTop, sRight, sBottom))
                            },
                            shape = SanctuaryShapes.pill,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FindInPage,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Details", fontSize = 12.sp)
                        }
                    }

                    Button(
                        onClick = {
                            val sLeft = (cropNorm.left * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
                            val sTop = (cropNorm.top * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
                            val sRight = (cropNorm.right * bitmap.width).toInt().coerceIn(sLeft + 1, bitmap.width)
                            val sBottom = (cropNorm.bottom * bitmap.height).toInt().coerceIn(sTop + 1, bitmap.height)
                            onConfirmOverlay(android.graphics.Rect(sLeft, sTop, sRight, sBottom))
                        },
                        shape = SanctuaryShapes.pill,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Translate,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Overlay on Page", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
