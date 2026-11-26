// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.core.designsystem.component

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** Altura máxima do painel, em fração da tela: sobra uma faixa em cima mostrando que ele é de puxar. */
private const val MAX_SHEET_FRACTION = 0.88f

/**
 * Painel que sobe de baixo (menus, detalhes, escolhas), usado no app inteiro no lugar do
 * `ModalBottomSheet` direto (modelo do BoomListener). O "voltar" é do app: o painel fecha
 * descendo, com a animação de sempre, e não encolhendo com a animação do sistema.
 * Para fechar descendo a partir de um botão do conteúdo, use [HelixSheetScope.dismiss].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelixModalSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = BottomSheetDefaults.ContainerColor,
    contentColor: Color = contentColorFor(containerColor),
    content: @Composable HelixSheetScope.() -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val dismiss by rememberUpdatedState(onDismiss)
    // Desce o painel e só então fecha.
    val hideThenDismiss: () -> Unit = { scope.launch { sheetState.hide() }.invokeOnCompletion { dismiss() } }
    ModalBottomSheet(
        onDismissRequest = { dismiss() },
        modifier = modifier,
        sheetState = sheetState,
        containerColor = containerColor,
        contentColor = contentColor,
        // Sem o voltar do M3 (e a animação de encolher do sistema): o voltar é o de baixo.
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = false),
    ) {
        BackHandler(onBack = hideThenDismiss)
        val maxHeight = LocalConfiguration.current.screenHeightDp.dp * MAX_SHEET_FRACTION
        Column(Modifier.heightIn(max = maxHeight).navigationBarsPadding()) {
            val sheetScope = remember(this) { HelixSheetScopeImpl(this, hideThenDismiss) }
            sheetScope.content()
        }
    }
}

/** O conteúdo do [HelixModalSheet]: uma coluna que também sabe fechar o painel descendo. */
interface HelixSheetScope : ColumnScope {
    /** Fecha o painel com a animação de descer (e chama o `onDismiss` no fim). */
    fun dismiss()
}

private class HelixSheetScopeImpl(column: ColumnScope, private val close: () -> Unit) :
    HelixSheetScope, ColumnScope by column {
    override fun dismiss() = close()
}
