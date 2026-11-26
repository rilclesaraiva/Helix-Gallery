// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.helixgallery.core.designsystem.R

/**
 * Uma opção do menu de três pontinhos. [destructive] (apagar) fica em vermelho; o app põe
 * as opções das mais usadas para as mais raras e a destrutiva por último, longe do dedo.
 * [selected] marca a escolha atual (ex.: o layout da grade) com um visto no fim da linha.
 */
data class HelixMenuAction(
    val label: String,
    val icon: ImageVector,
    val enabled: Boolean = true,
    val destructive: Boolean = false,
    val selected: Boolean = false,
    val onClick: () -> Unit,
)

/**
 * Botão de três pontinhos que abre o [HelixMenuSheet] (painel emergente, no lugar de um
 * menu flutuante). [header] é o nome do que o menu trata (opcional).
 */
@Composable
fun HelixMoreMenu(
    actions: List<HelixMenuAction>,
    modifier: Modifier = Modifier,
    header: String? = null,
) {
    var open by remember { mutableStateOf(false) }
    HelixFloatingCircle(modifier) {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.ds_more))
        }
    }
    if (open) HelixMenuSheet(actions = actions, onDismiss = { open = false }, header = header)
}

/** O painel do menu: sobe de baixo, com as opções (ícone + texto) em lista. Tocar numa opção a faz e fecha descendo. */
@Composable
fun HelixMenuSheet(
    actions: List<HelixMenuAction>,
    onDismiss: () -> Unit,
    header: String? = null,
) {
    HelixModalSheet(onDismiss = onDismiss, containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        val sheet = this
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            if (header != null) {
                Text(
                    header,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 16.dp),
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(8.dp))
            }
            actions.forEach { action ->
                val tint = if (action.destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = action.enabled) {
                            action.onClick()
                            sheet.dismiss()
                        }
                        .padding(horizontal = 24.dp, vertical = 14.dp)
                        .alpha(if (action.enabled) 1f else 0.38f),
                ) {
                    Icon(action.icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(20.dp))
                    Text(
                        action.label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (action.destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    if (action.selected) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
