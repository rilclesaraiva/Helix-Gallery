// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.features.cleanup

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.helixgallery.core.model.MediaItem
import com.helixgallery.core.model.MediaState
import com.helixgallery.core.model.duplicateCandidates
import com.helixgallery.core.model.exactDuplicateGroups
import com.helixgallery.data.media.MediaData
import com.helixgallery.data.media.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest

internal sealed interface DuplicateScan {
    data object Idle : DuplicateScan
    data class Scanning(val done: Int, val total: Int) : DuplicateScan
    data class Done(val groups: List<List<MediaItem>>) : DuplicateScan
}

internal class CleanupViewModel(application: Application) : AndroidViewModel(application) {
    val repository: MediaRepository = MediaData.repository(application)

    val media: StateFlow<MediaState> = repository.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MediaState())

    private val _scan = MutableStateFlow<DuplicateScan>(DuplicateScan.Idle)
    val scan: StateFlow<DuplicateScan> = _scan.asStateFlow()
    private var job: Job? = null

    /** Procura cópias idênticas: filtra por tamanho e dimensões e confirma lendo o conteúdo (SHA-256). Nada sai do aparelho. */
    fun scanDuplicates(items: List<MediaItem>) {
        if (job?.isActive == true) return
        job = viewModelScope.launch {
            val candidates = duplicateCandidates(items)
            val total = candidates.sumOf { it.size }
            var done = 0
            _scan.value = DuplicateScan.Scanning(0, total)
            val hashes = HashMap<Long, String?>()
            withContext(Dispatchers.IO) {
                for (item in candidates.flatten()) {
                    hashes[item.id] = hashOf(item.uri)
                    done++
                    _scan.value = DuplicateScan.Scanning(done, total)
                }
            }
            _scan.value = DuplicateScan.Done(exactDuplicateGroups(candidates) { hashes[it.id] })
        }
    }

    /** Depois de apagar algo, a próxima busca começa do zero. */
    fun resetScan() {
        job?.cancel()
        _scan.value = DuplicateScan.Idle
    }

    private fun hashOf(uri: String): String? = try {
        val digest = MessageDigest.getInstance("SHA-256")
        getApplication<Application>().contentResolver.openInputStream(Uri.parse(uri))?.use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        }
    } catch (_: Exception) {
        null
    }
}
