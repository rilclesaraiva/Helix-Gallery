// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.core.designsystem.component

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.helixgallery.core.designsystem.R

/** Largura que o título deixa livre à esquerda para nunca ficar por baixo do [HelixBackButton]. */
val HelixBackButtonInset = 72.dp

/** Opacidade do fundo dos botões que flutuam sobre a página (a página aparece por trás). */
const val FLOATING_BUTTON_ALPHA = 0.9f

/** O "voltar" do app: a seta dentro de um círculo translúcido, igual em toda tela. */
@Composable
fun HelixBackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    HelixFloatingCircle(modifier) {
        IconButton(onClick = onClick) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.ds_back))
        }
    }
}

/** O [HelixBackButton] parado no canto de cima, abaixo da faixa das notificações. */
@Composable
fun HelixFloatingBackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    HelixBackButton(onClick, modifier.statusBarsPadding().padding(8.dp))
}

/** Círculo translúcido para botões que flutuam sobre a página (voltar, ⋮, favoritar…). */
@Composable
fun HelixFloatingCircle(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = FLOATING_BUTTON_ALPHA),
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = modifier,
        content = content,
    )
}
