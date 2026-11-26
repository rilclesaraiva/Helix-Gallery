// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.features.cleanup

import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.helixgallery.core.designsystem.component.HelixAccent
import com.helixgallery.core.designsystem.component.HelixFloatingBackButton
import com.helixgallery.core.designsystem.component.HelixIcons
import com.helixgallery.core.designsystem.component.HelixPageTitle
import com.helixgallery.core.designsystem.component.HelixSettingsItem
import com.helixgallery.core.designsystem.component.HelixSettingsSection
import com.helixgallery.core.designsystem.component.HelixStatusBarScrim
import com.helixgallery.core.designsystem.component.HelixSubPage
import com.helixgallery.core.designsystem.component.MediaTile
import com.helixgallery.core.model.MediaItem
import com.helixgallery.core.model.largeVideosOf
import com.helixgallery.core.model.screenshotsOf
import com.helixgallery.core.model.totalBytes

private enum class Category { Screenshots, LargeVideos, Duplicates }

/** Limpeza: capturas de tela, vídeos grandes e cópias idênticas, para revisar e mandar à lixeira de uma vez. */
@Composable
fun CleanupScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel: CleanupViewModel = viewModel()
    val context = LocalContext.current
    val state by viewModel.media.collectAsStateWithLifecycle()
    val scan by viewModel.scan.collectAsStateWithLifecycle()
    var review by rememberSaveable { mutableStateOf<Category?>(null) }

    val screenshots = remember(state.items) { screenshotsOf(state.items) }
    val largeVideos = remember(state.items) { largeVideosOf(state.items) }
    // As sobras: de cada grupo de cópias idênticas, tudo menos a mais antiga (que se mantém).
    val duplicateExtras = remember(scan, state.items) {
        val present = state.items.map { it.id }.toSet()
        ((scan as? DuplicateScan.Done)?.groups.orEmpty()).flatMap { it.drop(1) }.filter { it.id in present }
    }

    BackHandler(enabled = review != null) { review = null }
    val category = review
    if (category == null) {
        HelixSubPage(title = stringResource(R.string.cleanup_title), onBack = onBack, modifier = modifier) {
            HelixSettingsSection(title = null) {
                HelixSettingsItem(
                    icon = HelixIcons.Photo,
                    accent = HelixAccent.Teal,
                    title = stringResource(R.string.cleanup_screenshots),
                    subtitle = summary(context, screenshots),
                    onClick = { if (screenshots.isNotEmpty()) review = Category.Screenshots },
                )
                HelixSettingsItem(
                    icon = HelixIcons.Videocam,
                    accent = HelixAccent.Orange,
                    title = stringResource(R.string.cleanup_large_videos),
                    subtitle = summary(context, largeVideos),
                    onClick = { if (largeVideos.isNotEmpty()) review = Category.LargeVideos },
                )
                HelixSettingsItem(
                    icon = HelixIcons.Collections,
                    accent = HelixAccent.Pink,
                    title = stringResource(R.string.cleanup_duplicates),
                    subtitle = when (val s = scan) {
                        DuplicateScan.Idle -> stringResource(R.string.cleanup_scan)
                        is DuplicateScan.Scanning -> stringResource(R.string.cleanup_scanning, s.done, s.total)
                        is DuplicateScan.Done -> summary(context, duplicateExtras)
                    },
                    onClick = {
                        when (scan) {
                            DuplicateScan.Idle -> viewModel.scanDuplicates(state.items)
                            is DuplicateScan.Done -> if (duplicateExtras.isNotEmpty()) review = Category.Duplicates
                            is DuplicateScan.Scanning -> Unit
                        }
                    },
                )
            }
            Text(
                text = stringResource(R.string.cleanup_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
        }
    } else {
        val items = when (category) {
            Category.Screenshots -> screenshots
            Category.LargeVideos -> largeVideos
            Category.Duplicates -> duplicateExtras
        }
        ReviewPage(
            title = stringResource(
                when (category) {
                    Category.Screenshots -> R.string.cleanup_screenshots
                    Category.LargeVideos -> R.string.cleanup_large_videos
                    Category.Duplicates -> R.string.cleanup_duplicates
                },
            ),
            items = items,
            preselectAll = category == Category.Duplicates,
            showKeepNote = category == Category.Duplicates,
            onBack = { review = null },
            onTrash = { uris -> viewModel.repository.trashRequest(uris, true) },
            onTrashed = { if (category == Category.Duplicates) viewModel.resetScan() },
            modifier = modifier,
        )
    }
}

private fun summary(context: android.content.Context, items: List<MediaItem>): String =
    context.resources.getQuantityString(
        R.plurals.cleanup_summary,
        items.size,
        items.size,
        Formatter.formatShortFileSize(context, totalBytes(items)),
    )

@Composable
private fun ReviewPage(
    title: String,
    items: List<MediaItem>,
    preselectAll: Boolean,
    showKeepNote: Boolean,
    onBack: () -> Unit,
    onTrash: (List<String>) -> android.content.IntentSender,
    onTrashed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var selected by rememberSaveable(items.map { it.id }.hashCode()) {
        mutableStateOf(if (preselectAll) items.map { it.id }.toSet() else emptySet())
    }
    val chosen = items.filter { it.id in selected }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            selected = emptySet()
            onTrashed()
        }
    }
    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 112.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) { HelixPageTitle(title = title, hasBack = true) }
            if (showKeepNote) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        stringResource(R.string.cleanup_keep_note),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
            items(items, key = { it.id }) { item ->
                val toggle = { selected = if (item.id in selected) selected - item.id else selected + item.id }
                MediaTile(
                    item = item,
                    selected = item.id in selected,
                    selecting = true,
                    onClick = toggle,
                    onLongClick = toggle,
                )
            }
        }
        HelixStatusBarScrim(Modifier.align(Alignment.TopStart))
        HelixFloatingBackButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart))
        if (chosen.isNotEmpty()) {
            Button(
                onClick = {
                    runCatching {
                        launcher.launch(IntentSenderRequest.Builder(onTrash(chosen.map { it.uri })).build())
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(16.dp)
                    .fillMaxWidth(),
            ) {
                Text(
                    pluralStringResource(
                        R.plurals.cleanup_trash_action,
                        chosen.size,
                        chosen.size,
                        Formatter.formatShortFileSize(context, totalBytes(chosen)),
                    ),
                )
            }
        }
    }
}
