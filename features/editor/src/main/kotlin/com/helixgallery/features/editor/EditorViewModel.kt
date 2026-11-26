// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.features.editor

import android.app.Application
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.helixgallery.data.media.MediaWriterData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal sealed interface EditorLoad {
    data object Loading : EditorLoad
    data object Ready : EditorLoad
    data object Failed : EditorLoad
}

private const val MAX_SOURCE_SIDE = 4096
private const val PREVIEW_SIDE = 1280

internal class EditorViewModel(application: Application) : AndroidViewModel(application) {
    private val writer = MediaWriterData.writer(application)

    private val _load = MutableStateFlow<EditorLoad>(EditorLoad.Loading)
    val load: StateFlow<EditorLoad> = _load.asStateFlow()

    private val _params = MutableStateFlow(EditParams())
    val params: StateFlow<EditParams> = _params.asStateFlow()

    private val _preview = MutableStateFlow<ImageBitmap?>(null)
    val preview: StateFlow<ImageBitmap?> = _preview.asStateFlow()

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    private var source: Bitmap? = null
    private var previewBase: Bitmap? = null
    private var started = false

    fun open(uri: String) {
        if (started) return
        started = true
        viewModelScope.launch {
            val bitmap = withContext(Dispatchers.IO) { decode(Uri.parse(uri)) }
            if (bitmap == null) {
                _load.value = EditorLoad.Failed
                return@launch
            }
            source = bitmap
            previewBase = withContext(Dispatchers.Default) { renderEdit(bitmap, EditParams(), PREVIEW_SIDE) }
            _load.value = EditorLoad.Ready
            // Cada mudança refaz a prévia; se vier outra no meio, a antiga é descartada.
            _params.collectLatest { p ->
                val base = previewBase ?: return@collectLatest
                _preview.value = withContext(Dispatchers.Default) { renderEdit(base, p).asImageBitmap() }
            }
        }
    }

    private fun decode(uri: Uri): Bitmap? = try {
        val source = ImageDecoder.createSource(getApplication<Application>().contentResolver, uri)
        ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val longest = maxOf(info.size.width, info.size.height)
            if (longest > MAX_SOURCE_SIDE) {
                val scale = MAX_SOURCE_SIDE.toFloat() / longest
                decoder.setTargetSize((info.size.width * scale).toInt(), (info.size.height * scale).toInt())
            }
        }
    } catch (_: Exception) {
        null
    }

    fun update(change: (EditParams) -> EditParams) {
        _params.value = change(_params.value)
    }

    fun reset() {
        _params.value = EditParams()
    }

    /** Salva uma cópia na galeria (o original não muda). [onDone] recebe se deu certo. */
    fun save(displayName: String, relativePath: String, takenMs: Long, onDone: (Boolean) -> Unit) {
        val bitmap = source ?: return onDone(false)
        if (_saving.value) return
        viewModelScope.launch {
            _saving.value = true
            val ok = withContext(Dispatchers.Default) {
                val result = renderEdit(bitmap, _params.value)
                writer.saveJpeg(result, displayName, relativePath, takenMs)
            }
            _saving.value = false
            onDone(ok)
        }
    }
}
