// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.features.viewer

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.helixgallery.core.designsystem.theme.helixSpatialSpec
import androidx.compose.ui.input.pointer.positionChange
import kotlin.math.abs
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.animation.core.animate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import com.helixgallery.core.designsystem.component.HelixImage

private const val MAX_ZOOM = 6f
private const val DOUBLE_TAP_ZOOM = 2.5f

/**
 * Foto com zoom por pinça e arrasto, e toque duplo para ampliar. Sem zoom, os gestos
 * de um dedo ficam livres para o carrossel passar para a foto ao lado.
 */
@Composable
fun ZoomableImage(
    uri: String,
    onTap: () -> Unit,
    onZoomChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var zoomJob by remember { mutableStateOf<Job?>(null) }
    val zoomSpec = helixSpatialSpec<Float>()
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(scale > 1.01f) { onZoomChanged(scale > 1.01f) }

    Box(
        modifier
            .fillMaxSize()
            .onSizeChanged { size = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    // Toque duplo anima o zoom (nada de salto), ampliando em volta do ponto tocado.
                    onDoubleTap = { tap ->
                        val fromScale = scale
                        val fromOffset = offset
                        val zoomIn = fromScale <= 1.01f
                        val toScale = if (zoomIn) DOUBLE_TAP_ZOOM else 1f
                        val toOffset = if (zoomIn) {
                            val cx = tap.x - size.width / 2f
                            val cy = tap.y - size.height / 2f
                            val maxX = size.width * (toScale - 1f) / 2f
                            val maxY = size.height * (toScale - 1f) / 2f
                            Offset(
                                (-cx * (toScale - 1f)).coerceIn(-maxX, maxX),
                                (-cy * (toScale - 1f)).coerceIn(-maxY, maxY),
                            )
                        } else {
                            Offset.Zero
                        }
                        zoomJob?.cancel()
                        zoomJob = scope.launch {
                            animate(0f, 1f, animationSpec = zoomSpec) { t, _ ->
                                scale = fromScale + (toScale - fromScale) * t
                                offset = Offset(
                                    fromOffset.x + (toOffset.x - fromOffset.x) * t,
                                    fromOffset.y + (toOffset.y - fromOffset.y) * t,
                                )
                            }
                        }
                    },
                )
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    zoomJob?.cancel()
                    do {
                        val event = awaitPointerEvent()
                        val multi = event.changes.size > 1
                        if (multi || scale > 1.01f) {
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            val newScale = (scale * zoom).coerceIn(1f, MAX_ZOOM)
                            val maxX = size.width * (newScale - 1f) / 2f
                            val maxY = size.height * (newScale - 1f) / 2f
                            offset = if (newScale <= 1f) {
                                Offset.Zero
                            } else {
                                Offset(
                                    (offset.x + pan.x).coerceIn(-maxX, maxX),
                                    (offset.y + pan.y).coerceIn(-maxY, maxY),
                                )
                            }
                            scale = newScale
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            },
    ) {
        HelixImage(
            uri = uri,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
        )
    }
}
