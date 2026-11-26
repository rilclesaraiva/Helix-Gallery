// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.features.viewer

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.helixgallery.core.designsystem.component.HelixFloatingCircle
import com.helixgallery.core.designsystem.component.HelixIcons
import com.helixgallery.core.designsystem.component.HelixSeekBar
import com.helixgallery.core.designsystem.component.formatDuration
import kotlinx.coroutines.delay

/** Intervalo de atualização da barra de progresso. */
private const val PROGRESS_POLL_MS = 200L

/**
 * Vídeo com os controles do app (a barra de progresso é a do BoomListener, não a do Media3).
 * Só toca quando é a página à vista; ao sair, pausa. Tocar no vídeo mostra/esconde os controles.
 */
@Composable
fun VideoPage(
    uri: String,
    isCurrent: Boolean,
    controlsVisible: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val player = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(ExoMediaItem.fromUri(Uri.parse(uri)))
            prepare()
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }
    LaunchedEffect(isCurrent) {
        if (isCurrent) player.play() else player.pause()
    }

    var playing by remember(player) { mutableStateOf(false) }
    var muted by remember(player) { mutableStateOf(false) }
    var positionMs by remember(player) { mutableLongStateOf(0L) }
    var durationMs by remember(player) { mutableLongStateOf(0L) }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playing = isPlaying
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }
    LaunchedEffect(player, isCurrent) {
        while (isCurrent) {
            positionMs = player.currentPosition.coerceAtLeast(0L)
            durationMs = player.duration.coerceAtLeast(0L)
            delay(PROGRESS_POLL_MS)
        }
    }

    Box(modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = false
                }
            },
            update = { it.player = player },
            modifier = Modifier.fillMaxSize(),
        )
        // Camada de toque por cima do vídeo: mostra/esconde os controles.
        Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures(onTap = { onTap() }) })
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Row(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 96.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HelixFloatingCircle {
                    IconButton(onClick = { if (player.isPlaying) player.pause() else player.play() }) {
                        Icon(
                            if (playing) HelixIcons.Pause else Icons.Filled.PlayArrow,
                            contentDescription = stringResource(if (playing) R.string.viewer_pause else R.string.viewer_play),
                        )
                    }
                }
                Box(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    HelixSeekBar(
                        progress = if (durationMs > 0) positionMs.toFloat() / durationMs else 0f,
                        isPlaying = playing,
                        onSeek = { fraction ->
                            val target = (fraction * durationMs).toLong()
                            player.seekTo(target)
                            positionMs = target
                        },
                        dragLabel = { fraction -> formatDuration((fraction * durationMs).toLong()) },
                    )
                }
                Text(
                    text = "${formatDuration(positionMs)} / ${formatDuration(durationMs)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                HelixFloatingCircle(Modifier.padding(start = 8.dp)) {
                    IconButton(
                        onClick = {
                            muted = !muted
                            player.volume = if (muted) 0f else 1f
                        },
                    ) {
                        Icon(
                            if (muted) HelixIcons.VolumeOff else HelixIcons.VolumeUp,
                            contentDescription = stringResource(if (muted) R.string.viewer_unmute else R.string.viewer_mute),
                        )
                    }
                }
            }
        }
    }
}
