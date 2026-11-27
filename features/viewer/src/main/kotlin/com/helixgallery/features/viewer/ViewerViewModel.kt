// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.features.viewer

import android.app.Application
import android.media.ExifInterface
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.helixgallery.core.model.MediaState
import com.helixgallery.data.media.MediaData
import com.helixgallery.core.model.MediaItem
import com.helixgallery.data.media.MediaRepository
import com.helixgallery.data.media.MediaWriterData
import com.helixgallery.data.vault.VaultData
import com.helixgallery.data.vault.VaultSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** O EXIF lido de [uri] (null em [info] = a foto não tem ou não foi possível ler). */
data class ExifResult(val uri: String, val info: ExifInfo?)

/** Dados da câmera lidos do próprio arquivo (EXIF). Campos que a foto não tem ficam nulos. */
data class ExifInfo(
    val camera: String? = null,
    val aperture: String? = null,
    val exposure: String? = null,
    val iso: String? = null,
    val focalLength: String? = null,
)

class ViewerViewModel(application: Application) : AndroidViewModel(application) {

    val repository: MediaRepository = MediaData.repository(application)

    val media: StateFlow<MediaState> = repository.state

    private val writer = MediaWriterData.writer(application)
    private val vault = VaultData.repository(application)

    /**
     * Tranca [item] no cofre sem pedir a senha. [onResult]: true = guardou (agora o original pode sair),
     * false = não deu, null = ainda não dá sem abrir o cofre (quem chama segue o caminho normal).
     */
    fun lockToVault(item: MediaItem, onResult: (Boolean?) -> Unit) {
        viewModelScope.launch {
            val result = try {
                vault.addQuietly(listOf(VaultSource(item.uri, item.displayName, item.relativePath, item.takenMs)))
            } catch (_: Exception) {
                onResult(false)
                return@launch
            }
            onResult(result?.let { item.uri in it.stored })
        }
    }

    /** Copia [item] para a pasta [relativePath] (o original fica). [onDone] recebe se deu certo. */
    fun copyTo(item: MediaItem, relativePath: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = writer.copyToFolder(
                sourceUri = item.uri,
                displayName = item.displayName,
                mimeType = item.mimeType,
                isVideo = item.isVideo,
                relativePath = relativePath,
                takenMs = item.takenMs,
            )
            onDone(ok)
        }
    }

    private val _exif = MutableStateFlow<ExifResult?>(null)
    val exif: StateFlow<ExifResult?> = _exif.asStateFlow()
    private var exifJob: Job? = null

    /** Lê o EXIF da foto aberta nos detalhes (vídeos não têm). */
    fun loadExif(uri: String, isVideo: Boolean) {
        // Uma leitura por vez e sempre ligada à foto de que veio: o resultado de uma foto nunca aparece em outra.
        exifJob?.cancel()
        _exif.value = null
        if (isVideo) return
        exifJob = viewModelScope.launch {
            val info = withContext(Dispatchers.IO) { readExif(uri) }
            _exif.value = ExifResult(uri, info)
        }
    }

    @Suppress("DEPRECATION")
    private fun readExif(uri: String): ExifInfo? = try {
        getApplication<Application>().contentResolver.openInputStream(Uri.parse(uri))?.use { input ->
            val exif = ExifInterface(input)
            val make = exif.getAttribute(ExifInterface.TAG_MAKE)?.trim().orEmpty()
            val model = exif.getAttribute(ExifInterface.TAG_MODEL)?.trim().orEmpty()
            val camera = when {
                model.isEmpty() -> make.ifEmpty { null }
                make.isNotEmpty() && !model.startsWith(make, ignoreCase = true) -> "$make $model"
                else -> model
            }
            ExifInfo(
                camera = camera,
                aperture = exif.getAttribute(ExifInterface.TAG_F_NUMBER)?.let { "f/$it" },
                exposure = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)?.toDoubleOrNull()?.let { formatExposure(it) },
                iso = exif.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS)?.let { "ISO $it" },
                focalLength = exif.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH, 0.0)
                    .takeIf { it > 0 }?.let { "%.0f mm".format(it) },
            )
        }
    } catch (_: Exception) {
        null
    }
}

/** 0,004 s → "1/250 s"; 2 s → "2 s". */
internal fun formatExposure(seconds: Double): String =
    if (seconds in 0.0..0.5 && seconds > 0) "1/${Math.round(1.0 / seconds)} s" else "%.1f s".format(seconds)
