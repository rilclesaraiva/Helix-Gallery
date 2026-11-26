// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.core.designsystem.component

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.SharedFlow

/**
 * Tocar de novo na aba que já está aberta: o `:app` emite o nome da aba aqui e a lista dela
 * sobe até o topo ([ScrollToTopOnReselect]). Mesmo modelo do BoomListener.
 */
val LocalTabReselect = staticCompositionLocalOf<SharedFlow<String>?> { null }

/** Sobe [state] até o topo quando a aba [tab] é tocada de novo, num movimento só e suave. */
@Composable
fun ScrollToTopOnReselect(tab: String, state: LazyGridState) {
    val reselect = LocalTabReselect.current ?: return
    LaunchedEffect(reselect, state, tab) {
        reselect.collect { which ->
            if (which != tab) return@collect
            // De muito longe, chega perto de uma vez e só o resto anima (sem paradas no caminho).
            if (state.firstVisibleItemIndex > NEAR_TOP_ITEMS) state.scrollToItem(NEAR_TOP_ITEMS)
            state.animateScrollToItem(0)
        }
    }
}

private const val NEAR_TOP_ITEMS = 12
