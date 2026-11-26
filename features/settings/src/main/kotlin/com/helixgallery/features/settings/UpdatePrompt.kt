// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.features.settings

import android.app.Application
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.helixgallery.core.common.openUrl
import com.helixgallery.data.updates.UpdateCheckResult
import com.helixgallery.data.updates.UpdatesData
import com.helixgallery.data.updates.shouldAutoCheck
import com.helixgallery.data.updates.shouldPrompt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * O aviso de versão nova que aparece sozinho ao abrir o app. Abrindo do zero sempre consulta
 * o GitHub; voltando ao app, no máximo a cada 10 min. "Agora não" vale até abrir o app de novo.
 */
class UpdatePromptViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = UpdatesData.repository(application)

    private val _available = MutableStateFlow<UpdateCheckResult.Available?>(null)
    val available: StateFlow<UpdateCheckResult.Available?> = _available.asStateFlow()

    private var handled = false
    private var checking = false
    private var first = true

    fun checkOnOpen() {
        if (handled) return
        val coldStart = first
        first = false
        val now = System.currentTimeMillis()
        repository.cachedUpdate()?.let { cached ->
            if (shouldPrompt(cached.latestVersion, now, repository.promptState())) _available.value = cached
        }
        if (checking || !shouldAutoCheck(now, repository.promptState(), coldStart)) return
        checking = true
        viewModelScope.launch {
            val result = repository.checkForUpdates()
            if (result !is UpdateCheckResult.Failed) repository.markAutoCheck(System.currentTimeMillis())
            when {
                handled -> Unit
                result is UpdateCheckResult.Available &&
                    shouldPrompt(result.latestVersion, System.currentTimeMillis(), repository.promptState()) ->
                    _available.value = result
                result is UpdateCheckResult.UpToDate || result is UpdateCheckResult.NoReleases -> _available.value = null
                else -> Unit
            }
            checking = false
        }
    }

    fun dismiss() {
        handled = true
        _available.value = null
    }

    fun skip() {
        _available.value?.let { repository.skipVersion(it.latestVersion) }
        dismiss()
    }
}

/** Coloque uma vez na raiz do app: mostra a janela de "Nova versão" quando houver. */
@Composable
fun UpdatePromptHost(viewModel: UpdatePromptViewModel = viewModel()) {
    val context = LocalContext.current
    val available by viewModel.available.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.checkOnOpen() }

    available?.let { update ->
        AlertDialog(
            onDismissRequest = viewModel::dismiss,
            title = { Text(stringResource(R.string.update_available_title, update.latestVersion)) },
            text = { Text(releaseNotesPreview(update.notes).ifBlank { stringResource(R.string.update_available_body) }) },
            confirmButton = {
                TextButton(onClick = {
                    context.openUrl(update.downloadUrl ?: update.pageUrl)
                    viewModel.dismiss()
                }) { Text(stringResource(R.string.update_download)) }
            },
            dismissButton = {
                Row2(
                    onLater = viewModel::dismiss,
                    onSkip = viewModel::skip,
                )
            },
        )
    }
}

@Composable
private fun Row2(onLater: () -> Unit, onSkip: () -> Unit) {
    androidx.compose.foundation.layout.Row {
        TextButton(onClick = onSkip) { Text(stringResource(R.string.update_skip)) }
        TextButton(onClick = onLater) { Text(stringResource(R.string.update_later)) }
    }
}

/** Notas da Release em Markdown → texto simples e curto para a janela. */
internal fun releaseNotesPreview(markdown: String, maxChars: Int = 600): String {
    val text = markdown.lines()
        .joinToString("\n") { it.trim().trimStart('#', '*', '-', ' ').trim() }
        .replace(Regex("\n{3,}"), "\n\n")
        .trim()
    return if (text.length <= maxChars) text else text.take(maxChars).trimEnd() + "…"
}
