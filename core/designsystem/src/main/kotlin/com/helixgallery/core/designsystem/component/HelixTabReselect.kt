// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.core.designsystem.component

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import com.helixgallery.core.designsystem.theme.helixScreenSpec
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
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
    val spec = helixScreenSpec<Float>()
    LaunchedEffect(reselect, state, tab) {
        reselect.collect { which ->
            if (which != tab) return@collect
            try {
                state.scrollToTopSmoothly(spec)
            } catch (e: CancellationException) {
                // Tocar na lista no meio da subida cancela só a animação (a rolagem do dedo tem prioridade).
                // Sem isto, o cancelamento derrubava também a escuta da aba, e ela deixava de subir ao topo
                // depois de usada demais. Se foi a própria escuta que acabou (saiu da aba), ensureActive() relança.
                currentCoroutineContext().ensureActive()
            }
        }
    }
}

/**
 * Sobe até o topo num movimento só, sem paradas no caminho (porte do `scrollToTopSmoothly` do BoomListener).
 *
 * `animateScrollToItem` com o alvo fora da tela anda em etapas (chuta uma distância, mede, corrige), e cada
 * etapa vira uma travadinha. Aqui a distância é conhecida de antemão: se o topo está à vista, é a posição
 * dele; se não, a grade pula (sem animação) para exatamente uma tela abaixo do topo e anima esses pixels de
 * uma vez, com uma mola só.
 */
suspend fun LazyGridState.scrollToTopSmoothly(spec: AnimationSpec<Float>) {
    val viewport = layoutInfo.viewportSize.height
    val firstVisible = layoutInfo.visibleItemsInfo.firstOrNull { it.index == 0 }
    if (firstVisible != null) {
        val distance = -firstVisible.offset.y
        if (distance > 0) animateScrollBy(-distance.toFloat(), spec)
        scrollToItem(0)
        return
    }
    if (viewport <= 0) {
        scrollToItem(0)
        return
    }
    scrollToItem(0, viewport)
    // A grade acaba antes (pouco conteúdo): o pulo foi encurtado. Mede de novo.
    val item0 = layoutInfo.visibleItemsInfo.firstOrNull { it.index == 0 }
    val distance = when {
        item0 != null -> -item0.offset.y
        firstVisibleItemIndex == 0 -> firstVisibleItemScrollOffset
        else -> viewport
    }
    if (distance > 0) animateScrollBy(-distance.toFloat(), spec)
    // Garante o topo exato (arredondamentos da mola).
    scrollToItem(0)
}
