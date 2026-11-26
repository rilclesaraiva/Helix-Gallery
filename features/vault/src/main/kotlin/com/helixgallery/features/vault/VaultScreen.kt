// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.features.vault

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.CancellationSignal
import android.os.PersistableBundle
import android.content.ContextWrapper
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.DisposableEffect
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.helixgallery.core.designsystem.component.FLOATING_BUTTON_ALPHA
import com.helixgallery.core.designsystem.component.HelixFloatingActions
import com.helixgallery.core.designsystem.component.HelixFloatingBackButton
import com.helixgallery.core.designsystem.component.HelixFloatingCircle
import com.helixgallery.core.designsystem.component.HelixIcons
import com.helixgallery.core.designsystem.component.HelixImage
import com.helixgallery.core.designsystem.component.HelixLoading
import com.helixgallery.core.designsystem.component.HelixMenuAction
import com.helixgallery.core.designsystem.component.HelixMoreMenu
import com.helixgallery.core.designsystem.component.HelixModalSheet
import com.helixgallery.core.designsystem.component.HelixPageTitle
import com.helixgallery.core.designsystem.component.HelixStatusBarScrim
import com.helixgallery.core.designsystem.component.helixActionsInset
import com.helixgallery.core.model.MediaItem
import com.helixgallery.data.media.MediaData
import com.helixgallery.data.vault.VaultEntry
import com.helixgallery.data.vault.VaultSource
import kotlinx.coroutines.launch

/**
 * O cofre: fotos e vídeos num ZIP com senha (AES-256). [pending] são os itens que a pessoa mandou
 * guardar na galeria; entram assim que o cofre abre, e só saem do aparelho depois de gravados.
 */
@Composable
fun VaultScreen(
    onBack: () -> Unit,
    onOpenEntry: (uri: String, isVideo: Boolean) -> Unit,
    pending: List<MediaItem>,
    onPendingHandled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: VaultViewModel = viewModel()
    val context = LocalContext.current
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    val quickEnabled by viewModel.quickEnabled.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.messages.collect { Toast.makeText(context, messageText(context, it), Toast.LENGTH_LONG).show() }
    }
    // Saiu do app: tranca (a senha some da memória e os arquivos extraídos são apagados).
    // Atenção: o ciclo de vida da tela (NavBackStackEntry) também "para" ao abrir uma foto do cofre
    // dentro do app; só o da Activity diz que a pessoa saiu mesmo.
    val activityLifecycle = remember(context) { context.findComponentActivity()?.lifecycle }
    DisposableEffect(activityLifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && !viewModel.busy.value) viewModel.lock()
        }
        activityLifecycle?.addObserver(observer)
        onDispose { activityLifecycle?.removeObserver(observer) }
    }

    val deleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { }
    var pendingStarted by rememberSaveable { mutableStateOf(false) }
    val isOpen = ui is VaultUi.Open
    LaunchedEffect(isOpen, pending) {
        if (isOpen && pending.isNotEmpty() && !pendingStarted) {
            pendingStarted = true
            val sources = pending.map { VaultSource(it.uri, it.displayName, it.relativePath, it.takenMs) }
            onPendingHandled()
            viewModel.add(sources) { stored ->
                // Já estão gravados no cofre: agora a cópia solta na galeria pode sair (o Android pergunta).
                runCatching {
                    deleteLauncher.launch(IntentSenderRequest.Builder(MediaData.repository(context).deleteRequest(stored)).build())
                }
            }
        }
    }

    var confirmDelete by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        when (val state = ui) {
            VaultUi.Loading -> HelixLoading()
            VaultUi.NoVault -> CreatePage(busy, pending.size, onCreate = viewModel::create)
            VaultUi.Locked -> LockedPage(
                busy = busy,
                pending = pending.size,
                quickEnabled = quickEnabled,
                onUnlock = viewModel::unlock,
                onQuickUnlock = {
                    authenticate(context, context.getString(R.string.vault_auth_title)) { ok ->
                        if (ok) viewModel.unlockWithQuickUnlock()
                    }
                },
            )
            is VaultUi.SavePassword -> SavePasswordPage(state.password, onContinue = viewModel::passwordSaved)
            is VaultUi.Open -> OpenPage(
                entries = state.entries,
                selected = selected,
                busy = busy,
                fileUri = viewModel::fileUri,
                onOpen = { entry ->
                    scope.launch { runCatching { viewModel.fileUri(entry.name) }.onSuccess { onOpenEntry(it, entry.isVideo) } }
                },
                onToggle = viewModel::toggle,
            )
        }
        HelixStatusBarScrim(Modifier.align(Alignment.TopStart))

        val open = ui as? VaultUi.Open
        if (open != null && selected.isNotEmpty()) {
            SelectionBar(
                count = selected.size,
                onClose = viewModel::clearSelection,
                onRestore = viewModel::restoreSelected,
                onDelete = { confirmDelete = true },
                modifier = Modifier.align(Alignment.TopCenter),
            )
        } else {
            HelixFloatingBackButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart))
            if (open != null) {
                HelixFloatingActions(Modifier.align(Alignment.TopEnd)) {
                    HelixFloatingCircle {
                        IconButton(onClick = viewModel::lock) {
                            Icon(HelixIcons.Lock, contentDescription = stringResource(R.string.vault_lock))
                        }
                    }
                    if (viewModel.quickSupported) {
                        HelixMoreMenu(
                            actions = listOf(
                                HelixMenuAction(
                                    label = stringResource(if (quickEnabled) R.string.vault_quick_off else R.string.vault_quick_on),
                                    icon = HelixIcons.Lock,
                                    onClick = {
                                        if (quickEnabled) {
                                            viewModel.disableQuickUnlock()
                                        } else {
                                            authenticate(context, context.getString(R.string.vault_auth_title)) { ok ->
                                                if (ok) viewModel.enableQuickUnlock()
                                            }
                                        }
                                    },
                                ),
                            ),
                        )
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        HelixModalSheet(onDismiss = { confirmDelete = false }) {
            val sheet = this
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
                Text(stringResource(R.string.vault_delete_title), style = MaterialTheme.typography.titleLarge)
                Text(
                    stringResource(R.string.vault_delete_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp, bottom = 16.dp),
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { sheet.dismiss() }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.vault_cancel))
                    }
                    Button(
                        onClick = {
                            viewModel.deleteSelected()
                            sheet.dismiss()
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.vault_delete)) }
                }
            }
        }
    }
}

