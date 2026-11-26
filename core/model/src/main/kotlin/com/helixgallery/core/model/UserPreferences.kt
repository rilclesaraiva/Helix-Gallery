// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.core.model

/** Tema do app: o do sistema, claro, escuro ou AMOLED (escuro com fundos pretos). */
enum class ThemeMode { System, Light, Dark, Amoled }

/** Colunas que a grade aceita (Ajustes → Aparência). */
val GRID_COLUMN_OPTIONS: List<Int> = listOf(3, 4, 5)

/**
 * Escolhas da pessoa. O padrão é o tema escuro com as cores próprias do Helix
 * (a cor dinâmica do papel de parede é opção).
 */
data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.Dark,
    val dynamicColor: Boolean = false,
    val gridLayout: GridLayout = GridLayout.Day,
    val columns: Int = 4,
    /** Ferramentas fixadas no topo da aba Álbuns, na ordem em que aparecem. */
    val albumTools: List<AlbumTool> = DEFAULT_ALBUM_TOOLS,
)
