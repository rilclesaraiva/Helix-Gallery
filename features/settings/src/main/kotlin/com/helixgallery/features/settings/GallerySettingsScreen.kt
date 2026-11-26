// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.features.settings

import androidx.compose.runtime.Composable
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
    HelixSubPage(title = stringResource(R.string.settings_gallery), onBack = onBack, modifier = modifier) {
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
