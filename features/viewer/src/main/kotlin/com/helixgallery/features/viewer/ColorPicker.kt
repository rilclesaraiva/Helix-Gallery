// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.features.viewer

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.helixgallery.core.designsystem.component.HelixFloatingBackButton
import com.helixgallery.core.designsystem.component.HelixIcons
import com.helixgallery.core.designsystem.theme.helixEffectsSpec
import com.helixgallery.core.designsystem.theme.helixSpatialSpec
import com.helixgallery.core.designsystem.theme.readableOn
import com.helixgallery.data.media.hexOf
import com.helixgallery.data.media.loadPickerBitmap
import com.helixgallery.data.media.panSensitivity
import com.helixgallery.data.media.sampleColor
import kotlin.math.min
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private const val MIN_ZOOM = 0.5f
private const val MAX_ZOOM = 40f

/** Ampliação do toque duplo: a mesma do visualizador de fotos. */
private const val DOUBLE_TAP_ZOOM = 2.5f

/**
 * Seletor de cor em tela cheia: a imagem se move e se amplia por baixo de uma bolinha fixa no centro, e a barra
 * de baixo mostra a cor do ponto (em hexadecimal) e ganha essa cor. Tocar na barra copia o código.
 */
@Composable
internal fun ColorPicker(uri: String, onClose: () -> Unit) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val context = LocalContext.current
        var shown by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { shown = true }
        val appear by animateFloatAsState(if (shown) 1f else 0f, helixEffectsSpec(), label = "colorAppear")
        // null = carregando; Result.failure = não abriu.
        val loaded by produceState<Result<Bitmap>?>(null, uri) {
            value = loadPickerBitmap(context, uri)?.let { Result.success(it) } ?: Result.failure(IllegalStateException())
        }

        Surface(
            modifier = Modifier.fillMaxSize().graphicsLayer { alpha = appear },
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            val result = loaded
            // O Surface manda o filho ocupar tudo; o botão de voltar precisa de um Box próprio para ficar do tamanho dele.
            Box(Modifier.fillMaxSize()) {
                when {
                    result == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    result.isFailure -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.viewer_text_failed), style = MaterialTheme.typography.bodyLarge)
                    }
                    else -> PickerContent(result.getOrThrow())
                }
                HelixFloatingBackButton(onClick = onClose, modifier = Modifier.align(Alignment.TopStart))
            }
        }
    }
}

