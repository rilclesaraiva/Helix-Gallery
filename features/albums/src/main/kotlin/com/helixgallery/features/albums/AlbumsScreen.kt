// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.features.albums

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import com.helixgallery.core.designsystem.component.ScrollToTopOnReselect
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.helixgallery.core.designsystem.component.HelixModalSheet
import com.helixgallery.core.model.AlbumTool
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.helixgallery.core.designsystem.component.HelixIcons
import com.helixgallery.core.designsystem.component.HelixImage
import com.helixgallery.core.designsystem.component.HelixLoading
import com.helixgallery.core.designsystem.component.HelixFloatingActions
import com.helixgallery.core.designsystem.component.HelixFloatingCircle
import com.helixgallery.core.designsystem.component.HelixStatusBarScrim
import com.helixgallery.core.model.MediaSource
import com.helixgallery.core.model.buildAlbums
import com.helixgallery.core.model.isScreenshot

/** Aba Álbuns: coleções prontas (favoritos, vídeos, capturas, lixeira) e as pastas do aparelho. */
@Composable
fun AlbumsScreen(
    onOpenCollection: (MediaSource) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenVault: () -> Unit,
    onOpenCleanup: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AlbumsViewModel = viewModel(),
) {
    val state by viewModel.media.collectAsStateWithLifecycle()
    val albums = remember(state.items) { buildAlbums(state.items) }
    val favorites = remember(state.items) { state.items.count { it.isFavorite } }
    val videos = remember(state.items) { state.items.count { it.isVideo } }
    val screenshots = remember(state.items) { state.items.count { it.isScreenshot() } }
    val tools by viewModel.tools.collectAsStateWithLifecycle()
    var editTools by remember { mutableStateOf(false) }

    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxSize()) {
        val titleRow: @Composable () -> Unit = {
            // Sem título: só a faixa livre para a barra de status e o atalho dos Ajustes.
            Spacer(Modifier.statusBarsPadding().height(64.dp))
        }
        if (!state.loaded) {
            titleRow()
            HelixLoading()
        } else {
            val gridState = rememberLazyGridState()
            ScrollToTopOnReselect("tab/albums", gridState)
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 120.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    // O título faz parte da página (rola junto); sem as margens da grade.
                    Box(Modifier.layout { m, c ->
                        val extra = 16.dp.roundToPx() * 2
                        val p = m.measure(c.copy(maxWidth = c.maxWidth + extra))
                        layout(c.maxWidth, p.height) { p.place(-extra / 2, 0) }
                    }) { titleRow() }
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    // Atalhos compactos em uma linha que rola para o lado (como os "Fixados" do Google Fotos).
                    Row(
                        Modifier.layout { m, c ->
                            val extra = 16.dp.roundToPx() * 2
                            val p = m.measure(c.copy(maxWidth = c.maxWidth + extra))
                            layout(c.maxWidth, p.height) { p.place(-extra / 2, 0) }
                        }.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        tools.forEach { tool ->
                            val info = toolInfo(tool, favorites, videos, screenshots, state.trashed.size)
                            ShortcutTile(info.icon, stringResource(info.title), info.count) {
                                when (tool) {
                                    AlbumTool.Favorites -> onOpenCollection(MediaSource.Favorites)
                                    AlbumTool.Videos -> onOpenCollection(MediaSource.Videos)
                                    AlbumTool.Screenshots -> onOpenCollection(MediaSource.Screenshots)
                                    AlbumTool.Trash -> onOpenCollection(MediaSource.Trash)
                                    AlbumTool.Vault -> onOpenVault()
                                    AlbumTool.Cleanup -> onOpenCleanup()
                                }
                            }
                        }
                        EditToolsTile { editTools = true }
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = stringResource(R.string.albums_on_device),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp, top = 12.dp),
                    )
                }
                items(albums, key = { it.bucketId }) { album ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = MaterialTheme.shapes.extraLarge,
                        modifier = Modifier.clip(MaterialTheme.shapes.extraLarge).clickable {
                            onOpenCollection(MediaSource.Album(album.bucketId))
                        },
                    ) {
                        Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            HelixImage(
                                uri = album.cover.uri,
                                modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(MaterialTheme.shapes.large),
                            )
                            Text(
                                text = album.name,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                            Text(
                                text = pluralStringResource(R.plurals.albums_items, album.count, album.count),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 4.dp),
                            )
                        }
                    }
                }
            }
        }
        }
        if (editTools) {
            EditToolsSheet(
                tools = tools,
                onUnpin = viewModel::unpin,
                onPin = viewModel::pin,
                onMove = viewModel::move,
                onDismiss = { editTools = false },
            )
        }
        HelixStatusBarScrim(Modifier.align(Alignment.TopStart))
        HelixFloatingActions(Modifier.align(Alignment.TopEnd)) {
            HelixFloatingCircle {
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.albums_settings))
                }
            }
        }
    }
}

