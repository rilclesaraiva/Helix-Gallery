// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.features.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.helixgallery.core.common.openUrl
import com.helixgallery.core.designsystem.component.HelixAccent
import com.helixgallery.core.designsystem.component.HelixIcons
import com.helixgallery.core.designsystem.component.HelixSettingsItem
import com.helixgallery.core.designsystem.component.HelixSettingsSection
import com.helixgallery.core.designsystem.component.HelixSubPage
import com.helixgallery.core.designsystem.theme.helixEffectsSpec
import com.helixgallery.core.designsystem.theme.helixSpatialSpec
import com.helixgallery.data.updates.UpdateCheckResult
import com.helixgallery.data.updates.UpdatePromptState
import java.text.DateFormat
import java.util.Date

/** Ajustes → Sistema: atualizações (verificar e escolher como ficam os avisos). */
@Composable
fun SystemSettingsScreen(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: SettingsViewModel = viewModel()) {
    val context = LocalContext.current
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val prompts by viewModel.prompts.collectAsStateWithLifecycle()
    var choosingPrompts by remember { mutableStateOf(false) }
    val available = (updateState as? UpdatesUiState.Done)?.result as? UpdateCheckResult.Available

    HelixSubPage(title = stringResource(R.string.settings_system), onBack = onBack, modifier = modifier) {
        BackupSection()
        HelixSettingsSection(title = stringResource(R.string.settings_updates_section)) {
            HelixSettingsItem(
                icon = HelixIcons.Update,
                accent = HelixAccent.Green,
                title = stringResource(R.string.settings_updates),
                subtitle = updatesSubtitle(updateState, appVersionName(context)),
                onClick = {
                    if (available != null) context.openUrl(available.downloadUrl ?: available.pageUrl)
                    else viewModel.checkForUpdates()
                },
            )
            // Avisos automáticos: ligados, adiados ou pulando uma versão; dá para desfazer aqui.
            val mode = promptMode(prompts, System.currentTimeMillis())
            HelixSettingsItem(
                icon = if (mode == PromptMode.Always) HelixIcons.NotificationsActive else HelixIcons.NotificationsOff,
                accent = HelixAccent.Green,
                title = stringResource(R.string.settings_update_prompts),
                subtitle = promptsSubtitle(prompts),
                onClick = { choosingPrompts = !choosingPrompts },
                trailing = {
                    val turn by animateFloatAsState(if (choosingPrompts) 180f else 0f, helixSpatialSpec(), label = "promptsArrow")
                    Icon(HelixIcons.ExpandMore, contentDescription = null, modifier = Modifier.rotate(turn))
                },
            )
            // Recolhível: as escolhas abrem aqui mesmo, sem painel.
            AnimatedVisibility(
                visible = choosingPrompts,
                enter = expandVertically(helixSpatialSpec()) + fadeIn(helixEffectsSpec()),
                exit = shrinkVertically(helixSpatialSpec()) + fadeOut(helixEffectsSpec()),
            ) {
                Column {
                    RadioSettingsItem(
                        title = stringResource(R.string.settings_update_prompts_always),
                        selected = mode == PromptMode.Always || mode == PromptMode.Skipped,
                        onClick = viewModel::resumePrompts,
                    )
                    RadioSettingsItem(
                        title = stringResource(R.string.settings_update_prompt_week),
                        selected = mode == PromptMode.Week,
                        onClick = { viewModel.snoozePrompts(WEEK_MS) },
                    )
                    RadioSettingsItem(
                        title = stringResource(R.string.settings_update_prompt_month),
                        selected = mode == PromptMode.Month,
                        onClick = { viewModel.snoozePrompts(MONTH_MS) },
                    )
                }
            }
        }
    }
}

@Composable
private fun updatesSubtitle(state: UpdatesUiState, version: String): String = when (state) {
    UpdatesUiState.Idle -> stringResource(R.string.settings_updates_version, version)
    UpdatesUiState.Checking -> stringResource(R.string.settings_updates_checking)
    is UpdatesUiState.Done -> when (val r = state.result) {
        is UpdateCheckResult.UpToDate -> stringResource(R.string.settings_updates_up_to_date)
        is UpdateCheckResult.Available -> stringResource(R.string.settings_updates_available, r.latestVersion)
        UpdateCheckResult.NoReleases -> stringResource(R.string.settings_updates_none)
        UpdateCheckResult.Failed -> stringResource(R.string.settings_updates_failed)
    }
}

@Composable
private fun promptsSubtitle(state: UpdatePromptState): String {
    val now = System.currentTimeMillis()
    val skipped = state.skippedVersion
    return when {
        state.snoozedUntilMs > now -> stringResource(
            R.string.settings_update_prompts_snoozed,
            DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(state.snoozedUntilMs)),
        )
        skipped != null -> stringResource(R.string.settings_update_prompts_skipped, skipped.removePrefix("v"))
        else -> stringResource(R.string.settings_update_prompts_on)
    }
}

/** Backup em ZIP: gravar num lugar à escolha da pessoa e devolver à galeria. */
@Composable
private fun BackupSection(viewModel: BackupViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) viewModel.export(uri)
    }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.import(uri)
    }
    val busy = state.busy
    val status: String? = when (val s = state) {
        BackupUi.Idle -> null
        is BackupUi.Exporting -> stringResource(R.string.settings_backup_working, s.done, s.total)
        BackupUi.Importing -> stringResource(R.string.settings_backup_importing)
        is BackupUi.Exported -> pluralStringResource(R.plurals.settings_backup_exported, s.count, s.count)
        is BackupUi.Imported -> stringResource(R.string.settings_backup_imported, s.imported, s.skipped)
        BackupUi.Failed -> stringResource(R.string.settings_backup_failed)
    }
    HelixSettingsSection(title = stringResource(R.string.settings_backup_section)) {
        HelixSettingsItem(
            icon = HelixIcons.Send,
            accent = HelixAccent.Green,
            title = stringResource(R.string.settings_backup_export),
            subtitle = status ?: stringResource(R.string.settings_backup_export_desc),
            enabled = !busy,
            onClick = { export.launch("helixgallery_backup_" + java.time.LocalDate.now() + ".zip") },
        )
        HelixSettingsItem(
            icon = HelixIcons.History,
            accent = HelixAccent.Green,
            title = stringResource(R.string.settings_backup_import),
            subtitle = stringResource(R.string.settings_backup_import_desc),
            enabled = !busy,
            onClick = { import.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream")) },
        )
    }
}
