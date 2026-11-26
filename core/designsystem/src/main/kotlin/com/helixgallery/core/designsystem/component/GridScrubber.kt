// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.core.designsystem.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.helixgallery.core.designsystem.theme.helixEffectsSpec
import com.helixgallery.core.designsystem.theme.helixFastSpatialSpec
import com.helixgallery.core.model.GridEntry
import com.helixgallery.core.model.epochDay
import com.helixgallery.core.model.gridLineStarts
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.roundToInt

private val TrackWidth = 36.dp
private val ThumbHeight = 52.dp
private val ThumbWidth = 6.dp
private val ThumbWidthDragging = 10.dp
private val BubbleSize = 64.dp
private const val HIDE_AFTER_MS = 1_500L

/** Abaixo disso a grade é curta e a barra só atrapalharia. */
private const val MIN_ITEMS = 25

/**
 * Rolagem rápida por data (porte da barra do BoomListener, que já passou por todas as correções):
 * aparece ao rolar e some 1,5 s depois de parar. O trilho NÃO pega toque (correr o dedo por ele
 * rola a grade normalmente); só segurar a bolinha arrasta, a partir de onde a grade está, sem
 * pular. A posição é medida em pixels de verdade, por linha da grade, e não por item: títulos
 * de dia e linhas de fotos têm alturas bem diferentes, e contar itens fazia a bolinha saltar.
 *
 * @param columns colunas da grade (para saber onde cada linha começa).
 * @param headerCount itens da grade antes das fotos (ex.: o título da tela).
 */
@Composable
fun GridScrubber(
    state: LazyGridState,
    entries: List<GridEntry>,
    columns: Int,
    modifier: Modifier = Modifier,
    headerCount: Int = 0,
) {
    val locale = currentLocale()
    val lineStarts = remember(entries, columns, headerCount) { gridLineStarts(entries, columns, headerCount) }
    val metrics = remember(state, lineStarts) { GridScrollMetrics(state, lineStarts) }
    if (entries.size < MIN_ITEMS) return

    var dragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    // Só a última posição pedida importa: rola até ela e descarta as que ficaram para trás
    // (senão a grade vai aos trancos atrás do dedo).
    var target by remember { mutableStateOf<Float?>(null) }
    LaunchedEffect(metrics) {
        snapshotFlow { target }.filterNotNull().collectLatest { metrics.scrollTo(it) }
    }
    val isScrolling by remember { derivedStateOf { state.isScrollInProgress } }

    // Aparece ao rolar ou arrastar; some um pouco depois de parar.
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(isScrolling, dragging) {
        if (dragging || isScrolling) {
            shown = true
        } else {
            delay(HIDE_AFTER_MS)
            shown = false
        }
    }
    val alpha by animateFloatAsState(if (shown) 1f else 0f, helixEffectsSpec(), label = "scrubberAlpha")
    val thumbWidth by animateDpAsState(
        if (dragging) ThumbWidthDragging else ThumbWidth,
        helixFastSpatialSpec(),
        label = "thumbWidth",
    )

    val gridFraction by remember(metrics) { derivedStateOf { metrics.fraction() } }
    // Escondida e parada, a posição não importa: fica com a última conhecida, para a bolinha
    // não saltar enquanto some.
    val lastFraction = remember { floatArrayOf(0f) }
    val currentFraction = when {
        dragging -> dragFraction
        alpha > 0f -> gridFraction.also { lastFraction[0] = it }
        else -> lastFraction[0]
    }

    BoxWithConstraints(modifier.fillMaxHeight().padding(vertical = 8.dp)) {
        val density = LocalDensity.current
        val trackPx = with(density) { maxHeight.toPx() }
        val thumbPx = with(density) { ThumbHeight.toPx() }
        val currentTrackPx = rememberUpdatedState(trackPx)
        val currentThumbPx = rememberUpdatedState(thumbPx)
        val thumbY = ((trackPx - thumbPx) * currentFraction).roundToInt()

        // Balão com a data, ao lado da bolinha, enquanto arrasta.
        if (dragging) {
            val entryIndex = (metrics.indexAt(currentFraction) - headerCount).coerceIn(0, (entries.size - 1).coerceAtLeast(0))
            val epoch = entries.getOrNull(entryIndex)?.epochDay()
            val label = remember(epoch, locale) {
                epoch?.let {
                    LocalDate.ofEpochDay(it)
                        .format(DateTimeFormatter.ofPattern("LLLL yyyy", locale))
                        .replaceFirstChar { c -> c.titlecase(locale) }
                }
            }
            if (label != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset {
                            val bubble = BubbleSize.roundToPx()
                            IntOffset(
                                x = -(TrackWidth.roundToPx() + 8.dp.roundToPx()),
                                y = (thumbY + thumbPx.roundToInt() / 2 - bubble / 2).coerceAtLeast(0),
                            )
                        }
                        .height(BubbleSize)
                        .widthIn(min = BubbleSize),
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 20.dp)) {
                        Text(label, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                    }
                }
            }
        }

        // O trilho em si não pega toque: tocar ou arrastar fora da bolinha rola a grade
        // normalmente. Só segurar a bolinha (a área dela, mais larga que o desenho) arrasta.
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .width(TrackWidth)
                .fillMaxHeight()
                .alpha(alpha),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset { IntOffset(0, thumbY) }
                    .size(width = TrackWidth, height = ThumbHeight)
                    // Só pega o toque quando está à mostra: escondida, a grade rola normal.
                    .then(
                        if (shown) {
                            Modifier.pointerInput(metrics) {
                                try {
                                    detectVerticalDragGestures(
                                        onDragStart = {
                                            // Parte de onde a grade está: segurar a bolinha não a faz pular.
                                            dragFraction = gridFraction
                                            dragging = true
                                        },
                                        onDragEnd = { dragging = false },
                                        onDragCancel = { dragging = false },
                                        onVerticalDrag = { change, delta ->
                                            change.consume()
                                            // Pelo quanto o dedo andou (a área da bolinha anda junto com ela).
                                            val room = (currentTrackPx.value - currentThumbPx.value).coerceAtLeast(1f)
                                            dragFraction = (dragFraction + delta / room).coerceIn(0f, 1f)
                                            target = dragFraction
                                        },
                                    )
                                } finally {
                                    // Se o gesto for interrompido (a tela mudou por baixo), o balão não fica preso.
                                    dragging = false
                                }
                            }
                        } else {
                            Modifier
                        },
                    ),
            ) {
                Box(
                    Modifier
                        .size(width = thumbWidth, height = ThumbHeight)
                        .background(
                            if (dragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            CircleShape,
                        ),
                )
            }
        }
    }
}

