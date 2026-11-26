// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.features.photos

import android.app.Activity
import android.content.IntentSender
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.helixgallery.core.designsystem.theme.helixSpatialSpec
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import com.helixgallery.core.designsystem.component.recedeWhileSelecting
import com.helixgallery.core.designsystem.component.HelixSelectionBar
import com.helixgallery.core.designsystem.component.HelixBottomActionBar
import com.helixgallery.core.designsystem.component.HelixConfirmSheet
import com.helixgallery.core.designsystem.theme.helixEffectsSpec
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.helixgallery.core.designsystem.component.HelixFloatingActions
import com.helixgallery.core.designsystem.component.HelixFloatingCircle
import com.helixgallery.core.designsystem.component.helixActionsInset
import com.helixgallery.core.designsystem.component.ScrollToTopOnReselect
import com.helixgallery.core.designsystem.component.FLOATING_BUTTON_ALPHA
import com.helixgallery.core.designsystem.component.GridScrubber
import com.helixgallery.core.designsystem.component.HelixFloatingBackButton
import com.helixgallery.core.designsystem.component.HelixIcons
import com.helixgallery.core.designsystem.component.HelixMenuAction
import com.helixgallery.core.designsystem.component.HelixMoreMenu
import com.helixgallery.core.designsystem.component.HelixPageTitle
import com.helixgallery.core.designsystem.component.HelixStatusBarScrim
import com.helixgallery.core.designsystem.component.HelixLoading
import com.helixgallery.core.designsystem.component.MediaGrid
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
 * @param onOpenSettings só na aba Fotos: o atalho dos Ajustes ao lado do título.
 * @param bottomInset espaço para a barra flutuante, quando ela está por cima.
 */
