// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.features.viewer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.helixgallery.core.designsystem.component.HelixImage
import com.helixgallery.core.designsystem.theme.helixEffectsSpec
import com.helixgallery.core.designsystem.theme.helixFastSpatialSpec
import com.helixgallery.data.media.RegionCorner
import com.helixgallery.data.media.TextRegion
import androidx.compose.ui.layout.ContentScale
import kotlin.math.hypot
import kotlin.math.min

/** Onde a imagem aparece dentro da caixa (ContentScale.Fit): posição e tamanho em pixels. */
private data class FitRect(val left: Float, val top: Float, val width: Float, val height: Float)

private fun fitRect(box: IntSize, imageWidth: Float, imageHeight: Float): FitRect? {
    if (box.width <= 0 || box.height <= 0 || imageWidth <= 0f || imageHeight <= 0f) return null
    val scale = min(box.width / imageWidth, box.height / imageHeight)
    val w = imageWidth * scale
    val h = imageHeight * scale
    return FitRect((box.width - w) / 2f, (box.height - h) / 2f, w, h)
}

private enum class DragMode { None, Move, Corner }

/**
 * Tela cheia para escolher a parte da imagem de onde ler o texto: um retângulo que se move e muda de tamanho
 * pelos cantos. Começa com a imagem inteira. O resto da imagem não é lido.
 */
@Composable
internal fun TextAreaPicker(uri: String, onRead: (TextRegion) -> Unit, onCancel: () -> Unit) {
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        var shown by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { shown = true }
        val appear by animateFloatAsState(if (shown) 1f else 0f, helixEffectsSpec(), label = "areaAppear")
        var region by remember { mutableStateOf(TextRegion.Full) }
        var box by remember { mutableStateOf(IntSize.Zero) }
        var imageSize by remember { mutableStateOf<Pair<Float, Float>?>(null) }
        val fit = imageSize?.let { fitRect(box, it.first, it.second) }

        Surface(
            modifier = Modifier.fillMaxSize().graphicsLayer { alpha = appear },
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Column(Modifier.fillMaxSize().systemBarsPadding().padding(16.dp)) {
                Text(stringResource(R.string.viewer_area_title), style = MaterialTheme.typography.titleLarge)
                Text(
                    stringResource(R.string.viewer_area_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                )
                Box(
                    Modifier.weight(1f).fillMaxWidth().onSizeChanged { box = it },
                    contentAlignment = Alignment.Center,
                ) {
                    HelixImage(
                        uri = uri,
                        contentScale = ContentScale.Fit,
                        onImageSize = { w, h -> imageSize = w to h },
                        modifier = Modifier.fillMaxSize(),
                    )
                    if (fit != null) AreaOverlay(fit = fit, region = region, onRegion = { region = it })
                }
                Row(
                    Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onCancel) { Text(stringResource(R.string.viewer_area_cancel)) }
                    Button(onClick = { onRead(region) }) { Text(stringResource(R.string.viewer_area_read)) }
                }
            }
        }
    }
}

