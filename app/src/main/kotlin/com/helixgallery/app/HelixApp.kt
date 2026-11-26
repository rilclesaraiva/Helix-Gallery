// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.app

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.helixgallery.core.common.hasMediaAccess
import com.helixgallery.core.common.navigateOnce
import com.helixgallery.core.common.popBackStackOnce
import com.helixgallery.core.common.requiredMediaPermissions
import com.helixgallery.core.designsystem.component.HelixLogo
import com.helixgallery.core.designsystem.theme.helixScreenSpec
import com.helixgallery.core.model.MediaSource
import com.helixgallery.features.photos.CollectionScreen
import com.helixgallery.features.settings.AboutScreen
import com.helixgallery.features.settings.SettingsScreen
import com.helixgallery.features.settings.UpdatePromptHost
import com.helixgallery.features.viewer.ViewerScreen

private const val ROUTE_MAIN = "main"
private const val ROUTE_COLLECTION = "collection/{source}"
private const val ROUTE_VIEWER = "viewer/{source}/{id}"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_ABOUT = "about"

@Composable
fun HelixApp() {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(hasMediaAccess(context)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        granted = hasMediaAccess(context)
    }
    // A pessoa pode liberar nos ajustes do Android e voltar para o app.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { granted = hasMediaAccess(context) }

    if (granted) {
        HelixNavigation()
        UpdatePromptHost()
    } else {
        PermissionScreen(
            onAllow = { launcher.launch(requiredMediaPermissions().toTypedArray()) },
            onOpenSettings = {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            },
        )
    }
}

@Composable
private fun HelixNavigation() {
    val navController = rememberNavController()
    val offsetSpec = helixScreenSpec<IntOffset>()
    val fadeSpec = helixScreenSpec<Float>()

    NavHost(
        navController = navController,
        startDestination = ROUTE_MAIN,
        modifier = Modifier.background(MaterialTheme.colorScheme.surface),
        enterTransition = { slideInHorizontally(offsetSpec) { it / 4 } + fadeIn(fadeSpec) },
        exitTransition = { fadeOut(fadeSpec) },
        popEnterTransition = { fadeIn(fadeSpec) },
        popExitTransition = { slideOutHorizontally(offsetSpec) { it / 4 } + fadeOut(fadeSpec) },
    ) {
        composable(ROUTE_MAIN) {
            MainTabs(
                onOpenCollection = { source -> navController.navigateOnce("collection/${source.key}") },
                onOpenViewer = { source, id -> navController.navigateOnce("viewer/${source.key}/$id") },
                onOpenSettings = { navController.navigateOnce(ROUTE_SETTINGS) },
            )
        }
        composable(
            route = ROUTE_COLLECTION,
            arguments = listOf(navArgument("source") { type = NavType.StringType }),
        ) { entry ->
            val source = MediaSource.parse(entry.arguments?.getString("source"))
            CollectionScreen(
                source = source,
                title = collectionTitle(source),
                onOpen = { src, id -> navController.navigateOnce("viewer/${src.key}/$id") },
                onBack = { navController.popBackStackOnce() },
            )
        }
        composable(
            route = ROUTE_VIEWER,
            arguments = listOf(
                navArgument("source") { type = NavType.StringType },
                navArgument("id") { type = NavType.LongType },
            ),
        ) { entry ->
            ViewerScreen(
                source = MediaSource.parse(entry.arguments?.getString("source")),
                startId = entry.arguments?.getLong("id") ?: 0L,
                onBack = { navController.popBackStackOnce() },
            )
        }
        composable(ROUTE_SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStackOnce() },
                onOpenAbout = { navController.navigateOnce(ROUTE_ABOUT) },
            )
        }
        composable(ROUTE_ABOUT) {
            AboutScreen(onBack = { navController.popBackStackOnce() })
        }
    }
}

/** Título das coleções prontas; álbum devolve nulo e a tela usa o nome da pasta. */
@Composable
private fun collectionTitle(source: MediaSource): String? = when (source) {
    MediaSource.Favorites -> stringResource(R.string.collection_favorites)
    MediaSource.Videos -> stringResource(R.string.collection_videos)
    MediaSource.Screenshots -> stringResource(R.string.collection_screenshots)
    MediaSource.Trash -> stringResource(R.string.collection_trash)
    MediaSource.All -> stringResource(R.string.tab_photos)
    is MediaSource.Album -> null
}

@Composable
private fun PermissionScreen(onAllow: () -> Unit, onOpenSettings: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        HelixLogo()
        Text(
            text = stringResource(R.string.permission_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.permission_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onAllow) { Text(stringResource(R.string.permission_allow)) }
        TextButton(onClick = onOpenSettings) { Text(stringResource(R.string.permission_settings)) }
    }
}
