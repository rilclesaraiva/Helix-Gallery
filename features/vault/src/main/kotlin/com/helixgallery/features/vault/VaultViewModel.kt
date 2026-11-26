// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.features.vault

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.helixgallery.data.vault.VaultAddResult
import com.helixgallery.data.vault.VaultData
import com.helixgallery.data.vault.VaultEntry
import com.helixgallery.data.vault.VaultSource
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal sealed interface VaultUi {
    data object Loading : VaultUi
    data object NoVault : VaultUi
    data object Locked : VaultUi
    /** A senha acabou de ser criada: mostrada uma vez. */
    data class SavePassword(val password: String) : VaultUi
    data class Open(val entries: List<VaultEntry>) : VaultUi
}

/** Avisos curtos para a pessoa (viram um Toast com texto traduzido na tela). */
internal sealed interface VaultMessage {
    data object WrongPassword : VaultMessage
    data object PasswordTooShort : VaultMessage
    data object PasswordMismatch : VaultMessage
    data object Failed : VaultMessage
    data class AddFailed(val count: Int) : VaultMessage
    data class Restored(val count: Int) : VaultMessage
}

internal class VaultViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = VaultData.repository(application)

    private val _ui = MutableStateFlow<VaultUi>(VaultUi.Loading)
    val ui: StateFlow<VaultUi> = _ui.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _selected = MutableStateFlow<Set<String>>(emptySet())
    val selected: StateFlow<Set<String>> = _selected.asStateFlow()

    private val _messages = MutableSharedFlow<VaultMessage>(extraBufferCapacity = 4)
    val messages: SharedFlow<VaultMessage> = _messages.asSharedFlow()

    private val _quickEnabled = MutableStateFlow(repository.quickUnlockEnabled())
    val quickEnabled: StateFlow<Boolean> = _quickEnabled.asStateFlow()

    val quickSupported: Boolean get() = repository.quickUnlockSupported()

    init {
        viewModelScope.launch { _ui.value = currentState() }
    }

    private suspend fun currentState(): VaultUi = when {
        repository.isUnlocked() -> VaultUi.Open(repository.entries())
        repository.exists() -> VaultUi.Locked
        else -> VaultUi.NoVault
    }

    private suspend fun refresh() {
        _ui.value = VaultUi.Open(repository.entries())
    }

    fun create(password: String, confirmation: String) {
        when (checkNewPassword(password, confirmation)) {
            PasswordCheck.TooShort -> _messages.tryEmit(VaultMessage.PasswordTooShort)
            PasswordCheck.Mismatch -> _messages.tryEmit(VaultMessage.PasswordMismatch)
            PasswordCheck.Ok -> viewModelScope.launch {
                _busy.value = true
                try {
                    repository.create(password.toCharArray())
                    _ui.value = VaultUi.SavePassword(password)
                } catch (_: Exception) {
                    _messages.tryEmit(VaultMessage.Failed)
                }
                _busy.value = false
            }
        }
    }

    /** A pessoa confirmou que guardou a senha: abre o cofre vazio. */
    fun passwordSaved() {
        viewModelScope.launch { refresh() }
    }

    fun unlock(password: String) {
        viewModelScope.launch {
            _busy.value = true
            val ok = try {
                repository.unlock(password.toCharArray())
            } catch (_: Exception) {
                _messages.tryEmit(VaultMessage.Failed)
                false
            }
            if (ok) refresh() else _messages.tryEmit(VaultMessage.WrongPassword)
            _busy.value = false
        }
    }

    /** Depois de a pessoa se identificar (biometria/PIN): abre com a senha guardada. */
    fun unlockWithQuickUnlock() {
        val saved = repository.quickUnlockPassword()
        if (saved == null) {
            _quickEnabled.value = repository.quickUnlockEnabled()
            return
        }
        viewModelScope.launch {
            _busy.value = true
            val ok = try {
                repository.unlock(saved)
            } catch (_: Exception) {
                false
            }
            saved.fill('\u0000')
            if (ok) refresh() else _messages.tryEmit(VaultMessage.WrongPassword)
            _busy.value = false
        }
    }

    /** Depois de a pessoa se identificar: liga o desbloqueio rápido. */
    fun enableQuickUnlock() {
        if (!repository.enableQuickUnlock()) _messages.tryEmit(VaultMessage.Failed)
        _quickEnabled.value = repository.quickUnlockEnabled()
    }

    fun disableQuickUnlock() {
        repository.disableQuickUnlock()
        _quickEnabled.value = false
    }

    fun lock() {
        repository.lock()
        _selected.value = emptySet()
        viewModelScope.launch { _ui.value = currentState() }
    }

    /** Guarda [sources]; [onStored] recebe os endereços que entraram de fato (e só esses podem sair do aparelho). */
    fun add(sources: List<VaultSource>, onStored: (List<String>) -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            val result: VaultAddResult? = try {
                repository.add(sources)
            } catch (_: Exception) {
                _messages.tryEmit(VaultMessage.Failed)
                null
            }
            if (result != null) {
                if (result.failed > 0) _messages.tryEmit(VaultMessage.AddFailed(result.failed))
                refresh()
                if (result.stored.isNotEmpty()) onStored(result.stored)
            }
            _busy.value = false
        }
    }

    fun selectAll(names: Collection<String>) {
        _selected.value = names.toSet()
    }

    fun toggle(name: String) {
        _selected.value = _selected.value.let { if (name in it) it - name else it + name }
    }

    fun clearSelection() {
        _selected.value = emptySet()
    }

    fun restoreSelected() = restore(_selected.value.toList())

    fun restore(names: List<String>) {
        viewModelScope.launch {
            _busy.value = true
            try {
                val count = repository.restore(names)
                _messages.tryEmit(VaultMessage.Restored(count))
                if (count < names.size) _messages.tryEmit(VaultMessage.Failed)
            } catch (_: Exception) {
                _messages.tryEmit(VaultMessage.Failed)
            }
            _selected.value = emptySet()
            refresh()
            _busy.value = false
        }
    }

    fun deleteSelected() = delete(_selected.value.toList())

    fun delete(names: List<String>) {
        viewModelScope.launch {
            _busy.value = true
            try {
                repository.delete(names)
            } catch (_: Exception) {
                _messages.tryEmit(VaultMessage.Failed)
            }
            _selected.value = emptySet()
            refresh()
            _busy.value = false
        }
    }

    /** O endereço (file://) do arquivo já descriptografado, para mostrar. */
    suspend fun fileUri(name: String): String = Uri.fromFile(repository.extractForViewing(name)).toString()

    override fun onCleared() {
        // Saiu da tela do cofre: tranca (apaga a senha da memória e o que foi extraído).
        repository.lock()
    }
}