private class ToolInfo(val icon: ImageVector, val title: Int, val count: Int?)

private fun toolInfo(tool: AlbumTool, favorites: Int, videos: Int, screenshots: Int, trashed: Int) = when (tool) {
    AlbumTool.Favorites -> ToolInfo(Icons.Filled.Favorite, R.string.albums_favorites, favorites)
    AlbumTool.Videos -> ToolInfo(HelixIcons.Videocam, R.string.albums_videos, videos)
    AlbumTool.Screenshots -> ToolInfo(HelixIcons.Photo, R.string.albums_screenshots, screenshots)
    AlbumTool.Trash -> ToolInfo(Icons.Filled.Delete, R.string.albums_trash, trashed)
    AlbumTool.Vault -> ToolInfo(HelixIcons.Lock, R.string.albums_vault, null)
    AlbumTool.Cleanup -> ToolInfo(HelixIcons.AutoFix, R.string.albums_cleanup, null)
}

/** O último item da linha: círculo pontilhado com "+" e o texto "Editar ferramentas" dentro do pontilhado. */
@Composable
private fun EditToolsTile(onClick: () -> Unit) {
    val line = MaterialTheme.colorScheme.outline
    val dash = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))
    val corner = with(LocalDensity.current) { 28.dp.toPx() }
    Column(
        Modifier
            .width(110.dp)
            .height(IntrinsicSize.Max)
            .clip(MaterialTheme.shapes.extraLarge)
            .clickable(onClick = onClick)
            .drawBehind {
                drawRoundRect(line, cornerRadius = CornerRadius(corner), style = Stroke(width = 2f, pathEffect = dash))
            }
            .padding(vertical = 14.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(52.dp).drawBehind {
                drawCircle(line, style = Stroke(width = 2f, pathEffect = dash))
            },
            contentAlignment = Alignment.Center,
        ) {
            Icon(HelixIcons.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            text = stringResource(R.string.albums_edit_tools),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}

@Composable
private fun EditToolsSheet(
    tools: List<AlbumTool>,
    onUnpin: (AlbumTool) -> Unit,
    onPin: (AlbumTool) -> Unit,
    onMove: (AlbumTool, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    HelixModalSheet(onDismiss = onDismiss) {
        val sheet = this
        Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            Text(
                stringResource(R.string.albums_edit_tools_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            Text(
                stringResource(R.string.albums_pinned),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            tools.forEachIndexed { index, tool ->
                val info = toolInfo(tool, 0, 0, 0, 0)
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(info.icon, contentDescription = null, modifier = Modifier.padding(start = 8.dp, end = 16.dp))
                    Text(
                        stringResource(info.title),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { onMove(tool, -1) }, enabled = index > 0) {
                        Icon(Icons.Filled.KeyboardArrowUp, contentDescription = stringResource(R.string.albums_move_up))
                    }
                    IconButton(onClick = { onMove(tool, 1) }, enabled = index < tools.lastIndex) {
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = stringResource(R.string.albums_move_down))
                    }
                    TextButton(onClick = { onUnpin(tool) }) { Text(stringResource(R.string.albums_unpin)) }
                }
            }
            val hidden = AlbumTool.entries.filter { it !in tools }
            if (hidden.isNotEmpty()) {
                Text(
                    stringResource(R.string.albums_to_pin),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
                hidden.forEach { tool ->
                    val info = toolInfo(tool, 0, 0, 0, 0)
                    Row(
                        Modifier.fillMaxWidth().clickable { onPin(tool) }.padding(horizontal = 24.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(info.icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            stringResource(info.title),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f).padding(start = 16.dp),
                        )
                        Icon(HelixIcons.Add, contentDescription = stringResource(R.string.albums_pin))
                    }
                }
            }
            TextButton(onClick = { sheet.dismiss() }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Text(stringResource(R.string.albums_done))
            }
        }
    }
}

@Composable
private fun ShortcutTile(icon: ImageVector, title: String, count: Int?, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.width(92.dp).clip(MaterialTheme.shapes.extraLarge).clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(vertical = 14.dp, horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(52.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 10.dp),
            )
            Text(
                text = count?.toString() ?: " ",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
