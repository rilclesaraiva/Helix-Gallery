// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.features.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.helixgallery.core.common.openUrl
import com.helixgallery.core.designsystem.component.HelixAccent
import com.helixgallery.core.designsystem.component.HelixIcons
import com.helixgallery.core.designsystem.component.HelixSettingsItem
import com.helixgallery.core.designsystem.component.HelixSettingsSection
import com.helixgallery.core.designsystem.component.HelixSubPage
import com.helixgallery.core.model.GRID_COLUMN_OPTIONS
import com.helixgallery.core.model.GridLayout
import com.helixgallery.core.model.ThemeMode
import com.helixgallery.data.updates.UpdateCheckResult

internal fun appVersionName(context: android.content.Context): String =
    try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
    } catch (_: Exception) {
        "?"
    }

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(),
) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var updatesOpen by remember { mutableStateOf(false) }

    HelixSubPage(title = stringResource(R.string.settings_title), onBack = onBack, modifier = modifier) {
        HelixSettingsSection(title = stringResource(R.string.settings_appearance)) {
            ChoiceItem(
                title = stringResource(R.string.settings_theme),
                current = stringResource(themeLabel(prefs.themeMode)),
                options = ThemeMode.entries.map { it to stringResource(themeLabel(it)) },
                selected = prefs.themeMode,
                onSelect = viewModel::setThemeMode,
            )
            HelixSettingsItem(
                icon = Icons.Filled.Star,
                title = stringResource(R.string.settings_dynamic_color),
                subtitle = stringResource(R.string.settings_dynamic_color_desc),
                accent = HelixAccent.Pink,
                trailing = {
                    Switch(checked = prefs.dynamicColor, onCheckedChange = viewModel::setDynamicColor)
                },
                onClick = { viewModel.setDynamicColor(!prefs.dynamicColor) },
            )
        }
        HelixSettingsSection(title = stringResource(R.string.settings_gallery)) {
            ChoiceItem(
                title = stringResource(R.string.settings_layout),
                current = stringResource(layoutLabel(prefs.gridLayout)),
                options = GridLayout.entries.map { it to stringResource(layoutLabel(it)) },
                selected = prefs.gridLayout,
                onSelect = viewModel::setGridLayout,
                icon = HelixIcons.Photo,
                accent = HelixAccent.Teal,
            )
            ChoiceItem(
                title = stringResource(R.string.settings_columns),
                current = pluralStringResource(R.plurals.settings_columns_value, prefs.columns, prefs.columns),
                options = GRID_COLUMN_OPTIONS.map { it to pluralStringResource(R.plurals.settings_columns_value, it, it) },
                selected = prefs.columns,
                onSelect = viewModel::setColumns,
                icon = HelixIcons.Collections,
                accent = HelixAccent.Teal,
            )
        }
        HelixSettingsSection(title = stringResource(R.string.settings_system)) {
            HelixSettingsItem(
                icon = Icons.Filled.Refresh,
                title = stringResource(R.string.settings_updates),
                subtitle = stringResource(R.string.settings_updates_version, appVersionName(context)),
                accent = HelixAccent.Green,
                onClick = { updatesOpen = !updatesOpen },
                trailing = {
                    Icon(
                        if (updatesOpen) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                    )
                },
            )
            AnimatedVisibility(
                visible = updatesOpen,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Button(
                            onClick = viewModel::checkForUpdates,
                            enabled = updateState != UpdatesUiState.Checking,
                        ) {
                            Text(stringResource(R.string.settings_updates_check))
                        }
                        val message = when (val s = updateState) {
                            UpdatesUiState.Idle -> null
                            UpdatesUiState.Checking -> stringResource(R.string.settings_updates_checking)
                            is UpdatesUiState.Done -> when (val r = s.result) {
                                is UpdateCheckResult.UpToDate -> stringResource(R.string.settings_updates_up_to_date)
                                is UpdateCheckResult.Available ->
                                    stringResource(R.string.settings_updates_available, r.latestVersion)
                                UpdateCheckResult.NoReleases -> stringResource(R.string.settings_updates_none)
                                UpdateCheckResult.Failed -> stringResource(R.string.settings_updates_failed)
                            }
                        }
                        if (message != null) {
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 12.dp),
                            )
                        }
                        val done = (updateState as? UpdatesUiState.Done)?.result as? UpdateCheckResult.Available
                        if (done != null) {
                            Button(
                                onClick = { context.openUrl(done.downloadUrl ?: done.pageUrl) },
                                modifier = Modifier.padding(top = 8.dp),
                            ) {
                                Text(stringResource(R.string.update_download))
                            }
                        }
                    }
                }
            }
            HelixSettingsItem(
                icon = Icons.Filled.Info,
                title = stringResource(R.string.settings_about),
                subtitle = stringResource(R.string.settings_about_desc),
                accent = HelixAccent.Blue,
                onClick = onOpenAbout,
            )
        }
    }
}

private fun themeLabel(mode: ThemeMode): Int = when (mode) {
    ThemeMode.System -> R.string.theme_system
    ThemeMode.Light -> R.string.theme_light
    ThemeMode.Dark -> R.string.theme_dark
    ThemeMode.Amoled -> R.string.theme_amoled
}

private fun layoutLabel(layout: GridLayout): Int = when (layout) {
    GridLayout.Regular -> R.string.layout_regular
    GridLayout.Day -> R.string.layout_day
    GridLayout.Month -> R.string.layout_month
}

/** Item que abre um menu com as opções; o valor atual aparece à direita. */
@Composable
private fun <T> ChoiceItem(
    title: String,
    current: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Filled.Settings,
    accent: HelixAccent = HelixAccent.Purple,
) {
    var open by remember { mutableStateOf(false) }
    HelixSettingsItem(
        icon = icon,
        title = title,
        accent = accent,
        onClick = { open = true },
        trailing = {
            Row {
                Text(
                    text = current,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                    options.forEach { (value, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            leadingIcon = {
                                if (value == selected) Icon(Icons.Filled.Check, contentDescription = null)
                            },
                            onClick = {
                                onSelect(value)
                                open = false
                            },
                        )
                    }
                }
            }
        },
    )
}
