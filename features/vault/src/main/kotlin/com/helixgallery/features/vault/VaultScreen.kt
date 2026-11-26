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
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.helixgallery.core.designsystem.component.dragSelect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import com.helixgallery.core.designsystem.theme.helixFastSpatialSpec
import com.helixgallery.core.designsystem.theme.helixSpatialSpec
import com.helixgallery.core.designsystem.theme.helixEffectsSpec
import com.helixgallery.core.designsystem.component.recedeWhileSelecting
import com.helixgallery.core.designsystem.component.HelixSelectionBar
import com.helixgallery.core.designsystem.component.HelixConfirmSheet
import com.helixgallery.core.designsystem.component.HelixBottomActionBar
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
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
/** O que o visualizador (do app) precisa para passar pelos arquivos do cofre com o gesto. */
class VaultViewerArgs(
    val count: Int,
    val startIndex: Int,
    val isVideo: (Int) -> Boolean,
    val resolve: suspend (Int) -> String?,
    val onClose: () -> Unit,
    val restoreLabel: String,
    val deleteLabel: String,
    val onRestore: (Int) -> Unit,
    val onDelete: (Int) -> Unit,
)

@Composable
fun VaultScreen(
    onBack: () -> Unit,
    viewer: @Composable (VaultViewerArgs) -> Unit,
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

    // Nomes a apagar do cofre (da seleção ou da foto aberta); não nulo = painel de confirmação aberto.
    var confirmDelete by remember { mutableStateOf<List<String>?>(null) }
    var viewing by rememberSaveable { mutableStateOf<Int?>(null) }

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
                onOpen = { entry -> viewing = state.entries.indexOf(entry).takeIf { it >= 0 } },
                onToggle = viewModel::toggle,
            )
        }
        val viewIndex = viewing
        val openState = ui as? VaultUi.Open
        if (viewIndex != null && openState != null && openState.entries.isNotEmpty()) {
            BackHandler { viewing = null }
            viewer(
                VaultViewerArgs(
                    count = openState.entries.size,
                    startIndex = viewIndex,
                    isVideo = { openState.entries.getOrNull(it)?.isVideo == true },
                    resolve = { index ->
                        openState.entries.getOrNull(index)?.let { runCatching { viewModel.fileUri(it.name) }.getOrNull() }
                    },
                    onClose = { viewing = null },
                    restoreLabel = stringResource(R.string.vault_restore_short),
                    deleteLabel = stringResource(R.string.vault_delete_short),
                    onRestore = { index -> openState.entries.getOrNull(index)?.let { viewModel.restore(listOf(it.name)) } },
                    onDelete = { index -> openState.entries.getOrNull(index)?.let { confirmDelete = listOf(it.name) } },
                ),
            )
        } else {
            HelixStatusBarScrim(Modifier.align(Alignment.TopStart))
        }

        val open = ui as? VaultUi.Open
        val inList = open != null && viewing == null
        val selecting = inList && selected.isNotEmpty()
        BackHandler(enabled = selecting) { viewModel.clearSelection() }
        // Voltar, trancar e ⋮ flutuam na lista; com itens marcados a barra de seleção toma o lugar deles.
        AnimatedVisibility(
            visible = !selecting && viewing == null,
            modifier = Modifier.align(Alignment.TopStart),
            enter = fadeIn(helixEffectsSpec()),
            exit = fadeOut(helixEffectsSpec()),
        ) { HelixFloatingBackButton(onClick = onBack) }
        if (open != null) {
            AnimatedVisibility(
                visible = !selecting && viewing == null,
                modifier = Modifier.align(Alignment.TopEnd),
                enter = fadeIn(helixEffectsSpec()),
                exit = fadeOut(helixEffectsSpec()),
            ) {
                HelixFloatingActions {
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
        HelixSelectionBar(
            visible = selecting,
            count = selected.size,
            allSelected = open != null && open.entries.isNotEmpty() && selected.size == open.entries.size,
            onClose = viewModel::clearSelection,
            onToggleAll = { all -> if (all) viewModel.selectAll(open?.entries?.map { it.name }.orEmpty()) else viewModel.clearSelection() },
            actions = emptyList(),
            modifier = Modifier.align(Alignment.TopCenter),
        )
        AnimatedVisibility(
            visible = selecting,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(helixSpatialSpec()) { it } + fadeIn(helixEffectsSpec()),
            exit = slideOutVertically(helixSpatialSpec()) { it } + fadeOut(helixEffectsSpec()),
        ) {
            HelixBottomActionBar(
                primaryLabel = stringResource(R.string.vault_restore_short),
                onPrimary = viewModel::restoreSelected,
                destructiveLabel = stringResource(R.string.vault_delete_short),
                onDestructive = { confirmDelete = selected.toList() },
            )
        }
    }

    confirmDelete?.let { names ->
        HelixConfirmSheet(
            title = stringResource(R.string.vault_delete_title),
            message = stringResource(R.string.vault_delete_body),
            confirmLabel = stringResource(R.string.vault_delete),
            onConfirm = { viewModel.delete(names) },
            onDismiss = { confirmDelete = null },
        )
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

/** Campo de senha de bordas bem redondas, com o olho para mostrar/esconder (como no Babel-Manager). */
@Composable
private fun PasswordField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    imeAction: ImeAction = ImeAction.Next,
    onDone: (() -> Unit)? = null,
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        shape = CircleShape,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction, autoCorrectEnabled = false),
        keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    if (visible) HelixIcons.VisibilityOff else HelixIcons.Visibility,
                    contentDescription = stringResource(if (visible) R.string.vault_hide_password else R.string.vault_show_password),
                )
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Tela de senha no estilo do Babel-Manager: círculo com cadeado, título, dica e os campos, no centro. */
@Composable
private fun LockLayout(title: String, hint: String, content: @Composable () -> Unit) {
    Box(
        Modifier.fillMaxSize().imePadding().navigationBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.widthIn(max = 480.dp).verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(
                modifier = Modifier.size(88.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(HelixIcons.Lock, contentDescription = null, modifier = Modifier.size(40.dp))
                }
            }
            Text(title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Text(
                hint,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            content()
        }
    }
}

@Composable
private fun CreatePage(busy: Boolean, pending: Int, onCreate: (String, String) -> Unit) {
    var password by rememberSaveable { mutableStateOf("") }
    var confirmation by rememberSaveable { mutableStateOf("") }
    LockLayout(stringResource(R.string.vault_create_title), stringResource(R.string.vault_create_intro)) {
        PendingNote(pending)
        PasswordField(password, { password = it }, stringResource(R.string.vault_password))
        PasswordField(
            confirmation,
            { confirmation = it },
            stringResource(R.string.vault_confirm),
            imeAction = ImeAction.Done,
            onDone = { onCreate(password, confirmation) },
        )
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
    LockLayout(stringResource(R.string.vault_locked_title), stringResource(R.string.vault_locked_intro)) {
        PendingNote(pending)
        PasswordField(
            password,
            { password = it },
            stringResource(R.string.vault_password),
            imeAction = ImeAction.Done,
            onDone = { onUnlock(password) },
        )
        Button(
            onClick = { onUnlock(password) },
            enabled = !busy && password.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.vault_unlock)) }
        if (quickEnabled) {
            FilledTonalButton(onClick = onQuickUnlock, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Icon(HelixIcons.Lock, contentDescription = null, modifier = Modifier.size(20.dp))
                Text(stringResource(R.string.vault_quick_unlock), modifier = Modifier.padding(start = 8.dp))
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
    val gridState = rememberLazyGridState()
    val suppressClick = remember { mutableStateOf(false) }
    val currentEntries by rememberUpdatedState(entries)
    val currentSelected by rememberUpdatedState(selected)
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        state = gridState,
        modifier = Modifier.fillMaxSize().dragSelect(
            state = gridState,
            order = { currentEntries.map { it.name } },
            selected = { currentSelected },
            onToggle = onToggle,
            suppressClick = suppressClick,
        ),
        contentPadding = PaddingValues(bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Box(Modifier.recedeWhileSelecting(selecting)) {
                HelixPageTitle(
                    title = stringResource(R.string.vault_title),
                    hasBack = true,
                    actionsInset = helixActionsInset(2),
                )
            }
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
                onClick = {
                    // Soltar o dedo depois de segurar (sem arrastar) não é um toque.
                    if (suppressClick.value) suppressClick.value = false
                    else if (selecting) onToggle(entry.name) else onOpen(entry)
                },
                selecting = selecting,
            )
        }
    }
}

@Composable
private fun EntryCell(
    entry: VaultEntry,
    selected: Boolean,
    selecting: Boolean,
    fileUri: suspend (String) -> String,
    onClick: () -> Unit,
) {
    // A miniatura vem do arquivo já descriptografado numa pasta temporária (apagada ao trancar).
    val uri by produceState<String?>(null, entry.name) { value = runCatching { fileUri(entry.name) }.getOrNull() }
    // Mesmo visual de marcação da galeria: a miniatura encolhe e arredonda, e a bolinha de seleção aparece.
    val scale by animateFloatAsState(
        targetValue = if (selected) 0.84f else 1f,
        animationSpec = helixFastSpatialSpec(),
        label = "entryScale",
    )
    Box(
        Modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(if (selected) RoundedCornerShape(16.dp) else RoundedCornerShape(0.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            uri?.let { HelixImage(uri = it, modifier = Modifier.fillMaxSize()) }
        }
        if (entry.isVideo) {
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                    .size(18.dp),
            )
        }
        if (selecting) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (selected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.35f)),
            ) {
                if (selected) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

private tailrec fun Context.findComponentActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findComponentActivity()
    else -> null
}
