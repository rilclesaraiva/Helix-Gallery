// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.features.viewer

import android.app.Activity
import android.net.Uri
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
import com.helixgallery.core.designsystem.component.heartbeat
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import com.helixgallery.core.designsystem.theme.helixEffectsSpec
import androidx.compose.material3.HorizontalDivider
import androidx.compose.animation.togetherWith
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.helixgallery.core.designsystem.component.HelixIcons
import com.helixgallery.core.designsystem.component.HelixBottomActionBar
import com.helixgallery.core.designsystem.component.HelixConfirmSheet
import com.helixgallery.core.designsystem.component.HelixSmartSheet
import com.helixgallery.core.model.buildAlbums
import com.helixgallery.core.model.AlbumSummary
import android.widget.Toast
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.geometry.Offset
import androidx.compose.animation.core.animate
import com.helixgallery.core.designsystem.theme.helixSpatialSpec
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import kotlin.math.abs
import kotlin.math.hypot
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Brush
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
    onMoveToVault: ((MediaItem) -> Unit)? = null,
    viewModel: ViewerViewModel = viewModel(),
) {
    val state by viewModel.media.collectAsStateWithLifecycle()
    val items = remember(source, state) { selectMedia(source, state) }
    Box(modifier.fillMaxSize().background(Color.Black)) {
        when {
            !state.loaded -> HelixLoading()
            items.isEmpty() -> LaunchedEffect(Unit) { onBack() }
            else -> ViewerContent(source, items, startId, onBack, onEdit, onMoveToVault, viewModel)
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
    onMoveToVault: ((MediaItem) -> Unit)?,
    viewModel: ViewerViewModel,
) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(
        initialPage = items.indexOfFirst { it.id == startId }.coerceAtLeast(0),
        pageCount = { items.size },
    )
    var chrome by remember { mutableStateOf(true) }
    var zoomed by remember { mutableStateOf(false) }
    // Detalhes: painel que sobe empurrando a foto junto (como no Google Fotos). [detailsProgress] 0 = fechado, 1 = aberto;
    // acompanha o dedo ao puxar e anima com a mola do tema ao soltar. [details] é o destino (aberto/fechado).
    var details by remember { mutableStateOf(false) }
    var detailsProgress by remember { mutableFloatStateOf(0f) }
    var detailsJob by remember { mutableStateOf<Job?>(null) }
    val detailsScope = rememberCoroutineScope()
    val detailsSpec = helixSpatialSpec<Float>()
    fun settleDetails(open: Boolean) {
        details = open
        detailsJob?.cancel()
        detailsJob = detailsScope.launch {
            animate(detailsProgress, if (open) 1f else 0f, animationSpec = detailsSpec) { value, _ -> detailsProgress = value }
        }
    }
    var addTo by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf<ViewerConfirm?>(null) }
    val inTrash = source == MediaSource.Trash
    val current = items[pagerState.currentPage.coerceIn(0, items.lastIndex)]

    LaunchedEffect(pagerState.currentPage) { zoomed = false }

    var onCancel by remember { mutableStateOf<(() -> Unit)?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        // A lista se atualiza sozinha (MediaStore avisa); só desfazemos o que mostramos na frente se a pessoa cancelou.
        if (result.resultCode != Activity.RESULT_OK) onCancel?.invoke()
        onCancel = null
    }
    fun launch(sender: IntentSender, onCancel0: (() -> Unit)? = null) {
        onCancel = onCancel0
        launcher.launch(IntentSenderRequest.Builder(sender).build())
    }

    fun share() {
        context.shareMedia(listOf(current.uri), listOf(current.mimeType), context.getString(R.string.viewer_share))
    }
    // Favoritar aparece na hora (como no Boom): o estado local vale até o MediaStore confirmar ou a pessoa cancelar.
    val favoriteOverride = remember { mutableStateMapOf<Long, Boolean>() }
    LaunchedEffect(items) { items.forEach { if (favoriteOverride[it.id] == it.isFavorite) favoriteOverride.remove(it.id) } }
    val currentFavorite = favoriteOverride[current.id] ?: current.isFavorite
    var beat by remember { mutableIntStateOf(0) }
    fun toggleFavorite() {
        val target = !currentFavorite
        favoriteOverride[current.id] = target
        if (target) beat++
        val id = current.id
        launch(viewModel.repository.favoriteRequest(listOf(current.uri), target)) { favoriteOverride.remove(id) }
    }
    var detailsPanelPx by remember { mutableFloatStateOf(1f) }
    var detailsViewportPx by remember { mutableFloatStateOf(0f) }
    fun dragDetails(dy: Float) {
        detailsJob?.cancel()
        detailsProgress = (detailsProgress - dy / detailsPanelPx).coerceIn(0f, 1f)
    }
    fun endDetails(velocityY: Float) = settleDetails(velocityY < -800f || (velocityY <= 800f && detailsProgress > 0.4f))
    // Dentro do painel, o conteúdo rola; ao chegar no topo, continuar puxando para baixo fecha o painel.
    var nestedDragging by remember { mutableStateOf(false) }
    val panelNested = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && nestedDragging && available.y < 0f && detailsProgress < 1f) {
                    dragDetails(available.y)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y > 0f && details) {
                    nestedDragging = true
                    dragDetails(available.y)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (!nestedDragging) return Velocity.Zero
                nestedDragging = false
                endDetails(available.y)
                return available
            }
        }
    }
    fun showDetails() {
        viewModel.loadExif(current.uri, current.isVideo)
        settleDetails(true)
    }
    BackHandler(enabled = details) { settleDetails(false) }
    // Trocando de foto com os detalhes abertos, eles passam a mostrar a nova.
    LaunchedEffect(current.id) { if (details) viewModel.loadExif(current.uri, current.isVideo) }
    val chromeShown by remember { derivedStateOf { chrome && detailsProgress < 0.05f } }
    val currentItems by rememberUpdatedState(items)

    fun trashOrRestore() = launch(viewModel.repository.trashRequest(listOf(current.uri), !inTrash))
    fun deleteForever() = launch(viewModel.repository.deleteRequest(listOf(current.uri)))

    // Os três pontinhos guardam só o que NÃO está na barra de baixo (compartilhar, editar, adicionar e lixeira já estão lá).
    val menuActions = listOf(
        HelixMenuAction(stringResource(R.string.viewer_details), Icons.Filled.Info, onClick = ::showDetails),
    )

    BoxWithConstraints(
        Modifier.fillMaxSize().detailsDrag(
            enabled = { !zoomed && (details || currentItems.getOrNull(pagerState.currentPage)?.isVideo != true) },
            progress = { detailsProgress },
            onDrag = ::dragDetails,
            onEnd = ::endDetails,
            skipStart = { pos -> detailsProgress > 0f && pos.y >= detailsViewportPx - detailsProgress * detailsPanelPx },
        ),
    ) {
        val density = LocalDensity.current
        val panelHeight = maxHeight * DETAILS_PANEL_FRACTION
        val panelPx = with(density) { panelHeight.toPx() }
        detailsPanelPx = panelPx
        detailsViewportPx = with(density) { maxHeight.toPx() }
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = !zoomed,
            key = { index -> items[index.coerceIn(0, items.lastIndex)].id },
            modifier = Modifier.fillMaxSize().graphicsLayer { translationY = -detailsProgress * panelPx * 0.5f },
        ) { page ->
            val item = items[page.coerceIn(0, items.lastIndex)]
            if (item.isVideo) {
                VideoPage(
                    uri = item.uri,
                    isCurrent = page == pagerState.currentPage,
                    controlsVisible = chrome,
                    onTap = { if (details) settleDetails(false) else chrome = !chrome },
                )
            } else {
                ZoomableImage(
                    uri = item.uri,
                    onTap = { if (details) settleDetails(false) else chrome = !chrome },
                    onZoomChanged = { if (page == pagerState.currentPage) zoomed = it },
                )
            }
        }

        // Nada de barra presa no topo: botões redondos translúcidos flutuam sobre a foto.
        AnimatedVisibility(
            visible = chromeShown,
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
                                if (currentFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = stringResource(
                                    if (currentFavorite) R.string.viewer_unfavorite else R.string.viewer_favorite,
                                ),
                                modifier = Modifier.heartbeat(beat),
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                }
                HelixMoreMenu(actions = menuActions)
            }
        }

        AnimatedVisibility(
            visible = chromeShown,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            if (inTrash) {
                // Na lixeira: Restaurar e Excluir em texto, como na seleção da lixeira.
                HelixBottomActionBar(
                    primaryLabel = stringResource(R.string.viewer_restore),
                    onPrimary = ::trashOrRestore,
                    destructiveLabel = stringResource(R.string.viewer_delete_trash),
                    onDestructive = { confirm = ViewerConfirm.DeleteForever },
                )
            } else
            // Barra inferior no padrão do Google Fotos: sem fundo cinza, só os ícones com rótulo sobre a foto,
            // com um leve escurecimento bem transparente atrás para continuarem legíveis.
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f)))),
            ) {
                CompositionLocalProvider(LocalContentColor provides Color.White) {
                    Row(Modifier.navigationBarsPadding().padding(start = 8.dp, end = 8.dp, top = 24.dp, bottom = 4.dp)) {
                        if (!inTrash) {
                            LabeledBarButton(Icons.Filled.Share, stringResource(R.string.viewer_share), Modifier.weight(1f)) { share() }
                            if (onEdit != null) {
                                LabeledBarButton(Icons.Filled.Edit, stringResource(R.string.viewer_edit), Modifier.weight(1f)) { onEdit(current) }
                            }
                            LabeledBarButton(HelixIcons.Add, stringResource(R.string.viewer_add_to), Modifier.weight(1f)) { addTo = true }
                        }
                        LabeledBarButton(
                            if (inTrash) Icons.Filled.Refresh else Icons.Filled.Delete,
                            stringResource(if (inTrash) R.string.viewer_restore else R.string.viewer_trash),
                            Modifier.weight(1f),
                        ) { if (inTrash) trashOrRestore() else confirm = ViewerConfirm.Trash }
                        if (inTrash) {
                            LabeledBarButton(Icons.Filled.Delete, stringResource(R.string.viewer_delete_forever), Modifier.weight(1f)) {
                                confirm = ViewerConfirm.DeleteForever
                            }
                        }
                    }
                }
            }
        }
        // Painel de detalhes: sobe de baixo junto com a foto (que sobe pela metade da altura dele).
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = MaterialTheme.shapes.extraLarge.copy(bottomStart = CornerSize(0.dp), bottomEnd = CornerSize(0.dp)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(panelHeight)
                .graphicsLayer { translationY = (1f - detailsProgress) * panelPx },
        ) {
            Column(Modifier.fillMaxSize()) {
                // Puxador: arrastar para baixo fecha.
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .detailsDrag(
                            enabled = { true },
                            progress = { detailsProgress },
                            onDrag = ::dragDetails,
                            onEnd = ::endDetails,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .width(32.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)),
                    )
                }
                Column(Modifier.weight(1f).nestedScroll(panelNested).verticalScroll(rememberScrollState()).navigationBarsPadding()) {
                    DetailsContent(item = current, viewModel = viewModel)
                }
            }
        }
    }

    if (addTo) {
        val all by viewModel.media.collectAsStateWithLifecycle()
        val albums = remember(all) { buildAlbums(all.items) }
        AddToSheet(
            albums = albums.filter { it.bucketId != current.bucketId },
            onVault = {
                val item = current
                viewModel.lockToVault(item) { result ->
                    when (result) {
                        true -> {
                            Toast.makeText(context, R.string.viewer_locked, Toast.LENGTH_SHORT).show()
                            // Já está no cofre: a cópia solta na galeria sai.
                            launch(viewModel.repository.deleteRequest(listOf(item.uri)))
                        }
                        false -> Toast.makeText(context, R.string.viewer_lock_failed, Toast.LENGTH_SHORT).show()
                        null -> onMoveToVault?.invoke(item)
                    }
                }
            },
            onAlbum = { album ->
                viewModel.copyTo(current, album.cover.relativePath) { ok ->
                    Toast.makeText(
                        context,
                        context.getString(if (ok) R.string.viewer_added else R.string.viewer_add_failed, album.name),
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            },
            onDismiss = { addTo = false },
        )
    }

    confirm?.let { kind ->
        HelixConfirmSheet(
            title = stringResource(if (kind == ViewerConfirm.Trash) R.string.viewer_confirm_trash_title else R.string.viewer_confirm_delete_title),
            message = stringResource(if (kind == ViewerConfirm.Trash) R.string.viewer_confirm_trash_message else R.string.viewer_confirm_delete_message),
            confirmLabel = stringResource(if (kind == ViewerConfirm.Trash) R.string.viewer_trash else R.string.viewer_delete_forever),
            onConfirm = { if (kind == ViewerConfirm.Trash) trashOrRestore() else deleteForever() },
            onDismiss = { confirm = null },
        )
    }

}

private enum class ViewerConfirm { Trash, DeleteForever }

/**
 * Painel "Adicionar a" (painel inteligente): pequeno, com Álbum, uma linha de separação e Cofre. Álbum abre
 * a lista de pastas como página do mesmo painel (com botão de voltar).
 */
@Composable
private fun AddToSheet(
    albums: List<AlbumSummary>,
    onVault: (() -> Unit)?,
    onAlbum: (AlbumSummary) -> Unit,
    onDismiss: () -> Unit,
) {
    HelixSmartSheet(onDismiss = onDismiss) {
        val sheet = this
        Column(Modifier.padding(bottom = 24.dp)) {
            Text(
                text = stringResource(R.string.viewer_add_to),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            AddToRow(HelixIcons.Collections, stringResource(R.string.viewer_add_to_album)) {
                sheet.open {
                    Column(Modifier.padding(bottom = 24.dp)) {
                        Text(
                            text = stringResource(R.string.viewer_add_to_album),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                        )
                        Column(Modifier.verticalScroll(rememberScrollState())) {
                            albums.forEach { album ->
                                AddToRow(HelixIcons.Collections, album.name) {
                                    onAlbum(album)
                                    dismiss()
                                }
                            }
                        }
                    }
                }
            }
            if (onVault != null) {
                HorizontalDivider(Modifier.padding(horizontal = 24.dp, vertical = 4.dp))
                AddToRow(HelixIcons.Lock, stringResource(R.string.viewer_add_to_vault)) {
                    onVault()
                    sheet.dismiss()
                }
            }
        }
    }
}

@Composable
private fun AddToRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
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
private fun DetailsContent(item: MediaItem, viewModel: ViewerViewModel) {
    val context = LocalContext.current
    val exif by viewModel.exif.collectAsStateWithLifecycle()
    Column(Modifier.padding(bottom = 24.dp)) {
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

private const val DETAILS_PANEL_FRACTION = 0.58f

/**
 * Puxar para cima abre os detalhes e puxar para baixo fecha, acompanhando o dedo. O eixo é decidido uma vez só,
 * no começo do gesto: se for mais vertical (e para cima, quando fechado), este gesto fica com ele e o carrossel
 * não recebe nada; se for mais horizontal, nada aqui, e só o carrossel troca de foto. Assim os dois nunca disputam.
 * [skipStart] diz onde um toque NÃO começa o gesto (dentro do painel, que rola sozinho).
 */
private fun Modifier.detailsDrag(
    enabled: () -> Boolean,
    progress: () -> Float,
    onDrag: (dy: Float) -> Unit,
    onEnd: (velocityY: Float) -> Unit,
    skipStart: (Offset) -> Boolean = { false },
): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        if (!enabled() || skipStart(down.position)) return@awaitEachGesture
        val tracker = VelocityTracker()
        tracker.addPosition(down.uptimeMillis, down.position)
        var dx = 0f
        var dy = 0f
        var vertical = false
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            if (!change.pressed) break
            if (!vertical) {
                // Dois dedos (zoom) ou já consumido por outro: não é com este gesto.
                if (event.changes.size > 1 || change.isConsumed) return@awaitEachGesture
                dx += change.positionChange().x
                dy += change.positionChange().y
                if (hypot(dx, dy) < viewConfiguration.touchSlop) continue
                val goesVertical = abs(dy) > abs(dx) * 1.2f && (progress() > 0f || dy < 0f)
                if (!goesVertical) return@awaitEachGesture
                vertical = true
                tracker.addPosition(change.uptimeMillis, change.position)
                onDrag(dy)
            } else {
                tracker.addPosition(change.uptimeMillis, change.position)
                onDrag(change.positionChange().y)
            }
            change.consume()
        }
        if (vertical) onEnd(tracker.calculateVelocity().y)
    }
}
