// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.core.designsystem.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Barra de baixo com duas ações em texto, de ponta a ponta: a primeira normal (ex.: Restaurar) e a
 * segunda destrutiva, em vermelho (ex.: Excluir). Usada na lixeira e no cofre, na seleção e dentro da foto.
 * Quem usa cuida de aparecer e sumir com animação.
 */
@Composable
fun HelixBottomActionBar(
    primaryLabel: String,
    onPrimary: () -> Unit,
    destructiveLabel: String,
    onDestructive: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = modifier.fillMaxWidth()) {
        Row(Modifier.navigationBarsPadding().padding(horizontal = 8.dp, vertical = 4.dp)) {
            TextButton(onClick = onPrimary, modifier = Modifier.weight(1f)) {
                Text(primaryLabel, style = MaterialTheme.typography.titleMedium)
            }
            TextButton(
                onClick = onDestructive,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Text(destructiveLabel, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
