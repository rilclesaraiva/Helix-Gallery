// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.features.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.helixgallery.core.datastore.HelixPreferences
import com.helixgallery.core.model.GridLayout
import com.helixgallery.core.model.ThemeMode
import com.helixgallery.core.model.UserPreferences
import com.helixgallery.data.updates.UpdateCheckResult
import com.helixgallery.data.updates.UpdatesData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal sealed interface UpdatesUiState {
    data object Idle : UpdatesUiState
    data object Checking : UpdatesUiState
    data class Done(val result: UpdateCheckResult) : UpdatesUiState
}

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = HelixPreferences.repository(application)
    private val updates = UpdatesData.repository(application)

    val preferences: StateFlow<UserPreferences> = prefs.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserPreferences())

    private val _updateState = MutableStateFlow<UpdatesUiState>(UpdatesUiState.Idle)
    internal val updateState: StateFlow<UpdatesUiState> = _updateState.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { prefs.setThemeMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { prefs.setDynamicColor(enabled) }
    }

    fun setGridLayout(layout: GridLayout) {
        viewModelScope.launch { prefs.setGridLayout(layout) }
    }

    fun setColumns(columns: Int) {
        viewModelScope.launch { prefs.setColumns(columns) }
    }

    /** "Verificar atualizações": só consulta o GitHub quando a pessoa toca. */
    fun checkForUpdates() {
        if (_updateState.value == UpdatesUiState.Checking) return
        _updateState.value = UpdatesUiState.Checking
        viewModelScope.launch {
            _updateState.value = UpdatesUiState.Done(updates.checkForUpdates())
        }
    }
}