@Composable
fun CollectionScreen(
    source: MediaSource,
    title: String?,
    onOpen: (source: MediaSource, mediaId: Long) -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
    bottomInset: Boolean = false,
    reselectTab: String? = null,
    onMoveToVault: ((List<MediaItem>) -> Unit)? = null,
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
    if (reselectTab != null) ScrollToTopOnReselect(reselectTab, gridState)
    val inTrash = source == MediaSource.Trash

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) viewModel.clearSelection()
    }
    fun launch(sender: IntentSender) = launcher.launch(IntentSenderRequest.Builder(sender).build())

    BackHandler(enabled = selectedIds.isNotEmpty()) { viewModel.clearSelection() }

    val pageTitle = title ?: items.firstOrNull()?.bucketName.orEmpty()
    val selecting = selectedIds.isNotEmpty()
    // O app confirma antes de apagar (no lugar da caixa do sistema).
    var confirm by remember { mutableStateOf<Confirm?>(null) }

    val titleRow: @Composable () -> Unit = {
        Box(Modifier.recedeWhileSelecting(selecting)) {
            HelixPageTitle(
                title = pageTitle,
                hasBack = onBack != null,
                actionsInset = helixActionsInset(
                    (if (onOpenSettings != null) 1 else 0) + (if (inTrash && items.isNotEmpty()) 1 else 0),
                ),
            )
        }
    }
    val selectionActions = buildList {
        if (!inTrash) {
            add(
                HelixMenuAction(
                    stringResource(R.string.photos_share),
                    Icons.Filled.Share,
                    onClick = {
                        context.shareMedia(
                            uris = selectedItems.map { it.uri },
                            mimeTypes = selectedItems.map { it.mimeType },
                            title = context.getString(R.string.photos_share),
                        )
                    },
                ),
            )
            val allFavorite = selectedItems.isNotEmpty() && selectedItems.all { it.isFavorite }
            add(
                HelixMenuAction(
                    stringResource(if (allFavorite) R.string.photos_unfavorite else R.string.photos_favorite),
                    if (allFavorite) Icons.Filled.FavoriteBorder else Icons.Filled.Favorite,
                    onClick = { launch(viewModel.repository.favoriteRequest(selectedItems.map { it.uri }, !allFavorite)) },
                ),
            )
            if (onMoveToVault != null) {
                add(
                    HelixMenuAction(
                        stringResource(R.string.photos_move_to_vault),
                        HelixIcons.Lock,
                        onClick = {
                            val chosen = selectedItems
                            viewModel.lockToVault(chosen) { stored, failed ->
                                when {
                                    stored == null -> {
                                        // Cofre ainda sem senha guardada: segue o caminho normal (abre o cofre).
                                        onMoveToVault(chosen)
                                        viewModel.clearSelection()
                                    }
                                    stored.isEmpty() -> Toast.makeText(context, R.string.photos_lock_failed, Toast.LENGTH_SHORT).show()
                                    else -> {
                                        Toast.makeText(
                                            context,
                                            context.resources.getQuantityString(R.plurals.photos_locked, stored.size, stored.size),
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                        if (failed > 0) Toast.makeText(context, R.string.photos_lock_failed, Toast.LENGTH_SHORT).show()
                                        viewModel.clearSelection()
                                        launch(viewModel.repository.deleteRequest(stored))
                                    }
                                }
                            }
                        },
                    ),
                )
            }
            add(
                HelixMenuAction(
                    stringResource(R.string.photos_trash),
                    Icons.Filled.Delete,
                    destructive = true,
                    onClick = { confirm = Confirm.Trash },
                ),
            )
        }
        // Na lixeira, Restaurar e Excluir ficam na barra de baixo (abaixo), não no ⋮.
    }
    val trashMenu = listOf(
        HelixMenuAction(
            stringResource(R.string.photos_empty_trash_action),
            Icons.Filled.Delete,
            destructive = true,
            onClick = { confirm = Confirm.EmptyTrash },
        ),
    )

    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        when {
            !mediaState.loaded -> Column(Modifier.fillMaxSize()) {
                titleRow()
                HelixLoading()
            }
            items.isEmpty() -> Column(Modifier.fillMaxSize()) {
                titleRow()
                EmptyState(inTrash)
            }
            else -> {
                MediaGrid(
                    entries = entries,
                    columns = prefs.columns,
                    selectedIds = selectedIds,
                    onOpen = { onOpen(source, it.id) },
                    onToggleSelect = { viewModel.toggle(it.id) },
                    state = gridState,
                    contentPadding = PaddingValues(bottom = if (bottomInset) 112.dp else 16.dp),
                    header = titleRow,
                    onZoomStep = viewModel::zoomStep,
                )
                GridScrubber(
                    state = gridState,
                    entries = entries,
                    columns = prefs.columns,
                    headerCount = 1,
                    modifier = Modifier.align(Alignment.CenterEnd).statusBarsPadding().padding(top = 64.dp),
                )
            }
        }
        HelixStatusBarScrim(Modifier.align(Alignment.TopStart))
        // Ajustes (e o ⋮ da lixeira) flutuam no canto, como o voltar; com seleção, a barra do Boom toma o lugar.
        AnimatedVisibility(
            visible = !selecting,
            modifier = Modifier.align(Alignment.TopEnd),
            enter = fadeIn(helixEffectsSpec()),
            exit = fadeOut(helixEffectsSpec()),
        ) {
            HelixFloatingActions {
                if (onOpenSettings != null) {
                    HelixFloatingCircle {
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.photos_settings))
                        }
                    }
                }
                if (inTrash && items.isNotEmpty()) HelixMoreMenu(actions = trashMenu)
            }
        }
        if (onBack != null) {
            AnimatedVisibility(
                visible = !selecting,
                modifier = Modifier.align(Alignment.TopStart),
                enter = fadeIn(helixEffectsSpec()),
                exit = fadeOut(helixEffectsSpec()),
            ) {
                HelixFloatingBackButton(onClick = onBack)
            }
        }
        AnimatedVisibility(
            visible = selecting && inTrash,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(helixSpatialSpec()) { it } + fadeIn(helixEffectsSpec()),
            exit = slideOutVertically(helixSpatialSpec()) { it } + fadeOut(helixEffectsSpec()),
        ) {
            HelixBottomActionBar(
                primaryLabel = stringResource(R.string.photos_restore),
                onPrimary = { launch(viewModel.repository.trashRequest(selectedItems.map { it.uri }, false)) },
                destructiveLabel = stringResource(R.string.photos_delete_trash),
                onDestructive = { confirm = Confirm.DeleteForever },
            )
        }
        HelixSelectionBar(
            visible = selecting,
            count = selectedIds.size,
            allSelected = items.isNotEmpty() && selectedIds.containsAll(items.map { it.id }),
            onClose = viewModel::clearSelection,
            onToggleAll = { all -> if (all) viewModel.selectAll(items.map { it.id }) else viewModel.clearSelection() },
            actions = selectionActions,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }

    confirm?.let { kind ->
        val count = if (kind == Confirm.EmptyTrash) items.size else selectedItems.size
        HelixConfirmSheet(
            title = when (kind) {
                Confirm.Trash -> pluralStringResource(R.plurals.photos_confirm_trash_title, count, count)
                Confirm.DeleteForever -> pluralStringResource(R.plurals.photos_confirm_delete_title, count, count)
                Confirm.EmptyTrash -> stringResource(R.string.photos_confirm_empty_title)
            },
            message = when (kind) {
                Confirm.Trash -> stringResource(R.string.photos_confirm_trash_message)
                Confirm.DeleteForever -> stringResource(R.string.photos_confirm_delete_message)
                Confirm.EmptyTrash -> pluralStringResource(R.plurals.photos_confirm_empty_message, count, count)
            },
            confirmLabel = stringResource(
                when (kind) {
                    Confirm.Trash -> R.string.photos_trash
                    Confirm.DeleteForever -> R.string.photos_delete
                    Confirm.EmptyTrash -> R.string.photos_empty_trash_action
                },
            ),
            onConfirm = {
                when (kind) {
                    Confirm.Trash -> launch(viewModel.repository.trashRequest(selectedItems.map { it.uri }, true))
                    Confirm.DeleteForever -> launch(viewModel.repository.deleteRequest(selectedItems.map { it.uri }))
                    Confirm.EmptyTrash -> launch(viewModel.repository.deleteRequest(items.map { it.uri }))
                }
            },
            onDismiss = { confirm = null },
        )
    }
}

private enum class Confirm { Trash, DeleteForever, EmptyTrash }

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
