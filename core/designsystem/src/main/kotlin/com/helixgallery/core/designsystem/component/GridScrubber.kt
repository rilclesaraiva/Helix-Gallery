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
 * Rolagem rápida por data: uma alça na borda direita que aparece ao rolar. Arrastar
 * a alça leva a grade até a data, que aparece num balão ao lado.
 */
@Composable
fun GridScrubber(
    state: LazyGridState,
    entries: List<GridEntry>,
    modifier: Modifier = Modifier,
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
            delay(1500)
            active = false
        }
    }
    val alpha by animateFloatAsState(
        targetValue = if (active && entries.size > 40) 1f else 0f,
        animationSpec = helixEffectsSpec(),
        label = "scrubberAlpha",
    )
    val total = entries.size
    val density = LocalDensity.current

    BoxWithConstraints(modifier.fillMaxHeight().width(40.dp)) {
        val heightPx = with(density) { maxHeight.toPx() }
        val thumbPx = with(density) { 48.dp.toPx() }
        val index = state.firstVisibleItemIndex.coerceIn(0, (total - 1).coerceAtLeast(0))
        val fraction = if (total <= 1) 0f else index.toFloat() / (total - 1)
        val offsetY = (fraction * (heightPx - thumbPx)).roundToInt()

        fun jumpTo(y: Float) {
            if (total <= 1) return
            val f = ((y - thumbPx / 2f) / (heightPx - thumbPx)).coerceIn(0f, 1f)
            val target = (f * (total - 1)).roundToInt()
            scope.launch { state.scrollToItem(target) }
        }

        val gesture = if (alpha > 0.05f) {
            Modifier.pointerInput(total) {
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        dragging = true
                        jumpTo(offset.y)
                    },
                    onDragEnd = { dragging = false },
                    onDragCancel = { dragging = false },
                    onVerticalDrag = { change, _ ->
                        change.consume()
                        jumpTo(change.position.y)
                    },
                )
            }
        } else {
            Modifier
        }

        Box(
            Modifier
                .fillMaxHeight()
                .width(40.dp)
                .alpha(alpha)
                .then(gesture),
        ) {
            Box(
                Modifier
                    .offset { IntOffset(0, offsetY) }
                    .align(Alignment.TopEnd)
                    .padding(end = 4.dp)
                    .width(8.dp)
                    .size(width = 8.dp, height = 48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
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
