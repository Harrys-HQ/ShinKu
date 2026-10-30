package com.shinku.reader.presentation.reader.dialog

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shinku.reader.presentation.core.components.SanctuaryOverlineBadge
import com.shinku.reader.presentation.core.theme.SanctuaryElevation
import com.shinku.reader.presentation.core.theme.SanctuaryShapes

@Composable
fun AreaOcrCropOverlay(
    bitmap: Bitmap,
    pageNumber: Int,
    onDismissRequest: () -> Unit,
    onConfirmArea: (cropRectOnBitmap: android.graphics.Rect) -> Unit,
    modifier: Modifier = Modifier,
) {
    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
    val primaryColor = MaterialTheme.colorScheme.primary
    val cornerColor = MaterialTheme.colorScheme.tertiary

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.90f)),
    ) {
        val containerWidth = constraints.maxWidth.toFloat()
        val containerHeight = constraints.maxHeight.toFloat()

        // Calculate fitted image destination rectangle
        val bmpWidth = bitmap.width.toFloat()
        val bmpHeight = bitmap.height.toFloat()
        val scale = minOf(containerWidth / bmpWidth, containerHeight / bmpHeight)
        val dstWidth = bmpWidth * scale
        val dstHeight = bmpHeight * scale
        val dstLeft = (containerWidth - dstWidth) / 2f
        val dstTop = (containerHeight - dstHeight) / 2f
        val imageBounds = remember(dstLeft, dstTop, dstWidth, dstHeight) {
            Rect(dstLeft, dstTop, dstLeft + dstWidth, dstTop + dstHeight)
        }

        // Normalized crop box relative to image bounds (0..1)
        var cropNorm by remember {
            mutableStateOf(Rect(0.20f, 0.30f, 0.80f, 0.55f))
        }

        // Convert normalized crop to screen coordinates
        val cropScreenLeft = imageBounds.left + cropNorm.left * imageBounds.width
        val cropScreenTop = imageBounds.top + cropNorm.top * imageBounds.height
        val cropScreenWidth = cropNorm.width * imageBounds.width
        val cropScreenHeight = cropNorm.height * imageBounds.height

        // Canvas drawing the image, dimmed scrim, and selection box
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(imageBounds) {
                    var dragMode = 0 // 0: new box, 1: move, 2: resize bottom-right
                    var startPoint = Offset.Zero

                    detectDragGestures(
                        onDragStart = { offset ->
                            val sLeft = imageBounds.left + cropNorm.left * imageBounds.width
                            val sTop = imageBounds.top + cropNorm.top * imageBounds.height
                            val sRight = imageBounds.left + cropNorm.right * imageBounds.width
                            val sBottom = imageBounds.top + cropNorm.bottom * imageBounds.height

                            val handleTolerance = 48.dp.toPx()
                            val isNearBottomRight = (offset.x - sRight).let { it * it } + (offset.y - sBottom).let { it * it } < handleTolerance * handleTolerance
                            val isInside = offset.x in sLeft..sRight && offset.y in sTop..sBottom

                            if (isNearBottomRight) {
                                dragMode = 2
                            } else if (isInside) {
                                dragMode = 1
                            } else {
                                dragMode = 0
                                startPoint = offset
                                val nX = ((offset.x - imageBounds.left) / imageBounds.width).coerceIn(0f, 1f)
                                val nY = ((offset.y - imageBounds.top) / imageBounds.height).coerceIn(0f, 1f)
                                cropNorm = Rect(nX, nY, nX, nY)
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            when (dragMode) {
                                1 -> {
                                    // Move box
                                    val deltaNormX = dragAmount.x / imageBounds.width
                                    val deltaNormY = dragAmount.y / imageBounds.height
                                    val w = cropNorm.width
                                    val h = cropNorm.height
                                    val newLeft = (cropNorm.left + deltaNormX).coerceIn(0f, 1f - w)
                                    val newTop = (cropNorm.top + deltaNormY).coerceIn(0f, 1f - h)
                                    cropNorm = Rect(newLeft, newTop, newLeft + w, newTop + h)
                                }
                                2 -> {
                                    // Resize bottom right
                                    val currentRight = imageBounds.left + cropNorm.right * imageBounds.width + dragAmount.x
                                    val currentBottom = imageBounds.top + cropNorm.bottom * imageBounds.height + dragAmount.y
                                    val nRight = ((currentRight - imageBounds.left) / imageBounds.width).coerceIn(cropNorm.left + 0.05f, 1f)
                                    val nBottom = ((currentBottom - imageBounds.top) / imageBounds.height).coerceIn(cropNorm.top + 0.05f, 1f)
                                    cropNorm = Rect(cropNorm.left, cropNorm.top, nRight, nBottom)
                                }
                                else -> {
                                    // Drawing new box
                                    val p1 = startPoint
                                    val p2 = change.position
                                    val minX = minOf(p1.x, p2.x)
                                    val maxX = maxOf(p1.x, p2.x)
                                    val minY = minOf(p1.y, p2.y)
                                    val maxY = maxOf(p1.y, p2.y)

                                    val nLeft = ((minX - imageBounds.left) / imageBounds.width).coerceIn(0f, 1f)
                                    val nTop = ((minY - imageBounds.top) / imageBounds.height).coerceIn(0f, 1f)
                                    val nRight = ((maxX - imageBounds.left) / imageBounds.width).coerceIn(0f, 1f)
                                    val nBottom = ((maxY - imageBounds.top) / imageBounds.height).coerceIn(0f, 1f)
                                    cropNorm = Rect(nLeft, nTop, maxOf(nRight, nLeft + 0.02f), maxOf(nBottom, nTop + 0.02f))
                                }
                            }
                        },
                    )
                },
        ) {
            // 1. Draw page image scaled to center
            drawImage(
                image = imageBitmap,
                dstOffset = IntOffset(dstLeft.toInt(), dstTop.toInt()),
                dstSize = IntSize(dstWidth.toInt(), dstHeight.toInt()),
            )

            // 2. Draw scrim outside crop box
            val cropRectPath = Path().apply {
                addRect(Rect(cropScreenLeft, cropScreenTop, cropScreenLeft + cropScreenWidth, cropScreenTop + cropScreenHeight))
            }

            clipPath(cropRectPath, clipOp = ClipOp.Difference) {
                drawRect(Color.Black.copy(alpha = 0.65f))
            }

            // 3. Draw crop box border
            drawRect(
                color = primaryColor,
                topLeft = Offset(cropScreenLeft, cropScreenTop),
                size = Size(cropScreenWidth, cropScreenHeight),
                style = Stroke(width = 2.dp.toPx()),
            )

            // 4. Draw corner indicators for easy grabbing
            val cornerLen = 18.dp.toPx()
            val cornerStroke = 4.dp.toPx()
            val right = cropScreenLeft + cropScreenWidth
            val bottom = cropScreenTop + cropScreenHeight

            // Bottom-right handle indicator (resize corner)
            drawLine(cornerColor, Offset(right - cornerLen, bottom), Offset(right, bottom), strokeWidth = cornerStroke)
            drawLine(cornerColor, Offset(right, bottom - cornerLen), Offset(right, bottom), strokeWidth = cornerStroke)

            // Top-left corner
            drawLine(primaryColor, Offset(cropScreenLeft, cropScreenTop), Offset(cropScreenLeft + cornerLen, cropScreenTop), strokeWidth = cornerStroke)
            drawLine(primaryColor, Offset(cropScreenLeft, cropScreenTop), Offset(cropScreenLeft, cropScreenTop + cornerLen), strokeWidth = cornerStroke)
        }

        // Top bar overlay
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            shape = SanctuaryShapes.card,
            tonalElevation = SanctuaryElevation.card,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
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
                            text = "Select Bubble or Panel Area",
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
                .padding(horizontal = 20.dp, vertical = 16.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            shape = SanctuaryShapes.hero,
            tonalElevation = SanctuaryElevation.hero,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                FilledTonalButton(
                    onClick = {
                        cropNorm = Rect(0.20f, 0.30f, 0.80f, 0.55f)
                    },
                    shape = SanctuaryShapes.pill,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Reset", fontSize = 13.sp)
                }

                Button(
                    onClick = {
                        val srcLeft = (cropNorm.left * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
                        val srcTop = (cropNorm.top * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
                        val srcRight = (cropNorm.right * bitmap.width).toInt().coerceIn(srcLeft + 1, bitmap.width)
                        val srcBottom = (cropNorm.bottom * bitmap.height).toInt().coerceIn(srcTop + 1, bitmap.height)
                        onConfirmArea(android.graphics.Rect(srcLeft, srcTop, srcRight, srcBottom))
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
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Translate Area", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
