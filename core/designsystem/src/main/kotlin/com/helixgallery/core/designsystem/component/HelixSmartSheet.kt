// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.core.designsystem.component

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.helixgallery.core.designsystem.R
import com.helixgallery.core.designsystem.theme.helixEffectsSpec
import com.helixgallery.core.designsystem.theme.helixSpatialSpec

/** Conteúdo de uma página aberta dentro do painel inteligente. */
typealias HelixPageContent = @Composable HelixMenuPageScope.() -> Unit

/** O que uma página aberta dentro do [HelixSmartSheet] pode pedir ao painel. */
interface HelixMenuPageScope {
    /** Volta à primeira tela do painel, sem fechá-lo. */
    fun back()

    /** Fecha o painel descendo. */
    fun dismiss()
}

/** O que a primeira tela do [HelixSmartSheet] pode pedir: o que uma página pede, mais abrir uma página. */
interface HelixSmartScope : HelixMenuPageScope {
    /** Abre [page] dentro do mesmo painel (a altura anima para o novo conteúdo; o voltar leva à primeira tela). */
    fun open(page: HelixPageContent)
}

/**
 * Painel inteligente (modelo do `BottomPanelFlow` do Intruphon, via BoomListener): um único painel com duas
 * camadas. A de fora anima a **altura**; a de dentro troca o **conteúdo**: o que está na tela sai correndo
 * para a direita e só então o novo entra, vindo do mesmo lado, em mola. Nas páginas há um botão de voltar
 * redondo no topo (e o voltar do sistema leva à primeira tela); na primeira tela o voltar fecha o painel.
 * Confirmação também é página ([HelixConfirmPage]), nunca um painel por cima de outro.
 */
@Composable
fun HelixSmartSheet(
    onDismiss: () -> Unit,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    root: @Composable HelixSmartScope.() -> Unit,
) {
    HelixModalSheet(onDismiss = onDismiss, containerColor = containerColor) {
        val sheet = this
        // O que a pessoa pediu (null = primeira tela) e o que está desenhado: ficam separados porque entre
        // o pedido e o desenho existe a animação de saída.
        var page by remember { mutableStateOf<HelixPageContent?>(null) }
        var shown by remember { mutableStateOf<HelixPageContent?>(null) }
        val currentSetPage by rememberUpdatedState<(HelixPageContent?) -> Unit> { page = it }
        val scope = remember(sheet) {
            object : HelixSmartScope {
                override fun open(page: HelixPageContent) = currentSetPage(page)
                override fun back() = currentSetPage(null)
                override fun dismiss() = sheet.dismiss()
            }
        }
        BackHandler(enabled = page != null) { page = null }

        val away = remember { Animatable(0f) } // 0 no lugar, 1 fora, à direita
        val leaving = helixEffectsSpec<Float>()
        val arriving = helixSpatialSpec<Float>()
        LaunchedEffect(page) {
            if (page !== shown) {
                away.animateTo(1f, leaving)
                shown = page
            }
            away.animateTo(0f, arriving)
        }
        val sizeSpec = helixSpatialSpec<IntSize>()
        Column(Modifier.fillMaxWidth().animateContentSize(sizeSpec)) {
            Column(
                Modifier.fillMaxWidth().graphicsLayer {
                    translationX = away.value * size.width
                    alpha = (1f - away.value).coerceIn(0f, 1f)
                },
            ) {
                val target = shown
                if (target != null) {
                    Column(Modifier.fillMaxWidth()) {
                        HelixFloatingCircle(Modifier.padding(start = 16.dp, bottom = 8.dp)) {
                            IconButton(onClick = { page = null }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.ds_back))
                            }
                        }
                        scope.target()
                    }
                } else {
                    scope.root()
                }
            }
        }
    }
}

/**
 * Página de confirmação dentro do [HelixSmartSheet] (no lugar de um painel por cima): título, explicação,
 * o botão que confirma (vermelho quando [destructive]) e "Cancelar", que fecha o painel.
 */
@Composable
fun HelixMenuPageScope.HelixConfirmPage(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    cancelLabel: String = stringResource(R.string.ds_cancel),
    destructive: Boolean = true,
) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
        Text(message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(
            onClick = {
                onConfirm()
                dismiss()
            },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            colors = if (destructive) {
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                )
            } else {
                ButtonDefaults.buttonColors()
            },
        ) { Text(confirmLabel) }
        TextButton(onClick = { dismiss() }, modifier = Modifier.fillMaxWidth()) { Text(cancelLabel) }
    }
}
