// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.helixgallery.core.designsystem.theme.helixEffectsSpec
import com.helixgallery.core.model.GridEntry
import com.helixgallery.core.model.GridLayout
import com.helixgallery.core.model.epochDay
import kotlinx.coroutines.delay

/**
 * Pílula no topo da tela com a data da foto que está no alto da grade, enquanto ela rola (ou a rolagem rápida
 * é arrastada), como no Google Fotos. Aparece só depois do topo da página ([headerCount] itens antes das fotos)
 * e some um pouco depois de parar. A grade da aba Fotos não tem títulos de data: é a pílula que mostra o dia.
 */
@Composable
fun GridDatePill(
    state: LazyGridState,
    entries: List<GridEntry>,
    headerCount: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    /** Mantém a pílula à mostra mesmo sem rolar (o dedo parado na bolinha da rolagem rápida). */
    forceShown: Boolean = false,
) {
    val isScrolling by remember { derivedStateOf { state.isScrollInProgress } }
    val pastHeader by remember(entries) { derivedStateOf { state.firstVisibleItemIndex >= headerCount && entries.isNotEmpty() } }
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(isScrolling, forceShown) {
        if (isScrolling || forceShown) {
            shown = true
        } else {
            delay(HIDE_AFTER_MS)
            shown = false
        }
    }
    val epoch by remember(entries) {
        derivedStateOf { entries.getOrNull((state.firstVisibleItemIndex - headerCount).coerceIn(0, maxOf(0, entries.lastIndex)))?.epochDay() }
    }
    // Guarda o último dia: ao sumir, a pílula não troca de texto no meio do desaparecer.
    var lastEpoch by remember { mutableStateOf(0L) }
    epoch?.let { lastEpoch = it }
    val text = rememberHeaderText(GridEntry.Header(lastEpoch, GridLayout.Day))
    Box(modifier.statusBarsPadding().padding(top = 12.dp), contentAlignment = Alignment.TopCenter) {
        AnimatedVisibility(
            visible = enabled && shown && pastHeader && epoch != null,
            enter = fadeIn(helixEffectsSpec()),
            exit = fadeOut(helixEffectsSpec()),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = MaterialTheme.shapes.extraLarge,
                shadowElevation = 4.dp,
            ) {
                Text(
                    text,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                )
            }
        }
    }
}

private const val HIDE_AFTER_MS = 1_200L
