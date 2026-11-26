// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.features.settings

import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.helixgallery.core.designsystem.component.HelixAccent
import com.helixgallery.core.designsystem.component.HelixIcons
import com.helixgallery.core.designsystem.component.HelixSettingsItem
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.helixgallery.core.designsystem.component.HelixSettingsSection
import com.helixgallery.core.designsystem.component.HelixSubPage
import com.helixgallery.core.model.GRID_COLUMN_OPTIONS
import com.helixgallery.core.model.GridLayout

@Composable
fun GallerySettingsScreen(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: SettingsViewModel = viewModel()) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var canManage by remember { mutableStateOf(MediaStore.canManageMedia(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { canManage = MediaStore.canManageMedia(context) }
    HelixSubPage(title = stringResource(R.string.settings_gallery), onBack = onBack, modifier = modifier) {
        HelixSettingsSection(title = stringResource(R.string.settings_manage_media)) {
            HelixSettingsItem(
                icon = HelixIcons.Settings,
                title = stringResource(R.string.settings_manage_media_title),
                subtitle = stringResource(
                    if (canManage) R.string.settings_manage_media_on else R.string.settings_manage_media_off,
                ),
                accent = HelixAccent.Teal,
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_REQUEST_MANAGE_MEDIA, Uri.parse("package:${context.packageName}")),
                    )
                },
            )
        }
        HelixSettingsSection(title = stringResource(R.string.settings_layout)) {
            GridLayout.entries.forEach { layout ->
                RadioSettingsItem(
                    title = stringResource(layoutLabel(layout)),
                    selected = prefs.gridLayout == layout,
                    onClick = { viewModel.setGridLayout(layout) },
                )
            }
        }
        HelixSettingsSection(title = stringResource(R.string.settings_columns)) {
            GRID_COLUMN_OPTIONS.forEach { columns ->
                RadioSettingsItem(
                    title = pluralStringResource(R.plurals.settings_columns_value, columns, columns),
                    selected = prefs.columns == columns,
                    onClick = { viewModel.setColumns(columns) },
                )
            }
        }
    }
}

private fun layoutLabel(layout: GridLayout): Int = when (layout) {
    GridLayout.Regular -> R.string.layout_regular
    GridLayout.Day -> R.string.layout_day
    GridLayout.Month -> R.string.layout_month
}
