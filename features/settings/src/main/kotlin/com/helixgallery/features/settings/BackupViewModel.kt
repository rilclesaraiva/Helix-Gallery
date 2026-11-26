// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.features.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.helixgallery.data.media.MediaBackupData
import com.helixgallery.data.media.MediaData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal sealed interface BackupUi {
    data object Idle : BackupUi
    data class Exporting(val done: Int, val total: Int) : BackupUi
    data object Importing : BackupUi
    data class Exported(val count: Int) : BackupUi
    data class Imported(val imported: Int, val skipped: Int) : BackupUi
    data object Failed : BackupUi

    val busy: Boolean get() = this is Exporting || this is Importing
}

/** Backup em ZIP para um lugar que a pessoa escolhe no seletor de arquivos do Android. */
internal class BackupViewModel(application: Application) : AndroidViewModel(application) {
    private val backup = MediaBackupData.backup(application)
    private val media = MediaData.repository(application)

    private val _state = MutableStateFlow<BackupUi>(BackupUi.Idle)
    val state: StateFlow<BackupUi> = _state.asStateFlow()

    fun export(target: Uri) {
        if (_state.value.busy) return
        viewModelScope.launch {
            _state.value = BackupUi.Exporting(0, 0)
            _state.value = try {
                val items = media.state.first { it.loaded }.items
                val out = getApplication<Application>().contentResolver.openOutputStream(target, "wt") ?: error("Cannot open")
                val count = backup.export(items, out) { done, total -> _state.value = BackupUi.Exporting(done, total) }
                BackupUi.Exported(count)
            } catch (_: Exception) {
                BackupUi.Failed
            }
        }
    }

    fun import(source: Uri) {
        if (_state.value.busy) return
        viewModelScope.launch {
            _state.value = BackupUi.Importing
            _state.value = try {
                val input = getApplication<Application>().contentResolver.openInputStream(source) ?: error("Cannot open")
                val result = backup.import(input) { }
                BackupUi.Imported(result.imported, result.skipped)
            } catch (_: Exception) {
                BackupUi.Failed
            }
        }
    }
}
