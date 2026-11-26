// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.features.editor

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.helixgallery.core.designsystem.component.HelixFloatingActions
import com.helixgallery.core.designsystem.component.HelixFloatingBackButton
import com.helixgallery.core.designsystem.component.HelixFloatingCircle
import com.helixgallery.core.designsystem.component.HelixIcons
import com.helixgallery.core.designsystem.component.formatDuration
import kotlinx.coroutines.delay

/** Editor de vídeo: cortar o trecho, girar e tirar o som; salva uma cópia (o original não muda). */
@Composable
fun VideoEditorScreen(
    uri: String,
    displayName: String,
    relativePath: String,
    takenMs: Long,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: VideoEditorViewModel = viewModel()
    val context = LocalContext.current
    val saving by viewModel.saving.collectAsStateWithLifecycle()

    var durationMs by remember { mutableLongStateOf(0L) }
    var range by remember { mutableStateOf(0f..1f) }
    var mute by remember { mutableStateOf(false) }
    var rotation by remember { mutableIntStateOf(0) }
    var playing by remember { mutableStateOf(false) }
    var rotationAngle by remember { mutableFloatStateOf(0f) }

    val player = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(uri)))
            prepare()
        }
    }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY && durationMs == 0L) durationMs = player.duration.coerceAtLeast(0L)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playing = isPlaying
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }
    val startMs = (range.start * durationMs).toLong()
    val endMs = (range.endInclusive * durationMs).toLong()
    LaunchedEffect(mute) { player.volume = if (mute) 0f else 1f }
    // Dentro do trecho: ao chegar no fim dele, pausa e volta ao começo.
    LaunchedEffect(player, startMs, endMs) {
        while (true) {
            if (player.isPlaying && endMs > 0 && player.currentPosition >= endMs) {
                player.pause()
                player.seekTo(startMs)
            }
            delay(100)
        }
    }
    val edit = VideoEdit(startMs, endMs, mute, rotation)
    val changed = durationMs > 0 && edit.isChanged(durationMs)

    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxWidth().statusBarsPadding().padding(top = 64.dp, bottom = 8.dp)) {
                AndroidView(
                    factory = { ctx -> PlayerView(ctx).apply { this.player = player; useController = false } },
                    modifier = Modifier.fillMaxSize().graphicsLayer { rotationZ = rotationAngle },
                )
            }
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(formatDuration(startMs), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                        Box(Modifier.weight(1f))
                        Text(
                            stringResource(R.string.editor_video_length, formatDuration((endMs - startMs).coerceAtLeast(0))),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Box(Modifier.weight(1f))
                        Text(formatDuration(endMs), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                    }
                    RangeSlider(
                        value = range,
                        onValueChange = { new ->
                            val movedStart = new.start != range.start
                            range = new
                            if (durationMs > 0) player.seekTo(((if (movedStart) new.start else new.endInclusive) * durationMs).toLong())
                        },
                        valueRange = 0f..1f,
                        enabled = durationMs > 0,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            if (player.isPlaying) player.pause() else {
                                if (player.currentPosition < startMs || player.currentPosition >= endMs) player.seekTo(startMs)
                                player.play()
                            }
                        }) {
                            Icon(
                                if (playing) HelixIcons.Pause else Icons.Filled.PlayArrow,
                                contentDescription = stringResource(if (playing) R.string.editor_video_pause else R.string.editor_video_play),
                            )
                        }
                        IconButton(onClick = {
                            rotation = (rotation + 1) % 4
                            rotationAngle += 90f
                        }) {
                            Icon(HelixIcons.RotateRight, contentDescription = stringResource(R.string.editor_rotate_right))
                        }
                        IconButton(onClick = { mute = !mute }) {
                            Icon(
                                if (mute) HelixIcons.VolumeOff else HelixIcons.VolumeUp,
                                contentDescription = stringResource(if (mute) R.string.editor_video_unmute else R.string.editor_video_mute),
                            )
                        }
                    }
                }
            }
        }
        HelixFloatingBackButton(onClick = onClose, modifier = Modifier.align(Alignment.TopStart))
        HelixFloatingActions(Modifier.align(Alignment.TopEnd)) {
            HelixFloatingCircle {
                IconButton(
                    enabled = !saving && changed,
                    onClick = {
                        player.pause()
                        viewModel.save(uri, displayName, relativePath, takenMs, edit) { ok ->
                            Toast.makeText(context, if (ok) R.string.editor_saved else R.string.editor_failed, Toast.LENGTH_LONG).show()
                            if (ok) onClose()
                        }
                    },
                ) {
                    Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.editor_save))
                }
            }
        }
    }
}
