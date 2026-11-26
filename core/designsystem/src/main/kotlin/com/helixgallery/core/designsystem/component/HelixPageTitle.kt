// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Título que faz parte da página (primeira linha da lista), no lugar de uma barra presa
 * na tela: fica no topo e sobe junto ao rolar. O voltar fica parado no canto
 * ([HelixFloatingBackButton]) e o título começa mais para o lado ([HelixBackButtonInset]),
 * para o botão nunca ficar por cima dele, nem ao rolar.
 *
 * @param hasBack a tela tem voltar: o título abre espaço para o botão.
 * @param large título grande embaixo da linha do voltar (Ajustes).
 * @param statusBarInset desce o título para baixo da faixa das notificações.
 */
@Composable
fun HelixPageTitle(
    title: String,
    modifier: Modifier = Modifier,
    hasBack: Boolean = false,
    large: Boolean = false,
    statusBarInset: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val start = if (hasBack) HelixBackButtonInset else 16.dp
    val insets = if (statusBarInset) modifier.statusBarsPadding() else modifier
    Column(insets.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().height(64.dp).padding(start = start, end = 4.dp),
        ) {
            if (!large) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
            actions()
        }
        if (large) {
            Text(
                title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = start, end = 16.dp, top = 8.dp, bottom = 20.dp),
            )
        }
    }
}

/**
 * Tela de dentro (Ajustes, Sobre…): título grande rolando com o conteúdo e o botão
 * de voltar translúcido parado no canto.
 */
@Composable
fun HelixSubPage(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding(),
        ) {
            HelixPageTitle(title = title, hasBack = true, large = true)
            content()
            Spacer(Modifier.height(24.dp))
        }
        HelixFloatingBackButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart))
    }
}

/** Margem padrão das listas para não ficarem sob a barra flutuante de navegação. */
val FloatingNavPadding: PaddingValues = PaddingValues(bottom = 112.dp)