/**
 * Onde a grade está, em pixels de verdade, por linha: guarda a altura de cada linha já medida
 * (as que ainda não passaram pela tela contam com a média das medidas). Rolando devagar, a
 * bolinha anda junto com a grade, sem saltos.
 */
private class GridScrollMetrics(private val state: LazyGridState, private val lineStarts: IntArray) {
    private var sizes = IntArray(lineStarts.size)
    private var cachedHeights: FloatArray? = null

    private fun lineOf(itemIndex: Int): Int {
        var low = 0
        var high = lineStarts.lastIndex
        while (low < high) {
            val mid = (low + high + 1) ushr 1
            if (lineStarts[mid] <= itemIndex) low = mid else high = mid - 1
        }
        return low
    }

    private fun measure() {
        val info = state.layoutInfo
        val spacing = info.mainAxisItemSpacing
        val perLine = HashMap<Int, Int>()
        info.visibleItemsInfo.forEach { item ->
            val line = lineOf(item.index)
            val height = item.size.height + spacing
            if (height > (perLine[line] ?: 0)) perLine[line] = height
        }
        perLine.forEach { (line, height) ->
            if (line in sizes.indices && sizes[line] != height) {
                sizes[line] = height
                cachedHeights = null
            }
        }
    }

    private fun heights(): FloatArray {
        cachedHeights?.let { return it }
        var sum = 0L
        var known = 0
        for (size in sizes) {
            if (size > 0) {
                sum += size
                known++
            }
        }
        val average = if (known > 0) sum.toFloat() / known else 1f
        return FloatArray(sizes.size) { sizes[it].takeIf { s -> s > 0 }?.toFloat() ?: average }.also { cachedHeights = it }
    }

    private fun viewport(): Float {
        val info = state.layoutInfo
        return (info.viewportSize.height - info.beforeContentPadding - info.afterContentPadding).toFloat()
    }

    /** De 0 (topo) a 1 (fim): quanto da grade já passou. */
    fun fraction(): Float {
        measure()
        val heights = heights()
        val max = heights.sum() - viewport()
        if (max <= 0f) return 0f
        if (!state.canScrollForward) return 1f
        val firstLine = lineOf(state.firstVisibleItemIndex)
        var scrolled = state.firstVisibleItemScrollOffset.toFloat()
        for (i in 0 until firstLine.coerceAtMost(heights.size)) scrolled += heights[i]
        return (scrolled / max).coerceIn(0f, 1f)
    }

    /** O primeiro item da linha na altura da bolinha: o balão mostra a data dele. */
    fun indexAt(fraction: Float): Int {
        measure()
        val heights = heights()
        if (heights.isEmpty()) return 0
        var remaining = fraction.coerceIn(0f, 1f) * heights.sum()
        var line = 0
        while (line < heights.lastIndex && remaining >= heights[line]) {
            remaining -= heights[line]
            line++
        }
        return lineStarts[line]
    }

    suspend fun scrollTo(fraction: Float) {
        measure()
        val heights = heights()
        if (heights.isEmpty()) return
        var remaining = (fraction * (heights.sum() - viewport())).coerceAtLeast(0f)
        var line = 0
        while (line < heights.lastIndex && remaining >= heights[line]) {
            remaining -= heights[line]
            line++
        }
        state.scrollToItem(lineStarts[line], remaining.roundToInt())
    }
}
