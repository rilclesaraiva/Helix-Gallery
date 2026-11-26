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
import com.helixgallery.core.designsystem.component.HelixFloatingCircle
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.helixgallery.core.common.shareMedia
import com.helixgallery.core.designsystem.component.FLOATING_BUTTON_ALPHA
import com.helixgallery.core.designsystem.component.HelixBackButton
import com.helixgallery.core.designsystem.component.HelixLoading
import com.helixgallery.core.designsystem.component.HelixMenuAction
import com.helixgallery.core.designsystem.component.HelixMoreMenu
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
    onEdit: ((MediaItem) -> Unit)? = null,
    viewModel: ViewerViewModel = viewModel(),
) {
    val state by viewModel.media.collectAsStateWithLifecycle()
    val items = remember(source, state) { selectMedia(source, state) }
    Box(modifier.fillMaxSize().background(Color.Black)) {
        when {
            !state.loaded -> HelixLoading()
            items.isEmpty() -> LaunchedEffect(Unit) { onBack() }
            else -> ViewerContent(source, items, startId, onBack, onEdit, viewModel)
        }
    }
}

@Composable
private fun ViewerContent(
    source: MediaSource,
    items: List<MediaItem>,
    startId: Long,
    onBack: () -> Unit,
    onEdit: ((MediaItem) -> Unit)?,
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

    fun share() {
        context.shareMedia(listOf(current.uri), listOf(current.mimeType), context.getString(R.string.viewer_share))
    }
    fun toggleFavorite() = launch(viewModel.repository.favoriteRequest(listOf(current.uri), !current.isFavorite))
    fun showDetails() {
        viewModel.loadExif(current.uri, current.isVideo)
        details = true
    }
    fun trashOrRestore() = launch(viewModel.repository.trashRequest(listOf(current.uri), !inTrash))
    fun deleteForever() = launch(viewModel.repository.deleteRequest(listOf(current.uri)))

    // O menu dos três pontinhos: das ações mais usadas às mais raras, a destrutiva por último.
    val menuActions = buildList {
        if (!inTrash) {
            add(HelixMenuAction(stringResource(R.string.viewer_share), Icons.Filled.Share, onClick = ::share))
            add(
                HelixMenuAction(
                    stringResource(if (current.isFavorite) R.string.viewer_unfavorite else R.string.viewer_favorite),
                    if (current.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    onClick = ::toggleFavorite,
                ),
            )
        }
        if (!inTrash && !current.isVideo && onEdit != null) {
            add(HelixMenuAction(stringResource(R.string.viewer_edit), Icons.Filled.Edit, onClick = { onEdit(current) }))
        }
        add(HelixMenuAction(stringResource(R.string.viewer_details), Icons.Filled.Info, onClick = ::showDetails))
        add(
            HelixMenuAction(
                stringResource(if (inTrash) R.string.viewer_restore else R.string.viewer_trash),
                if (inTrash) Icons.Filled.Refresh else Icons.Filled.Delete,
                destructive = !inTrash,
                onClick = ::trashOrRestore,
            ),
        )
        if (inTrash) {
            add(
                HelixMenuAction(
                    stringResource(R.string.viewer_delete_forever),
                    Icons.Filled.Delete,
                    destructive = true,
                    onClick = ::deleteForever,
                ),
            )
        }
    }

    Box(Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = !zoomed,
            key = { index -> items[index.coerceIn(0, items.lastIndex)].id },
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            val item = items[page.coerceIn(0, items.lastIndex)]
            if (item.isVideo) {
                VideoPage(
                    uri = item.uri,
                    isCurrent = page == pagerState.currentPage,
                    controlsVisible = chrome,
                    onTap = { chrome = !chrome },
                )
            } else {
                ZoomableImage(
                    uri = item.uri,
                    onTap = { chrome = !chrome },
                    onZoomChanged = { if (page == pagerState.currentPage) zoomed = it },
                )
            }
        }

        // Nada de barra presa no topo: botões redondos translúcidos flutuam sobre a foto.
        AnimatedVisibility(
            visible = chrome,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HelixBackButton(onClick = onBack)
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = FLOATING_BUTTON_ALPHA),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    Text(
                        text = formatDate(current.takenMs),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                if (!inTrash) {
                    HelixFloatingCircle {
                        IconButton(onClick = ::toggleFavorite) {
                            Icon(
                                if (current.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = stringResource(
                                    if (current.isFavorite) R.string.viewer_unfavorite else R.string.viewer_favorite,
                                ),
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                }
                HelixMoreMenu(actions = menuActions)
            }
        }

        AnimatedVisibility(
            visible = chrome,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            // Barra inferior no padrão do Google Fotos: de ponta a ponta, ícone com rótulo embaixo
            // (diferente da barra flutuante da tela inicial).
            Surface(
                shape = MaterialTheme.shapes.extraLarge.copy(bottomStart = CornerSize(0.dp), bottomEnd = CornerSize(0.dp)),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = FLOATING_BUTTON_ALPHA),
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.navigationBarsPadding().padding(horizontal = 8.dp, vertical = 4.dp)) {
                    if (!inTrash) {
                        LabeledBarButton(Icons.Filled.Share, stringResource(R.string.viewer_share), Modifier.weight(1f)) { share() }
                        if (!current.isVideo && onEdit != null) {
                            LabeledBarButton(Icons.Filled.Edit, stringResource(R.string.viewer_edit), Modifier.weight(1f)) { onEdit(current) }
                        }
                    }
                    LabeledBarButton(
                        if (inTrash) Icons.Filled.Refresh else Icons.Filled.Delete,
                        stringResource(if (inTrash) R.string.viewer_restore else R.string.viewer_trash),
                        Modifier.weight(1f),
                    ) { trashOrRestore() }
                    if (inTrash) {
                        LabeledBarButton(Icons.Filled.Delete, stringResource(R.string.viewer_delete_forever), Modifier.weight(1f)) { deleteForever() }
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
private fun LabeledBarButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier.clip(MaterialTheme.shapes.large).clickable(onClick = onClick).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null)
        Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1, modifier = Modifier.padding(top = 2.dp))
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
