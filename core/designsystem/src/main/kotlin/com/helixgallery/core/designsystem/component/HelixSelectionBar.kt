// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.helixgallery.core.designsystem.R
import com.helixgallery.core.designsystem.theme.helixEffectsSpec
import com.helixgallery.core.designsystem.theme.helixSpatialSpec

/** O :app dá um estado aqui; a barra de seleção o liga, e a barra de abas de baixo desce enquanto há seleção. */
val LocalSelectionBarActive = compositionLocalOf<MutableState<Boolean>?> { null }

/**
 * Controles flutuantes no topo enquanto há itens marcados (porte da barra do BoomListener): à esquerda uma
 * pílula com fechar e quantos são; à direita outra com "selecionar tudo" e o ⋮ com as [actions]. Sem faixa
 * de fundo: a página continua visível por trás. Entra deslizando de cima e sai do mesmo jeito.
 */
@Composable
fun HelixSelectionBar(
    visible: Boolean,
    count: Int,
    allSelected: Boolean,
    onClose: () -> Unit,
    onToggleAll: (selectAll: Boolean) -> Unit,
    actions: List<HelixMenuAction>,
    modifier: Modifier = Modifier,
) {
    val barActive = LocalSelectionBarActive.current
    DisposableEffect(barActive, visible) {
        barActive?.value = visible
        onDispose { barActive?.value = false }
    }
    // Durante a saída da barra o número não pode zerar antes de ela sumir.
    var shownCount by remember { mutableIntStateOf(0) }
    if (count > 0) shownCount = count

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically(helixSpatialSpec()) { -it } + fadeIn(helixEffectsSpec()),
        exit = slideOutVertically(helixSpatialSpec()) { -it } + fadeOut(helixEffectsSpec()),
    ) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHighest, shadowElevation = 3.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.ds_close))
                    }
                    Text(
                        text = shownCount.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(end = 20.dp, start = 4.dp),
                    )
                }
            }
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHighest, shadowElevation = 3.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconToggleButton(checked = allSelected, onCheckedChange = onToggleAll) {
                        Icon(
                            HelixIcons.SelectAll,
                            contentDescription = stringResource(R.string.ds_select_all),
                            tint = if (allSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    if (actions.isNotEmpty()) HelixMoreMenu(actions = actions, circle = false)
                }
            }
        }
    }
}

/**
 * Título da lista que se recolhe quando há itens marcados (sobe e esmaece, como a barra de abas desce):
 * assim as pílulas ficam livres, sem faixa de fundo atrás delas. Recolhido, não recebe toques.
 */
@Composable
fun Modifier.recedeWhileSelecting(active: Boolean): Modifier {
    val fraction by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = helixSpatialSpec(),
        label = "recedeWhileSelecting",
    )
    val extra = with(LocalDensity.current) { 120.dp.toPx() }
    return this
        .pointerInput(active) {
            if (active) {
                awaitPointerEventScope {
                    while (true) awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                }
            }
        }
        .graphicsLayer {
            val f = fraction.coerceIn(0f, 1f)
            translationY = -f * (size.height + extra)
            alpha = 1f - f
        }
}
