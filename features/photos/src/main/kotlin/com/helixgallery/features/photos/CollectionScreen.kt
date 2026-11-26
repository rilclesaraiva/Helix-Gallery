// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.features.photos

import android.app.Activity
import android.content.IntentSender
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.helixgallery.core.common.shareMedia
import com.helixgallery.core.designsystem.component.GridScrubber
import com.helixgallery.core.designsystem.component.HelixLoading
import com.helixgallery.core.designsystem.component.MediaGrid
import com.helixgallery.core.model.GRID_COLUMN_OPTIONS
import com.helixgallery.core.model.GridLayout
import com.helixgallery.core.model.MediaItem
import com.helixgallery.core.model.MediaSource
import com.helixgallery.core.model.buildEntries
import com.helixgallery.core.model.selectMedia

/**
 * Lista de fotos e vídeos de uma origem: a aba Fotos (todas) ou um álbum, favoritos,
 * vídeos, capturas de tela, lixeira. Cuida da grade, da seleção e das ações.
 *
 * @param onOpen abre o visualizador na foto tocada, dentro da mesma lista.
 * @param onBack só nas telas de dentro (álbum, coleção); a aba Fotos não tem.
 * @param bottomInset espaço para a barra flutuante, quando ela está por cima.
 */
@Composable
fun CollectionScreen(
    source: MediaSource,
    title: String?,
    onOpen: (source: MediaSource, mediaId: Long) -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    bottomInset: Boolean = false,
    viewModel: CollectionViewModel = viewModel(key = "collection_${source.key}"),
) {
    val context = LocalContext.current
    val mediaState by viewModel.media.collectAsStateWithLifecycle()
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selected.collectAsStateWithLifecycle()

    val items = remember(source, mediaState) { selectMedia(source, mediaState) }
    val entries = remember(items, prefs.gridLayout) { buildEntries(items, prefs.gridLayout) }
    val selectedItems = remember(items, selectedIds) { items.filter { it.id in selectedIds } }
    val gridState = rememberLazyGridState()
    val inTrash = source == MediaSource.Trash

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) viewModel.clearSelection()
    }
    fun launch(sender: IntentSender) = launcher.launch(IntentSenderRequest.Builder(sender).build())

    BackHandler(enabled = selectedIds.isNotEmpty()) { viewModel.clearSelection() }

    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxSize()) {
            if (selectedIds.isNotEmpty()) {
                SelectionBar(
                    count = selectedIds.size,
                    allFavorite = selectedItems.isNotEmpty() && selectedItems.all { it.isFavorite },
                    inTrash = inTrash,
                    onClose = viewModel::clearSelection,
                    onShare = {
                        context.shareMedia(
                            uris = selectedItems.map { it.uri },
                            mimeTypes = selectedItems.map { it.mimeType },
                            title = context.getString(R.string.photos_share),
                        )
                    },
                    onFavorite = { favorite ->
                        launch(viewModel.repository.favoriteRequest(selectedItems.map { it.uri }, favorite))
                    },
                    onTrash = {
                        launch(viewModel.repository.trashRequest(selectedItems.map { it.uri }, !inTrash))
                    },
                    onDelete = {
                        launch(viewModel.repository.deleteRequest(selectedItems.map { it.uri }))
                    },
                )
            } else {
                TitleBar(
                    title = title ?: items.firstOrNull()?.bucketName.orEmpty(),
                    onBack = onBack,
                    layout = prefs.gridLayout,
                    columns = prefs.columns,
                    onLayout = viewModel::setLayout,
                    onColumns = viewModel::setColumns,
                )
            }
            Box(Modifier.weight(1f)) {
                when {
                    !mediaState.loaded -> HelixLoading()
                    items.isEmpty() -> EmptyState(inTrash)
                    else -> {
                        MediaGrid(
                            entries = entries,
                            columns = prefs.columns,
                            selectedIds = selectedIds,
                            onOpen = { onOpen(source, it.id) },
                            onToggleSelect = { viewModel.toggle(it.id) },
                            state = gridState,
                            contentPadding = PaddingValues(bottom = if (bottomInset) 112.dp else 16.dp),
                        )
                        GridScrubber(
                            state = gridState,
                            entries = entries,
                            modifier = Modifier.align(Alignment.CenterEnd),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TitleBar(
    title: String,
    onBack: (() -> Unit)?,
    layout: GridLayout,
    columns: Int,
    onLayout: (GridLayout) -> Unit,
    onColumns: (Int) -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.photos_back))
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            maxLines = 1,
        )
        Box {
            IconButton(onClick = { menu = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.photos_view_options))
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                val layouts = listOf(
                    GridLayout.Regular to R.string.photos_layout_regular,
                    GridLayout.Day to R.string.photos_layout_day,
                    GridLayout.Month to R.string.photos_layout_month,
                )
                layouts.forEach { (value, label) ->
                    DropdownMenuItem(
                        text = { Text(stringResource(label)) },
                        leadingIcon = { if (layout == value) Icon(Icons.Filled.Check, contentDescription = null) },
                        onClick = {
                            onLayout(value)
                            menu = false
                        },
                    )
                }
                GRID_COLUMN_OPTIONS.forEach { value ->
                    DropdownMenuItem(
                        text = { Text(pluralStringResource(R.plurals.photos_columns, value, value)) },
                        leadingIcon = { if (columns == value) Icon(Icons.Filled.Check, contentDescription = null) },
                        onClick = {
                            onColumns(value)
                            menu = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectionBar(
    count: Int,
    allFavorite: Boolean,
    inTrash: Boolean,
    onClose: () -> Unit,
    onShare: () -> Unit,
    onFavorite: (Boolean) -> Unit,
    onTrash: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        IconButton(onClick = onClose) {
            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.photos_clear_selection))
        }
        Text(
            text = pluralStringResource(R.plurals.photos_selected, count, count),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (!inTrash) {
            IconButton(onClick = onShare) {
                Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.photos_share))
            }
            IconButton(onClick = { onFavorite(!allFavorite) }) {
                Icon(
                    if (allFavorite) Icons.Filled.FavoriteBorder else Icons.Filled.Favorite,
                    contentDescription = stringResource(if (allFavorite) R.string.photos_unfavorite else R.string.photos_favorite),
                )
            }
        }
        IconButton(onClick = onTrash) {
            Icon(
                if (inTrash) Icons.Filled.Refresh else Icons.Filled.Delete,
                contentDescription = stringResource(if (inTrash) R.string.photos_restore else R.string.photos_trash),
            )
        }
        if (inTrash) {
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.photos_delete_forever))
            }
        }
    }
}

@Composable
private fun EmptyState(inTrash: Boolean) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(if (inTrash) R.string.photos_empty_trash else R.string.photos_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
