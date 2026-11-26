// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.helixgallery.core.model.GridLayout
import com.helixgallery.core.model.ThemeMode
import com.helixgallery.core.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.helixDataStore: DataStore<Preferences> by preferencesDataStore(name = "helix_preferences")

/** Guarda as escolhas da pessoa no aparelho (tema, grade). Nada sai daqui. */
class PreferencesRepository internal constructor(private val store: DataStore<Preferences>) {

    val preferences: Flow<UserPreferences> = store.data.map { prefs ->
        val defaults = UserPreferences()
        UserPreferences(
            themeMode = prefs[THEME]?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } }
                ?: defaults.themeMode,
            dynamicColor = prefs[DYNAMIC] ?: defaults.dynamicColor,
            gridLayout = prefs[LAYOUT]?.let { name -> GridLayout.entries.firstOrNull { it.name == name } }
                ?: defaults.gridLayout,
            columns = prefs[COLUMNS] ?: defaults.columns,
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[THEME] = mode.name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        store.edit { it[DYNAMIC] = enabled }
    }

    suspend fun setGridLayout(layout: GridLayout) {
        store.edit { it[LAYOUT] = layout.name }
    }

    suspend fun setColumns(columns: Int) {
        store.edit { it[COLUMNS] = columns }
    }

    private companion object {
        val THEME = stringPreferencesKey("theme_mode")
        val DYNAMIC = booleanPreferencesKey("dynamic_color")
        val LAYOUT = stringPreferencesKey("grid_layout")
        val COLUMNS = intPreferencesKey("grid_columns")
    }
}

/** Ponto único de acesso: um repositório por processo. */
object HelixPreferences {
    @Volatile
    private var instance: PreferencesRepository? = null

    fun repository(context: Context): PreferencesRepository =
        instance ?: synchronized(this) {
            instance ?: PreferencesRepository(context.applicationContext.helixDataStore).also { instance = it }
        }
}