private fun messageText(context: Context, message: VaultMessage): String = when (message) {
    VaultMessage.WrongPassword -> context.getString(R.string.vault_wrong_password)
    VaultMessage.PasswordTooShort -> context.getString(R.string.vault_error_short, MIN_PASSWORD_LENGTH)
    VaultMessage.PasswordMismatch -> context.getString(R.string.vault_error_mismatch)
    VaultMessage.Failed -> context.getString(R.string.vault_failed)
    is VaultMessage.AddFailed -> context.resources.getQuantityString(R.plurals.vault_add_failed, message.count, message.count)
    is VaultMessage.Restored -> context.resources.getQuantityString(R.plurals.vault_restored, message.count, message.count)
}

/** Pede a biometria ou o PIN/padrão/senha do aparelho; [onResult] true se a pessoa se identificou. */
private fun authenticate(context: Context, title: String, onResult: (Boolean) -> Unit) {
    val prompt = BiometricPrompt.Builder(context)
        .setTitle(title)
        .setAllowedAuthenticators(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL,
        )
        .build()
    prompt.authenticate(
        CancellationSignal(),
        context.mainExecutor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) = onResult(true)
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) = onResult(false)
        },
    )
}

/** Moldura das páginas de formulário: título grande (com espaço para o voltar) e conteúdo rolável. */
@Composable
private fun FormPage(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .navigationBarsPadding(),
    ) {
        HelixPageTitle(title = stringResource(R.string.vault_title), hasBack = true, large = true)
        Column(Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            content()
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PendingNote(count: Int) {
    if (count > 0) {
        Text(
            pluralStringResource(R.plurals.vault_pending, count, count),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun PasswordField(value: String, onChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun CreatePage(busy: Boolean, pending: Int, onCreate: (String, String) -> Unit) {
    var password by rememberSaveable { mutableStateOf("") }
    var confirmation by rememberSaveable { mutableStateOf("") }
    FormPage {
        Text(
            stringResource(R.string.vault_create_intro),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        PendingNote(pending)
        PasswordField(password, { password = it }, stringResource(R.string.vault_password))
        PasswordField(confirmation, { confirmation = it }, stringResource(R.string.vault_confirm))
        Button(
            onClick = { onCreate(password, confirmation) },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.vault_create)) }
    }
}

@Composable
private fun LockedPage(
    busy: Boolean,
    pending: Int,
    quickEnabled: Boolean,
    onUnlock: (String) -> Unit,
    onQuickUnlock: () -> Unit,
) {
    var password by rememberSaveable { mutableStateOf("") }
    var autoTried by rememberSaveable { mutableStateOf(false) }
    // Com o desbloqueio rápido ligado, já pede a biometria/PIN ao abrir.
    LaunchedEffect(quickEnabled) {
        if (quickEnabled && !autoTried) {
            autoTried = true
            onQuickUnlock()
        }
    }
    FormPage {
        Text(
            stringResource(R.string.vault_locked_intro),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        PendingNote(pending)
        if (quickEnabled) {
            Button(onClick = onQuickUnlock, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Icon(HelixIcons.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.vault_quick_unlock))
            }
        }
        PasswordField(password, { password = it }, stringResource(R.string.vault_password))
        if (quickEnabled) {
            OutlinedButton(onClick = { onUnlock(password) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.vault_unlock))
            }
        } else {
            Button(onClick = { onUnlock(password) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.vault_unlock))
            }
        }
    }
}

@Composable
private fun SavePasswordPage(password: String, onContinue: () -> Unit) {
    val context = LocalContext.current
    FormPage {
        Text(stringResource(R.string.vault_save_title), style = MaterialTheme.typography.titleLarge)
        Text(
            stringResource(R.string.vault_save_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                password,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(16.dp),
            )
        }
        OutlinedButton(
            onClick = {
                val clip = ClipData.newPlainText("", password)
                // Marca como sensível: o Android esconde a prévia da senha copiada.
                clip.description.extras = PersistableBundle().apply { putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true) }
                context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(clip)
                Toast.makeText(context, R.string.vault_copied, Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.vault_copy)) }
        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.vault_saved_ack))
        }
    }
}

