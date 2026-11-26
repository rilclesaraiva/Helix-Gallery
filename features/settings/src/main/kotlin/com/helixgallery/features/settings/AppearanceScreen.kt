// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.features.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.helixgallery.core.designsystem.component.HelixSettingsItem
import com.helixgallery.core.designsystem.component.HelixSettingsSection
import com.helixgallery.core.designsystem.component.HelixSubPage
import com.helixgallery.core.model.ThemeMode
import java.util.Locale

@Composable
fun AppearanceScreen(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: SettingsViewModel = viewModel()) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val context = LocalContext.current
    HelixSubPage(title = stringResource(R.string.settings_appearance), onBack = onBack, modifier = modifier) {
        HelixSettingsSection(title = stringResource(R.string.settings_theme)) {
            ThemeMode.entries.forEach { mode ->
                RadioSettingsItem(
                    title = stringResource(themeLabel(mode)),
                    subtitle = themeSummary(mode)?.let { stringResource(it) },
                    selected = prefs.themeMode == mode,
                    onClick = { viewModel.setThemeMode(mode) },
                )
            }
        }
        HelixSettingsSection(title = stringResource(R.string.settings_colors)) {
            HelixSettingsItem(
                icon = null,
                title = stringResource(R.string.settings_dynamic_color),
                subtitle = stringResource(R.string.settings_dynamic_color_desc),
                onClick = { viewModel.setDynamicColor(!prefs.dynamicColor) },
                trailing = { Switch(checked = prefs.dynamicColor, onCheckedChange = viewModel::setDynamicColor) },
            )
        }
        HelixSettingsSection(title = stringResource(R.string.settings_language)) {
            // O idioma por app é escolhido nos ajustes do Android (minSdk 31, então o Android 13+ tem a tela).
            val perApp = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU
            HelixSettingsItem(
                icon = null,
                title = stringResource(R.string.settings_language_app),
                subtitle = if (perApp) {
                    Locale.getDefault().let { it.getDisplayLanguage(it).replaceFirstChar(Char::titlecase) }
                } else {
                    stringResource(R.string.settings_language_follows_phone)
                },
                enabled = perApp,
                onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_APP_LOCALE_SETTINGS)
                                .setData(Uri.fromParts("package", context.packageName, null))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }
                },
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

private fun themeSummary(mode: ThemeMode): Int? = when (mode) {
    ThemeMode.System -> R.string.theme_system_summary
    ThemeMode.Amoled -> R.string.theme_amoled_summary
    else -> null
}
