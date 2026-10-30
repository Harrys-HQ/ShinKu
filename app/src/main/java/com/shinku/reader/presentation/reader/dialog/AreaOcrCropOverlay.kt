package com.shinku.reader.presentation.reader.dialog

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import com.shinku.reader.presentation.core.theme.SanctuaryElevation

/**
 * Google Lens / Google Translate style screen-capture Area OCR overlay.
 * Renders the frozen 1:1 viewport snapshot and provides an interactive bounding box.
 */
@Composable
fun AreaOcrCropOverlay(
    bitmap: Bitmap,
    pageNumber: Int,
    onDismissRequest: () -> Unit,
    onConfirmOverlay: (cropRectOnScreen: android.graphics.Rect) -> Unit,
    onConfirmInspect: ((cropRectOnScreen: android.graphics.Rect) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
    val primaryColor = MaterialTheme.colorScheme.primary
    val cornerColor = MaterialTheme.colorScheme.tertiary
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        val containerWidth = constraints.maxWidth.toFloat()
        val containerHeight = constraints.maxHeight.toFloat()
        val bmpWidth = bitmap.width.toFloat()
        val bmpHeight = bitmap.height.toFloat()

        // 1:1 Scale fitting viewport snapshot cleanly
        val scale = minOf(containerWidth / bmpWidth, containerHeight / bmpHeight)
        val dstWidth = bmpWidth * scale
        val dstHeight = bmpHeight * scale
        val dstLeft = (containerWidth - dstWidth) / 2f
        val dstTop = (containerHeight - dstHeight) / 2f

        // Normalized crop box relative to snapshot (0..1)
        var cropNorm by remember(bitmap) {
            mutableStateOf(Rect(0.20f, 0.35f, 0.80f, 0.55f))
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(dstLeft, dstTop, dstWidth, dstHeight) {
                    var dragMode = 0 // 0: new box, 1: move box, 2: resize bottom-right
                    var startPoint = Offset.Zero

                    detectDragGestures(
                        onDragStart = { offset ->
                            val sLeft = dstLeft + cropNorm.left * dstWidth
                            val sTop = dstTop + cropNorm.top * dstHeight
                            val sRight = dstLeft + cropNorm.right * dstWidth
                            val sBottom = dstTop + cropNorm.bottom * dstHeight

                            val handleTol = 44.dp.toPx()
                            val nearBR = (offset.x - sRight).let { it * it } + (offset.y - sBottom).let { it * it } < handleTol * handleTol
                            val inside = offset.x in sLeft..sRight && offset.y in sTop..sBottom

                            when {
                                nearBR -> dragMode = 2
                                inside -> dragMode = 1
                                else -> {
                                    dragMode = 0
                                    startPoint = offset
                                    val nX = ((offset.x - dstLeft) / dstWidth).coerceIn(0f, 1f)
                                    val nY = ((offset.y - dstTop) / dstHeight).coerceIn(0f, 1f)
                                    cropNorm = Rect(nX, nY, nX, nY)
                                }
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
                                    val currentB = dstTop + cropNorm.bottom * dstHeight + dragAmount.y
                                    val nR = ((currentR - dstLeft) / dstWidth).coerceIn(cropNorm.left + 0.05f, 1f)
                                    val nB = ((currentB - dstTop) / dstHeight).coerceIn(cropNorm.top + 0.03f, 1f)
                                    cropNorm = Rect(cropNorm.left, cropNorm.top, nR, nB)
                                }
                                else -> {
                                    // Drawing new selection box
                                    val p1 = startPoint
                                    val p2 = change.position
                                    val minX = minOf(p1.x, p2.x)
                                    val maxX = maxOf(p1.x, p2.x)
                                    val minY = minOf(p1.y, p2.y)
                                    val maxY = maxOf(p1.y, p2.y)

                                    val nLeft = ((minX - dstLeft) / dstWidth).coerceIn(0f, 1f)
                                    val nTop = ((minY - dstTop) / dstHeight).coerceIn(0f, 1f)
                                    val nRight = ((maxX - dstLeft) / dstWidth).coerceIn(0f, 1f)
                                    val nBottom = ((maxY - dstTop) / dstHeight).coerceIn(0f, 1f)
                                    cropNorm = Rect(nLeft, nTop, maxOf(nRight, nLeft + 0.02f), maxOf(nBottom, nTop + 0.02f))
                                }
                            }
                        },
                    )
                },
        ) {
            // 1. Draw frozen 1:1 viewport snapshot
            drawImage(
                image = imageBitmap,
                dstOffset = IntOffset(dstLeft.toInt(), dstTop.toInt()),
                dstSize = IntSize(dstWidth.toInt(), dstHeight.toInt()),
            )

            // 2. Compute screen coordinates of the crop box
            val cropScreenLeft = dstLeft + cropNorm.left * dstWidth
            val cropScreenTop = dstTop + cropNorm.top * dstHeight
            val cropScreenWidth = cropNorm.width * dstWidth
            val cropScreenHeight = cropNorm.height * dstHeight

            // 3. Draw dimmed scrim outside selected area
            val cropRectPath = Path().apply {
                addRect(Rect(cropScreenLeft, cropScreenTop, cropScreenLeft + cropScreenWidth, cropScreenTop + cropScreenHeight))
            }
            clipPath(cropRectPath, clipOp = ClipOp.Difference) {
                drawRect(Color.Black.copy(alpha = 0.60f))
            }

            // 4. Draw bounding box border
            drawRect(
                color = primaryColor,
                topLeft = Offset(cropScreenLeft, cropScreenTop),
                size = Size(cropScreenWidth, cropScreenHeight),
                style = Stroke(width = 2.dp.toPx()),
            )

            // 5. Draw sleek corner indicators for handle grab
            val cLen = 18.dp.toPx()
            val cStroke = 3.5.dp.toPx()
            val r = cropScreenLeft + cropScreenWidth
            val b = cropScreenTop + cropScreenHeight

            // Top-left corner
            drawLine(primaryColor, Offset(cropScreenLeft, cropScreenTop), Offset(cropScreenLeft + cLen, cropScreenTop), strokeWidth = cStroke)
            drawLine(primaryColor, Offset(cropScreenLeft, cropScreenTop), Offset(cropScreenLeft, cropScreenTop + cLen), strokeWidth = cStroke)

            // Top-right corner
            drawLine(primaryColor, Offset(r, cropScreenTop), Offset(r - cLen, cropScreenTop), strokeWidth = cStroke)
            drawLine(primaryColor, Offset(r, cropScreenTop), Offset(r, cropScreenTop + cLen), strokeWidth = cStroke)

            // Bottom-left corner
            drawLine(primaryColor, Offset(cropScreenLeft, b), Offset(cropScreenLeft + cLen, b), strokeWidth = cStroke)
            drawLine(primaryColor, Offset(cropScreenLeft, b), Offset(cropScreenLeft, b - cLen), strokeWidth = cStroke)

            // Bottom-right corner (accented resize handle)
            drawLine(cornerColor, Offset(r - cLen, b), Offset(r, b), strokeWidth = cStroke)
            drawLine(cornerColor, Offset(r, b - cLen), Offset(r, b), strokeWidth = cStroke)
            drawCircle(
                color = cornerColor,
                radius = 4.dp.toPx(),
                center = Offset(r, b),
            )
        }

        // Top-start badge: minimal non-intrusive floating pill
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 16.dp, top = 12.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
            tonalElevation = SanctuaryElevation.subtle,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f)),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.CropFree,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = "ShinKu Lens",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        // Top-end close button: minimal floating circular button
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(end = 16.dp, top = 12.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
            tonalElevation = SanctuaryElevation.subtle,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f)),
        ) {
            IconButton(
                onClick = onDismissRequest,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Close",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        // Bottom floating action pill (FAB bar)
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 20.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            tonalElevation = SanctuaryElevation.hero,
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                IconButton(
                    onClick = {
                        cropNorm = Rect(0.20f, 0.35f, 0.80f, 0.55f)
                    },
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "Reset Selection",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }

                if (onConfirmInspect != null) {
                    FilledTonalButton(
                        onClick = {
                            val sLeft = (cropNorm.left * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
                            val sTop = (cropNorm.top * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
                            val sRight = (cropNorm.right * bitmap.width).toInt().coerceIn(sLeft + 1, bitmap.width)
                            val sBottom = (cropNorm.bottom * bitmap.height).toInt().coerceIn(sTop + 1, bitmap.height)
                            onConfirmInspect(android.graphics.Rect(sLeft, sTop, sRight, sBottom))
                        },
                        shape = CircleShape,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FindInPage,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Details",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false,
                        )
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
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Translate,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Translate",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
    }
}
