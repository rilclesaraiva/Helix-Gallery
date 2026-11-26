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
import com.helixgallery.core.designsystem.component.HelixPageTitle
import com.helixgallery.core.designsystem.component.HelixFloatingActions
import com.helixgallery.core.designsystem.component.HelixFloatingCircle
import com.helixgallery.core.designsystem.component.HelixStatusBarScrim
import com.helixgallery.core.designsystem.component.helixActionsInset
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

    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxSize()) {
        val titleRow: @Composable () -> Unit = {
            HelixPageTitle(title = stringResource(R.string.albums_title), actionsInset = helixActionsInset(1))
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
                        ShortcutTile(Icons.Filled.Favorite, stringResource(R.string.albums_favorites), favorites) {
                            onOpenCollection(MediaSource.Favorites)
                        }
                        ShortcutTile(HelixIcons.Videocam, stringResource(R.string.albums_videos), videos) {
                            onOpenCollection(MediaSource.Videos)
                        }
                        ShortcutTile(HelixIcons.Photo, stringResource(R.string.albums_screenshots), screenshots) {
                            onOpenCollection(MediaSource.Screenshots)
                        }
                        ShortcutTile(Icons.Filled.Delete, stringResource(R.string.albums_trash), state.trashed.size) {
                            onOpenCollection(MediaSource.Trash)
                        }
                        ShortcutTile(HelixIcons.Lock, stringResource(R.string.albums_vault), null, onOpenVault)
                        ShortcutTile(HelixIcons.AutoFix, stringResource(R.string.albums_cleanup), null, onOpenCleanup)
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