@Composable
private fun OpenPage(
    entries: List<VaultEntry>,
    selected: Set<String>,
    busy: Boolean,
    fileUri: suspend (String) -> String,
    onOpen: (VaultEntry) -> Unit,
    onToggle: (String) -> Unit,
) {
    val selecting = selected.isNotEmpty()
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            HelixPageTitle(
                title = stringResource(R.string.vault_title),
                hasBack = true,
                actionsInset = helixActionsInset(2),
            )
        }
        if (busy) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    stringResource(R.string.vault_working),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
        if (entries.isEmpty() && !busy) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    stringResource(R.string.vault_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(32.dp),
                )
            }
        }
        items(entries, key = { it.name }) { entry ->
            EntryCell(
                entry = entry,
                selected = entry.name in selected,
                fileUri = fileUri,
                onClick = { if (selecting) onToggle(entry.name) else onOpen(entry) },
                onLongClick = { onToggle(entry.name) },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EntryCell(
    entry: VaultEntry,
    selected: Boolean,
    fileUri: suspend (String) -> String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    // A miniatura vem do arquivo já descriptografado numa pasta temporária (apagada ao trancar).
    val uri by produceState<String?>(null, entry.name) { value = runCatching { fileUri(entry.name) }.getOrNull() }
    Box(
        Modifier
            .aspectRatio(1f)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        uri?.let { HelixImage(uri = it, modifier = Modifier.fillMaxSize()) }
        if (entry.isVideo) {
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .background(Color.Black.copy(alpha = 0.55f), MaterialTheme.shapes.small)
                    .size(18.dp),
            )
        }
        if (selected) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)))
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small)
                    .size(20.dp),
            )
        }
    }
}

@Composable
private fun SelectionBar(
    count: Int,
    onClose: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = FLOATING_BUTTON_ALPHA),
        shadowElevation = 3.dp,
        modifier = modifier.statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.vault_clear_selection))
            }
            Text(
                pluralStringResource(R.plurals.vault_selected, count, count),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onRestore) {
                Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.vault_restore))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.vault_delete))
            }
        }
    }
}

private tailrec fun Context.findComponentActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findComponentActivity()
    else -> null
}
