// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.helixgallery.core.designsystem.component.HelixFloatingNav
import com.helixgallery.core.designsystem.component.HelixIcons
import com.helixgallery.core.designsystem.component.HelixNavItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import com.helixgallery.core.designsystem.theme.helixEffectsSpec
import com.helixgallery.core.model.MediaSource
import com.helixgallery.features.albums.AlbumsScreen
import com.helixgallery.features.photos.CollectionScreen
import com.helixgallery.features.search.SearchScreen

/** As três abas (Fotos, Álbuns, Busca) com a barra flutuante por cima. */
@Composable
fun MainTabs(
    onOpenCollection: (MediaSource) -> Unit,
    onOpenViewer: (MediaSource, Long) -> Unit,
    onOpenSettings: () -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    BackHandler(enabled = tab != 0) { tab = 0 }

    val items = listOf(
        HelixNavItem(stringResource(R.string.tab_photos), HelixIcons.Photo),
        HelixNavItem(stringResource(R.string.tab_albums), HelixIcons.Collections),
        HelixNavItem(stringResource(R.string.tab_search), Icons.Filled.Search),
    )

    Box(Modifier.fillMaxSize()) {
        Crossfade(targetState = tab, animationSpec = helixEffectsSpec(), label = "tabs") { current ->
            when (current) {
                0 -> CollectionScreen(
                    source = MediaSource.All,
                    title = stringResource(R.string.tab_photos),
                    onOpen = onOpenViewer,
                    bottomInset = true,
                )
                1 -> AlbumsScreen(
                    onOpenCollection = onOpenCollection,
                    onOpenSettings = onOpenSettings,
                )
                else -> SearchScreen(onOpen = { id -> onOpenViewer(MediaSource.All, id) })
            }
        }
        HelixFloatingNav(
            items = items,
            selectedIndex = tab,
            onSelect = { tab = it },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
