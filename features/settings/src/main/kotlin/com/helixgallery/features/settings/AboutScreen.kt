// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.helixgallery.core.common.openUrl
import com.helixgallery.core.designsystem.component.HelixAccent
import com.helixgallery.core.designsystem.component.HelixIcons
import com.helixgallery.core.designsystem.component.HelixLogo
import com.helixgallery.core.designsystem.component.HelixSettingsItem
import com.helixgallery.core.designsystem.component.HelixSettingsSection
import com.helixgallery.core.designsystem.component.HelixSubPage
import com.helixgallery.data.updates.DeveloperAvatarData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val REPO = "https://github.com/rilclesaraiva/Helix-Gallery"
private const val DEVELOPER_URL = "https://github.com/rilclesaraiva"
private const val PAGES = "https://rilclesaraiva.github.io/Helix-Gallery"

@Composable
fun AboutScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    // Foto do GitHub do desenvolvedor: a guardada aparece na hora; o GitHub só é consultado de vez em quando.
    val developerPhoto by produceState<ImageBitmap?>(null) {
        val avatars = DeveloperAvatarData.repository(context)
        value = withContext(Dispatchers.IO) { avatars.cached() }?.asImageBitmap()
        avatars.refresh()?.let { value = it.asImageBitmap() }
    }
    HelixSubPage(title = stringResource(R.string.about_title), onBack = onBack, modifier = modifier) {
        Column(
            Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            HelixLogo(size = 112.dp)
            Text(stringResource(R.string.about_app_name), style = MaterialTheme.typography.headlineMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AboutChip(appVersionName(context))
                AboutChip(stringResource(R.string.about_build_release).uppercase())
            }
        }
        Text(
            text = stringResource(R.string.about_description),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 8.dp),
        )
        HelixSettingsSection(title = stringResource(R.string.about_developer)) {
            HelixSettingsItem(
                icon = HelixIcons.Person,
                accent = HelixAccent.Pink,
                title = "rilclesaraiva",
                subtitle = stringResource(R.string.about_developer_desc),
                onClick = { context.openUrl(DEVELOPER_URL) },
                image = developerPhoto,
            )
        }
        HelixSettingsSection(title = stringResource(R.string.about_project)) {
            HelixSettingsItem(
                icon = HelixIcons.Code,
                accent = HelixAccent.Blue,
                title = stringResource(R.string.about_source),
                subtitle = stringResource(R.string.about_source_desc),
                onClick = { context.openUrl(REPO) },
            )
            HelixSettingsItem(
                icon = HelixIcons.NewReleases,
                accent = HelixAccent.Orange,
                title = stringResource(R.string.about_changelog),
                subtitle = stringResource(R.string.about_changelog_desc),
                onClick = { context.openUrl("$REPO/blob/main/CHANGELOG.md") },
            )
            HelixSettingsItem(
                icon = HelixIcons.BugReport,
                accent = HelixAccent.Red,
                title = stringResource(R.string.about_issues),
                subtitle = stringResource(R.string.about_issues_desc),
                onClick = { context.openUrl("$REPO/issues") },
            )
        }
        HelixSettingsSection(title = stringResource(R.string.about_legal)) {
            HelixSettingsItem(
                icon = HelixIcons.Lock,
                accent = HelixAccent.Green,
                title = stringResource(R.string.about_privacy),
                subtitle = stringResource(R.string.about_privacy_desc),
                onClick = { context.openUrl("$PAGES/privacy") },
            )
            HelixSettingsItem(
                icon = HelixIcons.Article,
                accent = HelixAccent.Blue,
                title = stringResource(R.string.about_terms),
                subtitle = stringResource(R.string.about_terms_desc),
                onClick = { context.openUrl("$PAGES/terms") },
            )
            HelixSettingsItem(
                icon = HelixIcons.Article,
                accent = HelixAccent.Blue,
                title = stringResource(R.string.about_licenses),
                subtitle = stringResource(R.string.about_licenses_desc),
                onClick = { context.openUrl("$PAGES/licenses") },
            )
        }
        // Convite de apoio: só leva ao repositório (no app, nada de doação ou pagamento).
        HelixSettingsSection(title = null) {
            HelixSettingsItem(
                icon = HelixIcons.Heart,
                accent = HelixAccent.Red,
                title = stringResource(R.string.about_support),
                subtitle = stringResource(R.string.about_support_desc),
                onClick = { context.openUrl(REPO) },
            )
        }
    }
}

@Composable
private fun AboutChip(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = CircleShape,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp))
    }
}
