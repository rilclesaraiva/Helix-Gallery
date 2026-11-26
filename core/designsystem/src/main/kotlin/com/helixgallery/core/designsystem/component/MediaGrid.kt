// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import com.helixgallery.core.designsystem.theme.helixEffectsSpec
import com.helixgallery.core.designsystem.theme.helixSpatialSpec
import androidx.compose.ui.unit.IntOffset
import com.helixgallery.core.designsystem.R
import com.helixgallery.core.designsystem.theme.helixFastSpatialSpec
import com.helixgallery.core.model.GridEntry
import com.helixgallery.core.model.GridLayout
import com.helixgallery.core.model.MediaItem
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** Título de uma seção da grade ("Seg, 3 de nov." ou "novembro de 2026"), no idioma do aparelho. */
@Composable
fun rememberHeaderText(header: GridEntry.Header): String {
    val locale = currentLocale()
    return remember(header, locale) { formatHeader(header, locale) }
}

@Composable
internal fun currentLocale(): Locale {
    val config = LocalConfiguration.current
    return remember(config) { config.locales[0] ?: Locale.getDefault() }
}

internal fun formatHeader(header: GridEntry.Header, locale: Locale): String {
    val date = LocalDate.ofEpochDay(header.epochDay)
    val text = when (header.layout) {
        GridLayout.Month -> date.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale))
        else -> date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale))
    }
    return text.replaceFirstChar { it.titlecase(locale) }
}

/**
 * Grade de fotos e vídeos. Os títulos de dia/mês ocupam a linha toda; quando há
 * seleção, tocar numa foto marca ou desmarca em vez de abrir.
 */
@Composable
fun MediaGrid(
    entries: List<GridEntry>,
    columns: Int,
    selectedIds: Set<Long>,
    onOpen: (MediaItem) -> Unit,
    onToggleSelect: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
    state: LazyGridState = rememberLazyGridState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    /** Primeira linha da grade, que rola junto (o título da tela). Quem usa o [GridScrubber] passa `headerCount = 1`. */
    header: (@Composable () -> Unit)? = null,
    /** Pinça sobre a grade: `zoomOut` true quando os dedos se juntam. null = sem pinça. */
    onZoomStep: ((zoomOut: Boolean) -> Unit)? = null,
) {
    val selecting = selectedIds.isNotEmpty()
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val currentSelected by rememberUpdatedState(selectedIds)
    val currentEntries by rememberUpdatedState(entries)
    val currentOnToggle: (Long) -> Unit = { id ->
        currentEntries.firstNotNullOfOrNull { (it as? GridEntry.Media)?.item?.takeIf { m -> m.id == id } }?.let(onToggleSelect)
    }
    // Ao pinçar, as fotos deslizam para o novo lugar (em vez de pular).
    val placementSpec = helixSpatialSpec<IntOffset>()
    val fadeSpec = helixEffectsSpec<Float>()
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        state = state,
        modifier = (if (onZoomStep != null) modifier.pinchToZoom(onZoomStep) else modifier)
            .fillMaxSize()
            .pointerInput(state) {
                // Segurar numa foto marca; sem soltar, deslizar o dedo por cima das outras marca elas também.
                val visited = mutableSetOf<Long>()
                val edge = 88.dp.toPx()
                fun itemAt(position: Offset): Long? {
                    val info = state.layoutInfo.visibleItemsInfo.firstOrNull { item ->
                        position.x >= item.offset.x && position.x < item.offset.x + item.size.width &&
                            position.y >= item.offset.y && position.y < item.offset.y + item.size.height
                    }
                    val key = info?.key as? String ?: return null
                    return if (key.startsWith("m")) key.substring(1).toLongOrNull() else null
                }
                detectDragGesturesAfterLongPress(
                    onDragStart = { position ->
                        visited.clear()
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        itemAt(position)?.let { id ->
                            visited.add(id)
                            currentOnToggle(id)
                        }
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        itemAt(change.position)?.let { id ->
                            if (visited.add(id) && id !in currentSelected) currentOnToggle(id)
                        }
                        // Perto da borda de cima ou de baixo, a grade rola sozinha.
                        val height = state.layoutInfo.viewportSize.height
                        val push = when {
                            change.position.y < edge -> -EDGE_SCROLL_PX
                            change.position.y > height - edge -> EDGE_SCROLL_PX
                            else -> 0f
                        }
                        if (push != 0f) scope.launch { state.scrollBy(push) }
                    },
                    onDragEnd = { visited.clear() },
                    onDragCancel = { visited.clear() },
                )
            },
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (header != null) {
            item(key = "page-header", span = { GridItemSpan(maxLineSpan) }, contentType = "page-header") { header() }
        }
        items(
            items = entries,
            key = { entry ->
                when (entry) {
                    is GridEntry.Header -> "h${entry.epochDay}"
                    is GridEntry.Media -> "m${entry.item.id}"
                }
            },
            span = { entry ->
                when (entry) {
                    is GridEntry.Header -> GridItemSpan(maxLineSpan)
                    is GridEntry.Media -> GridItemSpan(1)
                }
            },
            contentType = { entry ->
                when (entry) {
                    is GridEntry.Header -> 0
                    is GridEntry.Media -> 1
                }
            },
        ) { entry ->
            when (entry) {
                is GridEntry.Header -> Text(
                    text = rememberHeaderText(entry),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.animateItem(placementSpec = placementSpec, fadeInSpec = fadeSpec, fadeOutSpec = fadeSpec)
                        .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
                )
                is GridEntry.Media -> MediaTile(
                    modifier = Modifier.animateItem(placementSpec = placementSpec, fadeInSpec = fadeSpec, fadeOutSpec = fadeSpec),
                    item = entry.item,
                    selected = entry.item.id in selectedIds,
                    selecting = selecting,
                    onClick = {
                        if (selecting) onToggleSelect(entry.item) else onOpen(entry.item)
                    },
                )
            }
        }
    }
}

private const val EDGE_SCROLL_PX = 28f

/** Uma foto ou vídeo na grade, com marcas de vídeo, favorito e seleção. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaTile(
    item: MediaItem,
    selected: Boolean,
    selecting: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 0.84f else 1f,
        animationSpec = helixFastSpatialSpec(),
        label = "tileScale",
    )
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        HelixImage(
            uri = item.uri,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(if (selected) RoundedCornerShape(16.dp) else RoundedCornerShape(0.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        )
        if (item.isVideo) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = stringResource(R.string.ds_video),
                    tint = Color.White,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = formatDuration(item.durationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                )
            }
        }
        if (item.isFavorite && !selecting) {
            Icon(
                Icons.Filled.Favorite,
                contentDescription = stringResource(R.string.ds_favorite),
                tint = Color.White,
                modifier = Modifier.align(Alignment.BottomStart).padding(6.dp).size(16.dp),
            )
        }
        if (selecting) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (selected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.35f)),
            ) {
                if (selected) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = stringResource(R.string.ds_selected),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

/** 75 000 ms → "1:15"; mais de uma hora → "1:02:03". */
fun formatDuration(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
