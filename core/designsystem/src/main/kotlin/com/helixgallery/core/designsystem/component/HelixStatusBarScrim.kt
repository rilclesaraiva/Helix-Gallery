// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush

/** Opacidade do escurecimento no topo da tela (some em degradê para baixo). */
private const val SCRIM_ALPHA = 0.45f

/**
 * Faixa levemente escurecida sob a barra de status: as fotos que rolam por trás dela não
 * atrapalham a leitura do relógio e dos ícones. É só desenho: não pega toque.
 */
@Composable
fun HelixStatusBarScrim(modifier: Modifier = Modifier) {
    val scrim = MaterialTheme.colorScheme.scrim
    Box(
        modifier
            .fillMaxWidth()
            .windowInsetsTopHeight(WindowInsets.statusBars)
            .background(
                Brush.verticalGradient(
                    listOf(scrim.copy(alpha = SCRIM_ALPHA), scrim.copy(alpha = SCRIM_ALPHA * 0.5f), scrim.copy(alpha = 0f)),
                ),
            ),
    )
}
