// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.features.settings

import android.content.Context
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.helixgallery.core.designsystem.component.HelixAccent
import com.helixgallery.core.designsystem.component.HelixIcons
import com.helixgallery.core.designsystem.component.HelixSettingsItem
import com.helixgallery.core.designsystem.component.HelixSettingsSection
import com.helixgallery.core.designsystem.component.HelixSubPage

/** Início dos Ajustes: categorias que abrem cada subpágina (modelo do BoomListener). */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenGallery: () -> Unit,
    onOpenSystem: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val version = appVersionName(LocalContext.current)
    HelixSubPage(title = stringResource(R.string.settings_title), onBack = onBack, modifier = modifier) {
        HelixSettingsSection(title = stringResource(R.string.settings_section_general)) {
            HelixSettingsItem(
                icon = HelixIcons.Palette,
                accent = HelixAccent.Purple,
                title = stringResource(R.string.settings_appearance),
                subtitle = stringResource(R.string.settings_appearance_summary),
                onClick = onOpenAppearance,
            )
            HelixSettingsItem(
                icon = HelixIcons.Photo,
                accent = HelixAccent.Teal,
                title = stringResource(R.string.settings_gallery),
                subtitle = stringResource(R.string.settings_gallery_summary),
                onClick = onOpenGallery,
            )
            HelixSettingsItem(
                icon = HelixIcons.Settings,
                accent = HelixAccent.Green,
                title = stringResource(R.string.settings_system),
                subtitle = stringResource(R.string.settings_system_summary),
                onClick = onOpenSystem,
            )
        }
        HelixSettingsSection(title = stringResource(R.string.settings_section_about)) {
            HelixSettingsItem(
                icon = HelixIcons.Info,
                accent = HelixAccent.Blue,
                title = stringResource(R.string.settings_about),
                subtitle = stringResource(R.string.settings_about_summary, version),
                onClick = onOpenAbout,
            )
        }
    }
}

/** Versão instalada (vem do APP_VERSION, via versionName). */
@Suppress("DEPRECATION")
internal fun appVersionName(context: Context): String =
    runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
        .getOrNull()?.removeSuffix("-debug") ?: "?"

/** Linha de escolha única: título, resumo opcional e o botão de rádio à direita. */
@Composable
internal fun RadioSettingsItem(title: String, selected: Boolean, onClick: () -> Unit, subtitle: String? = null) {
    HelixSettingsItem(
        icon = null,
        title = title,
        subtitle = subtitle,
        onClick = onClick,
        trailing = { RadioButton(selected = selected, onClick = onClick) },
    )
}
