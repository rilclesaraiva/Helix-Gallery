// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.features.editor

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.helixgallery.data.media.MediaWriterData
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** O que a pessoa escolheu para o vídeo: o trecho (início e fim em ms), tirar o som e girar. */
internal data class VideoEdit(
    val startMs: Long,
    val endMs: Long,
    val mute: Boolean = false,
    /** Giro em passos de 90° para a direita (0 a 3). */
    val rotationSteps: Int = 0,
)

/** Houve mudança em relação ao vídeo inteiro de [durationMs]? Sem mudança, salvar não faz sentido. */
internal fun VideoEdit.isChanged(durationMs: Long): Boolean =
    startMs > 0 || endMs < durationMs || mute || rotationSteps % 4 != 0

/** "clip.mp4" → "clip_edit.mp4": o nome da cópia editada. */
internal fun editedVideoName(displayName: String): String {
    val dot = displayName.lastIndexOf('.')
    val base = if (dot > 0) displayName.substring(0, dot) else displayName
    return base + "_edit.mp4"
}

/** Pasta da cópia: a do original se for DCIM ou Movies; senão Movies/HelixGallery. */
internal fun videoFolder(relativePath: String): String =
    if (relativePath.startsWith("DCIM") || relativePath.startsWith("Movies")) relativePath else "Movies/HelixGallery/"

internal class VideoEditorViewModel(application: Application) : AndroidViewModel(application) {
    private val writer = MediaWriterData.writer(application)

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    /** Exporta o trecho para um arquivo temporário e o grava na galeria como cópia. O original não muda. */
    fun save(uri: String, displayName: String, relativePath: String, takenMs: Long, edit: VideoEdit, onDone: (Boolean) -> Unit) {
        if (_saving.value) return
        _saving.value = true
        viewModelScope.launch {
            val context = getApplication<Application>()
            val temp = File(context.cacheDir, "video_edit_${System.nanoTime()}.mp4")
            val exported = export(uri, temp, edit)
            val ok = exported && writer.copyToFolder(
                sourceUri = Uri.fromFile(temp).toString(),
                displayName = editedVideoName(displayName),
                mimeType = "video/mp4",
                isVideo = true,
                relativePath = videoFolder(relativePath),
                takenMs = takenMs,
            )
            temp.delete()
            _saving.value = false
            onDone(ok)
        }
    }

    private suspend fun export(uri: String, output: File, edit: VideoEdit): Boolean = suspendCancellableCoroutine { cont ->
        val clip = MediaItem.ClippingConfiguration.Builder()
            .setStartPositionMs(edit.startMs)
            .setEndPositionMs(edit.endMs)
            .build()
        val item = MediaItem.Builder().setUri(uri).setClippingConfiguration(clip).build()
        val videoEffects = if (edit.rotationSteps % 4 != 0) {
            listOf(ScaleAndRotateTransformation.Builder().setRotationDegrees(-90f * (edit.rotationSteps % 4)).build())
        } else {
            emptyList()
        }
        val edited = EditedMediaItem.Builder(item)
            .setRemoveAudio(edit.mute)
            .setEffects(Effects(emptyList(), videoEffects))
            .build()
        val transformer = Transformer.Builder(getApplication<Application>())
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    if (cont.isActive) cont.resume(true)
                }

                override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                    if (cont.isActive) cont.resume(false)
                }
            })
            .build()
        cont.invokeOnCancellation { transformer.cancel() }
        transformer.start(edited, output.absolutePath)
    }
}
