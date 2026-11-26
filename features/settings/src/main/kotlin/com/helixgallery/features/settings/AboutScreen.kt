// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.helixgallery.core.common.openUrl
import com.helixgallery.core.designsystem.component.HelixAccent
import com.helixgallery.core.designsystem.component.HelixLogo
import com.helixgallery.core.designsystem.component.HelixSettingsItem
import com.helixgallery.core.designsystem.component.HelixSettingsSection
import com.helixgallery.core.designsystem.component.HelixSubPage

private const val REPO = "https://github.com/rilclesaraiva/Helix-Gallery"
private const val PAGES = "https://rilclesaraiva.github.io/Helix-Gallery"
private const val CONTACT = "mailto:saraivarilcle@gmail.com"

@Composable
fun AboutScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    HelixSubPage(title = stringResource(R.string.about_title), onBack = onBack, modifier = modifier) {
        Column(
            Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HelixLogo()
            Text(
                text = stringResource(R.string.about_app_name),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.about_version, appVersionName(context)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.about_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        HelixSettingsSection(title = stringResource(R.string.about_project)) {
            HelixSettingsItem(
                icon = Icons.Filled.Create,
                title = stringResource(R.string.about_source),
                subtitle = stringResource(R.string.about_source_desc),
                accent = HelixAccent.Purple,
                onClick = { context.openUrl(REPO) },
            )
            HelixSettingsItem(
                icon = Icons.Filled.Warning,
                title = stringResource(R.string.about_issues),
                subtitle = stringResource(R.string.about_issues_desc),
                accent = HelixAccent.Orange,
                onClick = { context.openUrl("$REPO/issues") },
            )
            HelixSettingsItem(
                icon = Icons.Filled.Email,
                title = stringResource(R.string.about_contact),
                subtitle = "saraivarilcle@gmail.com",
                accent = HelixAccent.Blue,
                onClick = { context.openUrl(CONTACT) },
            )
        }
        HelixSettingsSection(title = stringResource(R.string.about_legal)) {
            HelixSettingsItem(
                icon = Icons.Filled.Info,
                title = stringResource(R.string.about_license),
                subtitle = stringResource(R.string.about_license_desc),
                accent = HelixAccent.Green,
                onClick = { context.openUrl("$REPO/blob/main/LICENSE") },
            )
            HelixSettingsItem(
                icon = Icons.Filled.Lock,
                title = stringResource(R.string.about_privacy),
                accent = HelixAccent.Teal,
                onClick = { context.openUrl("$PAGES/privacy") },
            )
            HelixSettingsItem(
                icon = Icons.AutoMirrored.Filled.List,
                title = stringResource(R.string.about_terms),
                accent = HelixAccent.Neutral,
                onClick = { context.openUrl("$PAGES/terms") },
            )
            HelixSettingsItem(
                icon = Icons.AutoMirrored.Filled.List,
                title = stringResource(R.string.about_licenses),
                subtitle = stringResource(R.string.about_licenses_desc),
                accent = HelixAccent.Neutral,
                onClick = { context.openUrl("$PAGES/licenses") },
            )
        }
    }
}
