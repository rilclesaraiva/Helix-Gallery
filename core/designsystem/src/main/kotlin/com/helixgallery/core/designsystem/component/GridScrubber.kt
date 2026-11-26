// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.helixgallery.core.designsystem.theme.helixEffectsSpec
import com.helixgallery.core.model.GridEntry
import com.helixgallery.core.model.epochDay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Rolagem rápida por data (modelo do BoomListener): uma alça na borda direita que aparece ao
 * rolar. Só a alça responde ao dedo: o trilho não pega toque (correr o dedo ali rola a grade
 * normalmente) e, ao segurar a alça, ela segue o dedo a partir de onde está, sem pular.
 * Enquanto está segurada, a data aparece num balão ao lado.
 *
 * @param headerCount itens da grade antes das fotos (ex.: o título da tela), que não entram na conta.
 */
@Composable
fun GridScrubber(
    state: LazyGridState,
    entries: List<GridEntry>,
    modifier: Modifier = Modifier,
    headerCount: Int = 0,
) {
    val scope = rememberCoroutineScope()
    val locale = currentLocale()
    var dragging by remember { mutableStateOf(false) }
    var active by remember { mutableStateOf(false) }
    val scrolling = state.isScrollInProgress
    LaunchedEffect(scrolling, dragging) {
        if (scrolling || dragging) {
            active = true
        } else {
            delay(HIDE_AFTER_MS)
            active = false
        }
    }
    val alpha by animateFloatAsState(
        targetValue = if (active && entries.size > MIN_ENTRIES) 1f else 0f,
        animationSpec = helixEffectsSpec(),
        label = "scrubberAlpha",
    )
    val total = entries.size
    val density = LocalDensity.current

    BoxWithConstraints(modifier.fillMaxHeight().width(TrackWidth)) {
        val heightPx = with(density) { maxHeight.toPx() }
        val thumbPx = with(density) { ThumbHeight.toPx() }
        val travel = (heightPx - thumbPx).coerceAtLeast(1f)
        val index = (state.firstVisibleItemIndex - headerCount).coerceIn(0, (total - 1).coerceAtLeast(0))
        val fraction = if (total <= 1) 0f else index.toFloat() / (total - 1)
        // Onde a alça está desenhada; durante o arrasto, onde o dedo a levou.
        var dragFraction by remember { mutableFloatStateOf(0f) }
        val shown = if (dragging) dragFraction else fraction
        val offsetY = (shown * travel).roundToInt()

        fun scrollToFraction(f: Float) {
            if (total <= 1) return
            val target = (f * (total - 1)).roundToInt() + headerCount
            scope.launch { state.scrollToItem(target) }
        }

        Box(Modifier.fillMaxHeight().width(TrackWidth).alpha(alpha)) {
            // Só a alça tem área de toque; o resto da faixa deixa o toque passar para a grade.
            val thumbTouch = if (alpha > 0.05f) {
                Modifier.pointerInput(total, headerCount, travel) {
                    detectVerticalDragGestures(
                        onDragStart = {
                            dragFraction = fraction
                            dragging = true
                        },
                        onDragEnd = { dragging = false },
                        onDragCancel = { dragging = false },
                        onVerticalDrag = { change, dy ->
                            change.consume()
                            dragFraction = (dragFraction + dy / travel).coerceIn(0f, 1f)
                            scrollToFraction(dragFraction)
                        },
                    )
                }
            } else {
                Modifier
            }
            Box(
                Modifier
                    .offset { IntOffset(0, offsetY) }
                    .align(Alignment.TopEnd)
                    .size(width = TrackWidth, height = ThumbHeight)
                    .then(thumbTouch),
            ) {
                Box(
                    Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp)
                        .size(width = if (dragging) ThumbWidthDragging else ThumbWidth, height = ThumbHeight)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
            if (dragging && total > 0) {
                val epoch = entries[index].epochDay()
                val label = remember(epoch, locale) {
                    LocalDate.ofEpochDay(epoch)
                        .format(DateTimeFormatter.ofPattern("LLLL yyyy", locale))
                        .replaceFirstChar { it.titlecase(locale) }
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset { IntOffset(-with(density) { 20.dp.roundToPx() }, offsetY + with(density) { 6.dp.roundToPx() }) },
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

private val TrackWidth = 36.dp
private val ThumbHeight = 52.dp
private val ThumbWidth = 6.dp
private val ThumbWidthDragging = 10.dp
private const val HIDE_AFTER_MS = 1500L
private const val MIN_ENTRIES = 25
