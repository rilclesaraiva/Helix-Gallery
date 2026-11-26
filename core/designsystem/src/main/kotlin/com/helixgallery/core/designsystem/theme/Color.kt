// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/*
 * Cores próprias do Helix, tiradas da Nebulosa da Hélice: azul-noite do espaço,
 * anel ciano, miolo âmbar e toques de magenta.
 */

internal val HelixDarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFF7FD7E8),
    onPrimary = Color(0xFF00363F),
    primaryContainer = Color(0xFF004E5B),
    onPrimaryContainer = Color(0xFFA8EEFF),
    secondary = Color(0xFFFFB77C),
    onSecondary = Color(0xFF4A2800),
    secondaryContainer = Color(0xFF6A3B00),
    onSecondaryContainer = Color(0xFFFFDCC2),
    tertiary = Color(0xFFFFADEE),
    onTertiary = Color(0xFF5A0F55),
    tertiaryContainer = Color(0xFF76296E),
    onTertiaryContainer = Color(0xFFFFD6F5),
    background = Color(0xFF0B1220),
    onBackground = Color(0xFFE3E8F2),
    surface = Color(0xFF0B1220),
    onSurface = Color(0xFFE3E8F2),
    surfaceVariant = Color(0xFF3E4857),
    onSurfaceVariant = Color(0xFFBEC8D8),
    outline = Color(0xFF8892A3),
    outlineVariant = Color(0xFF3E4857),
    surfaceContainerLowest = Color(0xFF070C16),
    surfaceContainerLow = Color(0xFF101827),
    surfaceContainer = Color(0xFF141C2C),
    surfaceContainerHigh = Color(0xFF1E2737),
    surfaceContainerHighest = Color(0xFF293242),
)

internal val HelixLightColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF006878),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFA8EEFF),
    onPrimaryContainer = Color(0xFF001F26),
    secondary = Color(0xFF8B4F00),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDCC2),
    onSecondaryContainer = Color(0xFF2D1600),
    tertiary = Color(0xFF93418A),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD6F5),
    onTertiaryContainer = Color(0xFF3A0036),
    background = Color(0xFFF5FAFC),
    onBackground = Color(0xFF171C1F),
    surface = Color(0xFFF5FAFC),
    onSurface = Color(0xFF171C1F),
    surfaceVariant = Color(0xFFDAE4E8),
    onSurfaceVariant = Color(0xFF3E484C),
    outline = Color(0xFF6E797D),
    outlineVariant = Color(0xFFBEC8CC),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEFF4F7),
    surfaceContainer = Color(0xFFE9EFF1),
    surfaceContainerHigh = Color(0xFFE3E9EC),
    surfaceContainerHighest = Color(0xFFDDE3E6),
)

/** AMOLED: o mesmo esquema escuro, mas com fundos pretos de verdade. */
internal fun ColorScheme.toAmoled(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0A0A0A),
    surfaceContainer = Color(0xFF121212),
    surfaceContainerHigh = Color(0xFF1A1A1A),
    surfaceContainerHighest = Color(0xFF222222),
)
