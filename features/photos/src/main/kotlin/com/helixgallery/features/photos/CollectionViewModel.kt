// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.features.photos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.helixgallery.core.datastore.HelixPreferences
import com.helixgallery.core.model.GridLayout
import com.helixgallery.core.model.GridZoom
import com.helixgallery.core.model.MediaState
import com.helixgallery.core.model.UserPreferences
import com.helixgallery.core.model.Story
import com.helixgallery.core.model.buildStories
import com.helixgallery.core.model.nextGridZoom
import com.helixgallery.data.media.MediaData
import com.helixgallery.data.media.MediaRepository
import com.helixgallery.data.vault.VaultData
import com.helixgallery.data.vault.VaultSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Estado de uma lista de fotos (a aba Fotos ou um álbum/coleção): mídia, preferências e seleção. */
class CollectionViewModel(application: Application) : AndroidViewModel(application) {

    val repository: MediaRepository = MediaData.repository(application)
    private val prefs = HelixPreferences.repository(application)
    private val vault = VaultData.repository(application)

    /**
     * Tranca [items] no cofre sem pedir a senha. [onResult] recebe os endereços que entraram (os que podem sair
     * da galeria), ou null se ainda não dá sem abrir o cofre.
     */
    fun lockToVault(items: List<com.helixgallery.core.model.MediaItem>, onResult: (stored: List<String>?, failed: Int) -> Unit) {
        viewModelScope.launch {
            val result = try {
                vault.addQuietly(items.map { VaultSource(it.uri, it.displayName, it.relativePath, it.takenMs) })
            } catch (_: Exception) {
                onResult(emptyList(), items.size)
                return@launch
            }
            onResult(result?.stored, result?.failed ?: 0)
        }
    }

    val media: StateFlow<MediaState> = repository.state

    val preferences: StateFlow<UserPreferences> = prefs.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserPreferences())

    // As lembranças: calculadas fora do fio principal e refeitas quando a mídia ou o dia mudam.
    private val today = MutableStateFlow(LocalDate.now())

    /** Chamado ao voltar para a tela: na virada do dia, "neste dia" muda. */
    fun refreshToday() {
        today.value = LocalDate.now()
    }

    val stories: StateFlow<List<Story>> = combine(repository.state, today) { state, day -> state.items to day }
        .map { (items, day) -> buildStories(items, day) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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

    /** Pinça na grade: afasta ([zoomOut]) ou aproxima um passo (layout e colunas juntos; só colunas se [regularOnly]). */
    fun zoomStep(zoomOut: Boolean, regularOnly: Boolean = false) {
        val current = preferences.value
        if (regularOnly) {
            // A aba Fotos é sempre corrida: a pinça só muda as colunas e não mexe no layout dos álbuns.
            val columns = nextGridZoom(GridZoom(GridLayout.Regular, current.columns), zoomOut).columns
            if (columns != current.columns) viewModelScope.launch { prefs.setColumns(columns) }
            return
        }
        val next = nextGridZoom(GridZoom(current.gridLayout, current.columns), zoomOut)
        viewModelScope.launch {
            if (next.layout != current.gridLayout) prefs.setGridLayout(next.layout)
            if (next.columns != current.columns) prefs.setColumns(next.columns)
        }
    }

    fun setColumns(columns: Int) {
        viewModelScope.launch { prefs.setColumns(columns) }
    }
}
