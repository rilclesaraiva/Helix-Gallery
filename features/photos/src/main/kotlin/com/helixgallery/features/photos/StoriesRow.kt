// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.features.photos

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.helixgallery.core.designsystem.component.HelixStoryCard
import com.helixgallery.core.model.MediaSource
import com.helixgallery.core.model.Story

/** Fileira de lembranças no topo da aba Fotos: rola para o lado, cada cartão abre a sua lista. */
@Composable
internal fun StoriesRow(
    stories: List<Story>,
    onOpen: (MediaSource) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        stories.forEach { story ->
            val count = pluralStringResource(R.plurals.photos_story_count, story.count, story.count)
            when (story) {
                is Story.OnThisDay -> HelixStoryCard(
                    coverUri = story.cover.uri,
                    title = pluralStringResource(R.plurals.photos_story_years_ago, story.yearsAgo, story.yearsAgo),
                    subtitle = stringResource(R.string.photos_story_on_this_day),
                    onClick = { onOpen(story.source) },
                )
                is Story.QuickAlbum -> HelixStoryCard(
                    coverUri = story.cover.uri,
                    title = story.name,
                    subtitle = count,
                    onClick = { onOpen(story.source) },
                )
                is Story.Favorites -> HelixStoryCard(
                    coverUri = story.cover.uri,
                    title = stringResource(R.string.photos_story_favorites),
                    subtitle = count,
                    onClick = { onOpen(story.source) },
                )
            }
        }
    }
}
