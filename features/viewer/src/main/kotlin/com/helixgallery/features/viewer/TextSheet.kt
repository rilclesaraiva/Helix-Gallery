// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.features.viewer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.helixgallery.core.designsystem.component.HelixModalSheet

/**
 * Painel do "Copiar texto": mostra o texto reconhecido na imagem num campo editável (para corrigir erros) e um
 * botão para copiar tudo. A leitura é feita no aparelho.
 */
@Composable
internal fun TextSheet(state: TextState, onDismiss: () -> Unit) {
    val context = LocalContext.current
    HelixModalSheet(onDismiss = onDismiss) {
        val sheet = this
        Column(Modifier.fillMaxWidth().imePadding().padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
            Text(
                stringResource(R.string.viewer_text_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            when (state) {
                TextState.Reading -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 3.dp)
                    Text(stringResource(R.string.viewer_text_reading), style = MaterialTheme.typography.bodyLarge)
                }
                is TextState.Failed -> {
                    Text(stringResource(R.string.viewer_text_failed), style = MaterialTheme.typography.bodyLarge)
                    // Detalhe técnico do erro (tipo e mensagem), para quem for relatar o problema.
                    if (state.detail.isNotBlank()) {
                        SelectionContainer {
                            Text(
                                state.detail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                }
                is TextState.Done -> if (state.flowed.isBlank()) {
                    Text(stringResource(R.string.viewer_text_none), style = MaterialTheme.typography.bodyLarge)
                } else {
                    // O texto lido pode ter erros: dá para corrigir aqui antes de copiar.
                    var edited by rememberSaveable(state.flowed) { mutableStateOf(state.flowed) }
                    OutlinedTextField(
                        value = edited,
                        onValueChange = { edited = it },
                        label = { Text(stringResource(R.string.viewer_text_edit_label)) },
                        shape = MaterialTheme.shapes.large,
                        textStyle = MaterialTheme.typography.bodyLarge,
                        minLines = 3,
                        maxLines = 12,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = {
                            copyToClipboard(context, edited)
                            sheet.dismiss()
                        },
                        enabled = edited.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    ) { Text(stringResource(R.string.viewer_text_copy_all)) }
                    // Com as quebras de linha como estão na imagem (útil para listas e tabelas); some depois de editar,
                    // porque aí o texto é um só.
                    if (state.original != state.flowed && edited == state.flowed) {
                        TextButton(
                            onClick = {
                                copyToClipboard(context, state.original)
                                sheet.dismiss()
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(stringResource(R.string.viewer_text_copy_original)) }
                    }
                }
            }
        }
    }
}

internal fun copyToClipboard(context: Context, text: String, @StringRes toast: Int = R.string.viewer_text_copied) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(null, text))
    // A partir do Android 13 o próprio sistema avisa que copiou.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        Toast.makeText(context, toast, Toast.LENGTH_SHORT).show()
    }
}
