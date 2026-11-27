// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.features.photos

import com.helixgallery.core.designsystem.theme.helixScreenSpec
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
import com.helixgallery.core.designsystem.component.HelixConfirmPage
import com.helixgallery.core.designsystem.component.HelixConfirmSheet
import com.helixgallery.core.designsystem.component.HelixSelectionActionsBar
import com.helixgallery.core.designsystem.theme.helixEffectsSpec
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
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
import com.helixgallery.core.designsystem.component.GridDatePill
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
 * @param onOpenSettings só na aba Fotos: o atalho dos Ajustes (a aba não tem título; no lugar vão as lembranças).
 * @param onOpenStory só na aba Fotos: abre a lista de um cartão de lembrança.
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
    onOpenStory: ((MediaSource) -> Unit)? = null,
    viewModel: CollectionViewModel = viewModel(key = "collection_${source.key}"),
) {
    val context = LocalContext.current
    val mediaState by viewModel.media.collectAsStateWithLifecycle()
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selected.collectAsStateWithLifecycle()

    val items = remember(source, mediaState) { selectMedia(source, mediaState) }
    // A aba Fotos é uma grade corrida, sem títulos de data nem bolinhas (a data aparece na pílula da rolagem rápida);
    // álbuns e coleções seguem o layout escolhido (Regular/Dia/Mês).
    val isTab = onBack == null && onOpenSettings != null
    val entries = remember(items, prefs.gridLayout, isTab) { buildEntries(items, if (isTab) GridLayout.Regular else prefs.gridLayout) }
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
    var scrubbing by remember { mutableStateOf(false) }
    // O app confirma antes de apagar (no lugar da caixa do sistema).
    var confirm by remember { mutableStateOf<Confirm?>(null) }

    // A aba Fotos não tem título: no topo ficam a faixa dos Ajustes e a fileira de lembranças.
    val stories = if (isTab && onOpenStory != null) viewModel.stories.collectAsStateWithLifecycle().value else emptyList()
    if (isTab) LifecycleResumeEffect(Unit) {
        viewModel.refreshToday()
        onPauseOrDispose { }
    }
    // Os botões (Ajustes, barra da seleção) flutuam sobre a página: a fileira de lembranças fica onde está ao
    // selecionar, só sem toque. O recuo é do título das telas de dentro.
    val titleRow: @Composable () -> Unit = {
        if (isTab) {
            Column(Modifier.statusBarsPadding()) {
                Spacer(Modifier.height(64.dp))
                if (stories.isNotEmpty() && onOpenStory != null) {
                    StoriesRow(stories, onOpenStory, enabled = !selecting)
                    Spacer(Modifier.height(12.dp))
                }
            }
        } else {
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
    // Esvaziar a lixeira confirma numa página do mesmo painel do menu (com voltar), não em painel por cima.
    val trashMenu = listOf(
        HelixMenuAction(
            stringResource(R.string.photos_empty_trash_action),
            Icons.Filled.Delete,
            destructive = true,
            page = {
                HelixConfirmPage(
                    title = stringResource(R.string.photos_confirm_empty_title),
                    message = pluralStringResource(R.plurals.photos_confirm_empty_message, items.size, items.size),
                    confirmLabel = stringResource(R.string.photos_empty_trash_action),
                    onConfirm = { launch(viewModel.repository.deleteRequest(items.map { it.uri })) },
                )
            },
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
                    onZoomStep = { out -> viewModel.zoomStep(out, regularOnly = isTab) },
                    roundedEnds = isTab,
                )
            }
        }
        HelixStatusBarScrim(Modifier.align(Alignment.TopStart))
        if (isTab && mediaState.loaded && items.isNotEmpty()) {
            GridDatePill(
                state = gridState,
                entries = entries,
                headerCount = 1,
                enabled = !selecting,
                forceShown = scrubbing,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
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
            enter = slideInVertically(helixScreenSpec()) { it } + fadeIn(helixEffectsSpec()),
            exit = slideOutVertically(helixScreenSpec()) { it } + fadeOut(helixEffectsSpec()),
        ) {
            HelixBottomActionBar(
                primaryLabel = stringResource(R.string.photos_restore),
                onPrimary = { launch(viewModel.repository.trashRequest(selectedItems.map { it.uri }, false)) },
                destructiveLabel = stringResource(R.string.photos_delete_trash),
                onDestructive = { confirm = Confirm.DeleteForever },
            )
        }
        // Fora da lixeira, as ações da seleção ficam embaixo (lista horizontal), não num ⋮ lá em cima.
        AnimatedVisibility(
            visible = selecting && !inTrash,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(helixScreenSpec()) { it } + fadeIn(helixEffectsSpec()),
            exit = slideOutVertically(helixScreenSpec()) { it } + fadeOut(helixEffectsSpec()),
        ) {
            HelixSelectionActionsBar(actions = selectionActions)
        }
        HelixSelectionBar(
            visible = selecting,
            count = selectedIds.size,
            allSelected = items.isNotEmpty() && selectedIds.containsAll(items.map { it.id }),
            onClose = viewModel::clearSelection,
            onToggleAll = { all -> if (all) viewModel.selectAll(items.map { it.id }) else viewModel.clearSelection() },
            actions = emptyList(),
            modifier = Modifier.align(Alignment.TopCenter),
        )
        // A rolagem rápida fica por cima das barras da seleção e deixa livre o espaço delas (em cima e embaixo).
        if (mediaState.loaded && items.isNotEmpty()) {
            val scrubTop by animateDpAsState(if (selecting) 144.dp else 64.dp, helixSpatialSpec(), label = "scrubTop")
            val scrubBottom by animateDpAsState(if (selecting) 112.dp else 0.dp, helixSpatialSpec(), label = "scrubBottom")
            GridScrubber(
                state = gridState,
                entries = entries,
                columns = prefs.columns,
                headerCount = 1,
                showBubble = !isTab,
                onDraggingChange = { scrubbing = it },
                modifier = Modifier.align(Alignment.CenterEnd).statusBarsPadding().padding(top = scrubTop.coerceAtLeast(0.dp), bottom = scrubBottom.coerceAtLeast(0.dp)),
            )
        }
    }

    confirm?.let { kind ->
        val count = selectedItems.size
        HelixConfirmSheet(
            title = when (kind) {
                Confirm.Trash -> pluralStringResource(R.plurals.photos_confirm_trash_title, count, count)
                Confirm.DeleteForever -> pluralStringResource(R.plurals.photos_confirm_delete_title, count, count)
            },
            message = when (kind) {
                Confirm.Trash -> stringResource(R.string.photos_confirm_trash_message)
                Confirm.DeleteForever -> stringResource(R.string.photos_confirm_delete_message)
            },
            confirmLabel = stringResource(
                when (kind) {
                    Confirm.Trash -> R.string.photos_trash
                    Confirm.DeleteForever -> R.string.photos_delete
                },
            ),
            onConfirm = {
                when (kind) {
                    Confirm.Trash -> launch(viewModel.repository.trashRequest(selectedItems.map { it.uri }, true))
                    Confirm.DeleteForever -> launch(viewModel.repository.deleteRequest(selectedItems.map { it.uri }))
                }
            },
            onDismiss = { confirm = null },
        )
    }
}

private enum class Confirm { Trash, DeleteForever }

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
