// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.core.designsystem.component

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Segurar e deslizar marca todo o intervalo (na ordem da grade) entre o item onde o dedo começou e o que
 * está sob ele, como no Google Fotos; voltar desmarca. Mesmo comportamento da [MediaGrid], para grades com
 * chaves de texto (o cofre). [order] devolve as chaves na ordem da grade; [suppressClick] é o mesmo
 * truque da galeria: soltar o dedo depois de segurar não conta como toque.
 */
@androidx.compose.runtime.Composable
fun Modifier.dragSelect(
    state: LazyGridState,
    order: () -> List<String>,
    selected: () -> Set<String>,
    onToggle: (String) -> Unit,
    suppressClick: MutableState<Boolean>,
): Modifier {
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val currentOrder by rememberUpdatedState(order)
    val currentSelected by rememberUpdatedState(selected)
    val currentToggle by rememberUpdatedState(onToggle)
    return this
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                suppressClick.value = false
            }
        }
        .pointerInput(state) {
            val edge = 88.dp.toPx()
            var keys: List<String> = emptyList()
            var anchor = -1
            var base: Set<String> = emptySet()
            var applied: Set<String> = emptySet()
            fun itemAt(position: Offset): String? {
                val info = state.layoutInfo.visibleItemsInfo.firstOrNull { item ->
                    position.x >= item.offset.x && position.x < item.offset.x + item.size.width &&
                        position.y >= item.offset.y && position.y < item.offset.y + item.size.height
                }
                return (info?.key as? String)?.takeIf { it in keys }
            }
            fun applyRange(currentIndex: Int) {
                val from = minOf(anchor, currentIndex)
                val to = maxOf(anchor, currentIndex)
                val wanted = keys.subList(from, to + 1).filter { it !in base }.toSet()
                (wanted - applied).forEach(currentToggle)
                (applied - wanted).forEach(currentToggle)
                applied = wanted
            }
            detectDragGesturesAfterLongPress(
                onDragStart = { position ->
                    suppressClick.value = true
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    keys = currentOrder()
                    base = currentSelected()
                    applied = emptySet()
                    anchor = itemAt(position)?.let { keys.indexOf(it) } ?: -1
                    if (anchor >= 0) applyRange(anchor)
                },
                onDrag = { change, _ ->
                    change.consume()
                    if (anchor >= 0) {
                        itemAt(change.position)?.let { key ->
                            val index = keys.indexOf(key)
                            if (index >= 0) applyRange(index)
                        }
                    }
                    val height = state.layoutInfo.viewportSize.height
                    val push = when {
                        change.position.y < edge -> -28f
                        change.position.y > height - edge -> 28f
                        else -> 0f
                    }
                    if (push != 0f) scope.launch { state.scrollBy(push) }
                },
                onDragEnd = { applied = emptySet() },
                onDragCancel = { applied = emptySet() },
            )
        }
}
