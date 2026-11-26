// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.features.photos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.helixgallery.core.datastore.HelixPreferences
import com.helixgallery.core.model.GridLayout
import com.helixgallery.core.model.MediaState
import com.helixgallery.core.model.UserPreferences
import com.helixgallery.data.media.MediaData
import com.helixgallery.data.media.MediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Estado de uma lista de fotos (a aba Fotos ou um álbum/coleção): mídia, preferências e seleção. */
class CollectionViewModel(application: Application) : AndroidViewModel(application) {

    val repository: MediaRepository = MediaData.repository(application)
    private val prefs = HelixPreferences.repository(application)

    val media: StateFlow<MediaState> = repository.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MediaState())

    val preferences: StateFlow<UserPreferences> = prefs.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserPreferences())

    private val _selected = MutableStateFlow<Set<Long>>(emptySet())
    val selected: StateFlow<Set<Long>> = _selected.asStateFlow()

    fun toggle(id: Long) {
        _selected.value = if (id in _selected.value) _selected.value - id else _selected.value + id
    }

    fun selectAll(ids: Collection<Long>) {
        _selected.value = ids.toSet()
    }

    fun clearSelection() {
        _selected.value = emptySet()
    }

    fun setLayout(layout: GridLayout) {
        viewModelScope.launch { prefs.setGridLayout(layout) }
    }

    fun setColumns(columns: Int) {
        viewModelScope.launch { prefs.setColumns(columns) }
    }
}