@Composable
private fun PickerContent(bitmap: Bitmap) {
    val context = LocalContext.current
    val image = remember(bitmap) { bitmap.asImageBitmap() }
    var box by remember { mutableStateOf(IntSize.Zero) }
    // O ponto da imagem (em pixels dela) que está sob a bolinha do centro, e a ampliação em cima do "caber na tela".
    var cx by remember(bitmap) { mutableFloatStateOf(bitmap.width / 2f) }
    var cy by remember(bitmap) { mutableFloatStateOf(bitmap.height / 2f) }
    var zoom by remember(bitmap) { mutableFloatStateOf(1f) }
    val fitScale = if (box.width == 0) 1f else min(box.width / bitmap.width.toFloat(), box.height / bitmap.height.toFloat())
    val scale = fitScale * zoom

    val rgb = remember(bitmap, cx, cy) { sampleColor(bitmap, cx.toInt(), cy.toInt()) }
    val picked by animateColorAsState(Color(0xFF000000.toInt() or rgb), helixEffectsSpec(), label = "pickedColor")
    val hex = hexOf(rgb)

    val density = LocalDensity.current
    val ringRadius = with(density) { 16.dp.toPx() }
    val ringStroke = with(density) { 2.dp.toPx() }
    val ringOuter = MaterialTheme.colorScheme.surface
    val ringInner = MaterialTheme.colorScheme.onSurface

    fun moveBy(dxPx: Float, dyPx: Float) {
        cx = (cx - dxPx / scale).coerceIn(0f, bitmap.width - 1f)
        cy = (cy - dyPx / scale).coerceIn(0f, bitmap.height - 1f)
    }

    // Toque duplo como no visualizador de fotos: alterna entre caber na tela e 2,5×, animado, em volta do ponto tocado.
    val scope = rememberCoroutineScope()
    var zoomJob by remember { mutableStateOf<Job?>(null) }
    val zoomSpec = helixSpatialSpec<Float>()
    fun doubleTapZoom(tap: Offset) {
        val fromZoom = zoom
        val fromX = cx
        val fromY = cy
        val zoomIn = fromZoom <= 1.01f
        val toZoom = if (zoomIn) DOUBLE_TAP_ZOOM else 1f
        // Ao ampliar, o ponto tocado fica onde está na tela; ao reduzir, a imagem volta inteira e centrada.
        val toX: Float
        val toY: Float
        if (zoomIn) {
            val toScale = fitScale * toZoom
            val pointX = fromX + (tap.x - box.width / 2f) / scale
            val pointY = fromY + (tap.y - box.height / 2f) / scale
            toX = (pointX - (tap.x - box.width / 2f) / toScale).coerceIn(0f, bitmap.width - 1f)
            toY = (pointY - (tap.y - box.height / 2f) / toScale).coerceIn(0f, bitmap.height - 1f)
        } else {
            toX = bitmap.width / 2f
            toY = bitmap.height / 2f
        }
        zoomJob?.cancel()
        zoomJob = scope.launch {
            animate(0f, 1f, animationSpec = zoomSpec) { t, _ ->
                zoom = (fromZoom + (toZoom - fromZoom) * t).coerceIn(MIN_ZOOM, MAX_ZOOM)
                cx = (fromX + (toX - fromX) * t).coerceIn(0f, bitmap.width - 1f)
                cy = (fromY + (toY - fromY) * t).coerceIn(0f, bitmap.height - 1f)
            }
        }
    }

    val description = stringResource(R.string.viewer_color_description)
    val zoomIn = stringResource(R.string.viewer_color_zoom_in)
    val zoomOut = stringResource(R.string.viewer_color_zoom_out)
    val up = stringResource(R.string.viewer_color_up)
    val down = stringResource(R.string.viewer_color_down)
    val toLeft = stringResource(R.string.viewer_color_left)
    val toRight = stringResource(R.string.viewer_color_right)
    val step = with(density) { 48.dp.toPx() }
    val copiedToast = R.string.viewer_color_copied
    val hexDescription = stringResource(R.string.viewer_color_pill_description, hex)

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .onSizeChanged { box = it }
                // Todo gesto de arrastar tem alternativa para quem usa leitor de tela.
                .semantics {
                    contentDescription = description
                    customActions = listOf(
                        CustomAccessibilityAction(zoomIn) { zoom = (zoom * 1.5f).coerceAtMost(MAX_ZOOM); true },
                        CustomAccessibilityAction(zoomOut) { zoom = (zoom / 1.5f).coerceAtLeast(MIN_ZOOM); true },
                        CustomAccessibilityAction(up) { moveBy(0f, step); true },
                        CustomAccessibilityAction(down) { moveBy(0f, -step); true },
                        CustomAccessibilityAction(toLeft) { moveBy(step, 0f); true },
                        CustomAccessibilityAction(toRight) { moveBy(-step, 0f); true },
                    )
                }
                .pointerInput(bitmap) {
                    detectTapGestures(onDoubleTap = { tap -> doubleTapZoom(tap) })
                }
                .pointerInput(bitmap) {
                    detectTransformGestures { _, pan, zoomChange, _ ->
                        zoomJob?.cancel()
                        zoom = (zoom * zoomChange).coerceIn(MIN_ZOOM, MAX_ZOOM)
                        // O ponto anda menos que o dedo (e menos ainda com mais zoom): ajuste fino da cor.
                        val k = panSensitivity(zoom)
                        moveBy(pan.x * k, pan.y * k)
                    }
                },
        ) {
            Canvas(Modifier.fillMaxSize()) {
                // Cada pixel da imagem vira um quadrado nítido quando bem ampliada (dá para ver a cor exata de cada um).
                drawImage(
                    image = image,
                    dstOffset = IntOffset((size.width / 2f - cx * scale).toInt(), (size.height / 2f - cy * scale).toInt()),
                    dstSize = IntSize((bitmap.width * scale).toInt().coerceAtLeast(1), (bitmap.height * scale).toInt().coerceAtLeast(1)),
                    filterQuality = if (scale >= 3f) FilterQuality.None else FilterQuality.Low,
                )
                val center = Offset(size.width / 2f, size.height / 2f)
                drawCircle(ringOuter, ringRadius + ringStroke, center, style = Stroke(ringStroke))
                drawCircle(ringInner, ringRadius, center, style = Stroke(ringStroke))
            }
        }
        // O código numa pílula pequena que flutua embaixo, com a cor do ponto e um contorno para aparecer sobre qualquer foto.
        val onPicked = readableOn(picked)
        Surface(
            color = picked,
            contentColor = onPicked,
            shape = MaterialTheme.shapes.extraLarge,
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.surface),
            shadowElevation = 6.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
                .semantics { contentDescription = hexDescription }
                .clickable { copyToClipboard(context, hex, copiedToast) },
        ) {
            Row(
                Modifier.padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(hex, style = MaterialTheme.typography.titleLarge)
                Icon(HelixIcons.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}