/** O retângulo da área, com o fundo escurecido fora dele e os cantos para arrastar. */
@Composable
private fun AreaOverlay(fit: FitRect, region: TextRegion, onRegion: (TextRegion) -> Unit) {
    val density = LocalDensity.current
    val handleRadius = with(density) { 9.dp.toPx() }
    val grabSlop = with(density) { 36.dp.toPx() }
    val stroke = with(density) { 2.dp.toPx() }
    val dim = MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f)
    val accent = MaterialTheme.colorScheme.primary
    var dragging by remember { mutableStateOf(false) }
    // Ao arrastar, o retângulo segue o dedo na hora; nas ações de acessibilidade ele anima para o novo lugar.
    val spec = if (dragging) snap() else helixFastSpatialSpec<Float>()
    val left by animateFloatAsState(region.left, spec, label = "areaLeft")
    val top by animateFloatAsState(region.top, spec, label = "areaTop")
    val right by animateFloatAsState(region.right, spec, label = "areaRight")
    val bottom by animateFloatAsState(region.bottom, spec, label = "areaBottom")

    val latestRegion by rememberUpdatedState(region)
    val latestOnRegion by rememberUpdatedState(onRegion)
    // Todo gesto de arrastar tem alternativa para quem usa leitor de tela: mover e mudar o tamanho por ações.
    val description = stringResource(R.string.viewer_area_description)
    val bigger = stringResource(R.string.viewer_area_bigger)
    val smaller = stringResource(R.string.viewer_area_smaller)
    val up = stringResource(R.string.viewer_area_up)
    val down = stringResource(R.string.viewer_area_down)
    val toLeft = stringResource(R.string.viewer_area_left)
    val toRight = stringResource(R.string.viewer_area_right)
    fun change(transform: (TextRegion) -> TextRegion): Boolean {
        latestOnRegion(transform(latestRegion))
        return true
    }
    val actions = listOf(
        CustomAccessibilityAction(bigger) { change { it.scaledAroundCenter(1.25f) } },
        CustomAccessibilityAction(smaller) { change { it.scaledAroundCenter(0.8f) } },
        CustomAccessibilityAction(up) { change { it.movedBy(0f, -0.1f) } },
        CustomAccessibilityAction(down) { change { it.movedBy(0f, 0.1f) } },
        CustomAccessibilityAction(toLeft) { change { it.movedBy(-0.1f, 0f) } },
        CustomAccessibilityAction(toRight) { change { it.movedBy(0.1f, 0f) } },
    )

    Canvas(
        Modifier
            .fillMaxSize()
            .semantics {
                contentDescription = description
                customActions = actions
            }
            .pointerInput(fit) {
                var mode = DragMode.None
                var corner = RegionCorner.TopLeft
                detectDragGestures(
                    onDragStart = { start ->
                        val current = latestRegion
                        val corners = RegionCorner.entries.map { c ->
                            c to Offset(
                                fit.left + (if (c == RegionCorner.TopLeft || c == RegionCorner.BottomLeft) current.left else current.right) * fit.width,
                                fit.top + (if (c == RegionCorner.TopLeft || c == RegionCorner.TopRight) current.top else current.bottom) * fit.height,
                            )
                        }
                        val nearest = corners.minBy { (_, p) -> hypot(p.x - start.x, p.y - start.y) }
                        val inside = start.x in (fit.left + current.left * fit.width)..(fit.left + current.right * fit.width) &&
                            start.y in (fit.top + current.top * fit.height)..(fit.top + current.bottom * fit.height)
                        mode = when {
                            hypot(nearest.second.x - start.x, nearest.second.y - start.y) <= grabSlop -> DragMode.Corner.also { corner = nearest.first }
                            inside -> DragMode.Move
                            else -> DragMode.None
                        }
                        dragging = mode != DragMode.None
                    },
                    onDragEnd = { dragging = false },
                    onDragCancel = { dragging = false },
                ) { change, amount ->
                    val current = latestRegion
                    when (mode) {
                        DragMode.Move -> {
                            change.consume()
                            latestOnRegion(current.movedBy(amount.x / fit.width, amount.y / fit.height))
                        }
                        DragMode.Corner -> {
                            change.consume()
                            val cx = (if (corner == RegionCorner.TopLeft || corner == RegionCorner.BottomLeft) current.left else current.right)
                            val cy = (if (corner == RegionCorner.TopLeft || corner == RegionCorner.TopRight) current.top else current.bottom)
                            latestOnRegion(current.withCorner(corner, cx + amount.x / fit.width, cy + amount.y / fit.height))
                        }
                        DragMode.None -> Unit
                    }
                }
            },
    ) {
        val l = fit.left + left.coerceIn(0f, 1f) * fit.width
        val t = fit.top + top.coerceIn(0f, 1f) * fit.height
        val r = fit.left + right.coerceIn(0f, 1f) * fit.width
        val b = fit.top + bottom.coerceIn(0f, 1f) * fit.height
        // Escurece só a imagem, em volta da área escolhida.
        drawRect(dim, Offset(fit.left, fit.top), Size(fit.width, t - fit.top))
        drawRect(dim, Offset(fit.left, b), Size(fit.width, fit.top + fit.height - b))
        drawRect(dim, Offset(fit.left, t), Size(l - fit.left, b - t))
        drawRect(dim, Offset(r, t), Size(fit.left + fit.width - r, b - t))
        drawRect(accent, Offset(l, t), Size(r - l, b - t), style = Stroke(stroke))
        listOf(Offset(l, t), Offset(r, t), Offset(l, b), Offset(r, b)).forEach { drawCircle(accent, handleRadius, it) }
    }
}
