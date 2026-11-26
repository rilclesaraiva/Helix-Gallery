// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.features.viewer

import android.app.Activity
import android.content.IntentSender
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.helixgallery.core.common.shareMedia
import com.helixgallery.core.designsystem.component.HelixLoading
import com.helixgallery.core.designsystem.component.HelixModalSheet
import com.helixgallery.core.designsystem.component.formatDuration
import com.helixgallery.core.model.MediaItem
import com.helixgallery.core.model.MediaSource
import com.helixgallery.core.model.selectMedia
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Visualizador: passa de foto em foto (e de vídeo em vídeo) dentro da lista de onde veio. */
@Composable
fun ViewerScreen(
    source: MediaSource,
    startId: Long,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ViewerViewModel = viewModel(),
) {
    val state by viewModel.media.collectAsStateWithLifecycle()
    val items = remember(source, state) { selectMedia(source, state) }
    Box(modifier.fillMaxSize().background(Color.Black)) {
        when {
            !state.loaded -> HelixLoading()
            items.isEmpty() -> LaunchedEffect(Unit) { onBack() }
            else -> ViewerContent(source, items, startId, onBack, viewModel)
        }
    }
}

@Composable
private fun ViewerContent(
    source: MediaSource,
    items: List<MediaItem>,
    startId: Long,
    onBack: () -> Unit,
    viewModel: ViewerViewModel,
) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(
        initialPage = items.indexOfFirst { it.id == startId }.coerceAtLeast(0),
        pageCount = { items.size },
    )
    var chrome by remember { mutableStateOf(true) }
    var zoomed by remember { mutableStateOf(false) }
    var details by remember { mutableStateOf(false) }
    val inTrash = source == MediaSource.Trash
    val current = items[pagerState.currentPage.coerceIn(0, items.lastIndex)]

    LaunchedEffect(pagerState.currentPage) { zoomed = false }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { _ ->
        // A lista se atualiza sozinha (MediaStore avisa); nada a fazer aqui.
    }
    fun launch(sender: IntentSender) = launcher.launch(IntentSenderRequest.Builder(sender).build())

    Box(Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = !zoomed,
            key = { index -> items[index.coerceIn(0, items.lastIndex)].id },
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            val item = items[page.coerceIn(0, items.lastIndex)]
            if (item.isVideo) {
                VideoPage(uri = item.uri, isCurrent = page == pagerState.currentPage)
            } else {
                ZoomableImage(
                    uri = item.uri,
                    onTap = { chrome = !chrome },
                    onZoomChanged = { if (page == pagerState.currentPage) zoomed = it },
                )
            }
        }

        AnimatedVisibility(
            visible = chrome,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)))
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.viewer_back), tint = Color.White)
                }
                Text(
                    text = formatDate(current.takenMs),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
            }
        }

        AnimatedVisibility(
            visible = chrome,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))))
                    .navigationBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!inTrash) {
                    BarButton(Icons.Filled.Share, stringResource(R.string.viewer_share), Modifier.weight(1f)) {
                        context.shareMedia(listOf(current.uri), listOf(current.mimeType), context.getString(R.string.viewer_share))
                    }
                    BarButton(
                        if (current.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        stringResource(if (current.isFavorite) R.string.viewer_unfavorite else R.string.viewer_favorite),
                        Modifier.weight(1f),
                    ) {
                        launch(viewModel.repository.favoriteRequest(listOf(current.uri), !current.isFavorite))
                    }
                }
                BarButton(Icons.Filled.Info, stringResource(R.string.viewer_details), Modifier.weight(1f)) {
                    viewModel.loadExif(current.uri, current.isVideo)
                    details = true
                }
                BarButton(
                    if (inTrash) Icons.Filled.Refresh else Icons.Filled.Delete,
                    stringResource(if (inTrash) R.string.viewer_restore else R.string.viewer_trash),
                    Modifier.weight(1f),
                ) {
                    launch(viewModel.repository.trashRequest(listOf(current.uri), !inTrash))
                }
                if (inTrash) {
                    BarButton(Icons.Filled.Delete, stringResource(R.string.viewer_delete_forever), Modifier.weight(1f)) {
                        launch(viewModel.repository.deleteRequest(listOf(current.uri)))
                    }
                }
            }
        }
    }

    if (details) {
        DetailsSheet(item = current, viewModel = viewModel, onDismiss = { details = false })
    }
}

@Composable
private fun BarButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(icon, contentDescription = description, tint = Color.White)
    }
}

@Composable
private fun DetailsSheet(item: MediaItem, viewModel: ViewerViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val exif by viewModel.exif.collectAsStateWithLifecycle()
    HelixModalSheet(onDismiss = onDismiss) {
        Text(
            text = stringResource(R.string.details_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        Column(Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
            Detail(stringResource(R.string.details_name), item.displayName)
            Detail(stringResource(R.string.details_date), formatDateTime(item.takenMs))
            Detail(stringResource(R.string.details_size), Formatter.formatFileSize(context, item.sizeBytes))
            if (item.width > 0 && item.height > 0) {
                Detail(stringResource(R.string.details_resolution), "${item.width} × ${item.height}")
            }
            if (item.isVideo && item.durationMs > 0) {
                Detail(stringResource(R.string.details_duration), formatDuration(item.durationMs))
            }
            Detail(stringResource(R.string.details_type), item.mimeType)
            Detail(stringResource(R.string.details_folder), item.relativePath.ifBlank { item.bucketName })
            exif?.let { info ->
                info.camera?.let { Detail(stringResource(R.string.details_camera), it) }
                val settings = listOfNotNull(info.aperture, info.exposure, info.iso, info.focalLength)
                if (settings.isNotEmpty()) {
                    Detail(stringResource(R.string.details_settings), settings.joinToString("  ·  "))
                }
            }
        }
    }
}

@Composable
private fun Detail(label: String, value: String) {
    Column(Modifier.padding(vertical = 6.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

private fun formatDate(ms: Long): String =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        .format(Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()))

private fun formatDateTime(ms: Long): String =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
        .format(Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()))
