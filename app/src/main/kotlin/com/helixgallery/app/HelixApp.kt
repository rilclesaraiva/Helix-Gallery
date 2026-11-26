// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.app

import android.content.Intent
import androidx.compose.runtime.LaunchedEffect
import android.provider.MediaStore
import android.content.Context
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.helixgallery.core.designsystem.component.HelixFloatingNav
import com.helixgallery.core.designsystem.component.HelixIcons
import com.helixgallery.core.designsystem.component.HelixNavItem
import com.helixgallery.core.designsystem.component.LocalTabReselect
import com.helixgallery.core.designsystem.theme.helixEffectsSpec
import com.helixgallery.core.designsystem.theme.helixSpatialSpec
import com.helixgallery.features.albums.AlbumsScreen
import com.helixgallery.features.search.SearchScreen
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
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
import com.helixgallery.core.model.MediaItem
import com.helixgallery.core.model.MediaSource
import com.helixgallery.features.cleanup.CleanupScreen
import com.helixgallery.features.editor.EditorScreen
import com.helixgallery.features.vault.VaultScreen
import com.helixgallery.features.viewer.ExternalViewerScreen
import com.helixgallery.features.photos.CollectionScreen
import com.helixgallery.features.settings.AboutScreen
import com.helixgallery.features.settings.AppearanceScreen
import com.helixgallery.features.settings.GallerySettingsScreen
import com.helixgallery.features.settings.SystemSettingsScreen
import com.helixgallery.features.settings.SettingsScreen
import com.helixgallery.features.settings.UpdatePromptHost
import com.helixgallery.features.viewer.ViewerScreen

private const val ROUTE_PHOTOS = "tab/photos"
private const val ROUTE_ALBUMS = "tab/albums"
private const val ROUTE_SEARCH = "tab/search"
private const val ROUTE_APPEARANCE = "settings/appearance"
private const val ROUTE_GALLERY_SETTINGS = "settings/gallery"
private const val ROUTE_SYSTEM = "settings/system"
private const val ROUTE_EDITOR = "editor?uri={uri}&name={name}&path={path}&taken={taken}"
private const val ROUTE_CLEANUP = "cleanup"
private const val ROUTE_VAULT = "vault"
private const val ROUTE_VAULT_VIEW = "vault/view?uri={uri}&video={video}"
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

    // Com o acesso às fotos liberado, pede uma vez a permissão de apagar sem painel (a pessoa pode
    // negar; em Ajustes → Galeria dá para mudar depois).
    LaunchedEffect(granted) {
        if (granted) askManageMediaOnce(context)
    }

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

private const val PREFS_NAME = "helix_permissions"
private const val KEY_ASKED_MANAGE_MEDIA = "asked_manage_media"

/** Abre uma única vez a tela do Android que libera "gerenciar mídia" (apagar e lixeira sem confirmação). */
private fun askManageMediaOnce(context: Context) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    if (prefs.getBoolean(KEY_ASKED_MANAGE_MEDIA, false) || MediaStore.canManageMedia(context)) return
    prefs.edit().putBoolean(KEY_ASKED_MANAGE_MEDIA, true).apply()
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_REQUEST_MANAGE_MEDIA, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

/** As abas da barra flutuante: cada uma é um destino do NavHost (assim guarda a própria posição). */
private enum class Tab(val route: String) {
    Photos(ROUTE_PHOTOS),
    Albums(ROUTE_ALBUMS),
    Search(ROUTE_SEARCH),
}

private val tabRoutes = Tab.entries.map { it.route }.toSet()

/** Troca entre abas (e não abrir/fechar uma tela de dentro). */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.isTabSwitch(): Boolean =
    initialState.destination.route in tabRoutes && targetState.destination.route in tabRoutes

/**
 * Voltar (e não abrir): a tela de destino está abaixo da atual na pilha, ou a atual já saiu
 * dela (voltar concluído).
 */
internal fun isBackNavigation(stack: List<NavBackStackEntry>, from: NavBackStackEntry, to: NavBackStackEntry): Boolean {
    val fromIndex = stack.indexOfFirst { it.id == from.id }
    val toIndex = stack.indexOfFirst { it.id == to.id }
    return fromIndex == -1 || (toIndex != -1 && toIndex < fromIndex)
}

@Composable
private fun HelixNavigation() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route.orEmpty()
    val onTab = route.isEmpty() || route in tabRoutes
    // Tocar de novo na aba aberta: a lista dela sobe até o topo.
    val reselect = remember { MutableSharedFlow<String>(extraBufferCapacity = 1) }
    // Itens que a pessoa mandou guardar no cofre; o cofre os recebe ao abrir.
    var pendingVault by remember { mutableStateOf<List<MediaItem>>(emptyList()) }

    // Modelo do BoomListener: telas de dentro entram deslizando por cima da anterior (que recua um
    // pouco e esmaece) e saem deslizando de volta; trocar de aba só esmaece. O "voltar" preditivo
    // usa as mesmas animações, então a tela acompanha o dedo e termina o movimento ao soltar.
    val slideSpec: FiniteAnimationSpec<IntOffset> = helixScreenSpec()
    val fadeSpec: FiniteAnimationSpec<Float> = helixEffectsSpec()
    // A direção (abrir ou voltar) é decidida pela pilha de telas, e não pelo NavHost: no voltar
    // preditivo ele às vezes usava as animações de "abrir". Assim as quatro sempre concordam.
    val forwardEnter = slideInHorizontally(slideSpec) { it }
    val forwardExit = slideOutHorizontally(slideSpec) { -it / 4 } + fadeOut(fadeSpec, targetAlpha = 0.4f)
    val backEnter = slideInHorizontally(slideSpec) { -it / 4 } + fadeIn(fadeSpec, initialAlpha = 0.4f)
    val backExit = slideOutHorizontally(slideSpec) { it }
    fun AnimatedContentTransitionScope<NavBackStackEntry>.goingBack(): Boolean =
        isBackNavigation(navController.currentBackStack.value, initialState, targetState)
    val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        when {
            isTabSwitch() -> fadeIn(fadeSpec)
            goingBack() -> backEnter
            else -> forwardEnter
        }
    }
    val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        when {
            isTabSwitch() -> fadeOut(fadeSpec)
            goingBack() -> backExit
            else -> forwardExit
        }
    }

    // Ir para uma aba. Cada aba guarda o próprio estado (rolagem, busca) ao sair e o devolve ao voltar.
    fun openTab(tab: Tab) {
        when (route) {
            tab.route -> reselect.tryEmit(tab.route)
            else -> navController.navigate(tab.route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }
    val selectedTab = Tab.entries.firstOrNull { it.route == route }
    val navItems = listOf(
        HelixNavItem(stringResource(R.string.tab_photos), HelixIcons.Photo),
        HelixNavItem(stringResource(R.string.tab_albums), HelixIcons.Collections),
        HelixNavItem(stringResource(R.string.tab_search), Icons.Filled.Search),
    )
    val barSpec: FiniteAnimationSpec<IntOffset> = helixSpatialSpec()

    CompositionLocalProvider(LocalTabReselect provides reselect) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
            NavHost(
                navController = navController,
                startDestination = ROUTE_PHOTOS,
                enterTransition = enter,
                exitTransition = exit,
                popEnterTransition = enter,
                popExitTransition = exit,
                // Sem isto o Android usa a animação padrão (a tela encolhe num cartão e some de uma vez).
                predictivePopEnterTransition = { enter(this) },
                predictivePopExitTransition = { exit(this) },
            ) {
                composable(ROUTE_PHOTOS) {
                    CollectionScreen(
                        source = MediaSource.All,
                        title = stringResource(R.string.tab_photos),
                        onOpen = { src, id -> navController.navigateOnce("viewer/${src.key}/$id") },
                        onOpenSettings = { navController.navigateOnce(ROUTE_SETTINGS) },
                        bottomInset = true,
                        reselectTab = ROUTE_PHOTOS,
                        onMoveToVault = { items ->
                            pendingVault = items
                            navController.navigateOnce(ROUTE_VAULT)
                        },
                    )
                }
                composable(ROUTE_ALBUMS) {
                    AlbumsScreen(
                        onOpenCollection = { source -> navController.navigateOnce("collection/${source.key}") },
                        onOpenSettings = { navController.navigateOnce(ROUTE_SETTINGS) },
                        onOpenVault = { navController.navigateOnce(ROUTE_VAULT) },
                        onOpenCleanup = { navController.navigateOnce(ROUTE_CLEANUP) },
                    )
                }
                composable(ROUTE_SEARCH) {
                    SearchScreen(onOpen = { id -> navController.navigateOnce("viewer/${MediaSource.All.key}/$id") })
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
                        onMoveToVault = { items ->
                            pendingVault = items
                            navController.navigateOnce(ROUTE_VAULT)
                        },
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
                        onEdit = { item ->
                            navController.navigateOnce(
                                "editor?uri=${Uri.encode(item.uri)}&name=${Uri.encode(item.displayName)}" +
                                    "&path=${Uri.encode(item.relativePath)}&taken=${item.takenMs}",
                            )
                        },
                    )
                }
                composable(
                    route = ROUTE_EDITOR,
                    arguments = listOf(
                        navArgument("uri") { type = NavType.StringType },
                        navArgument("name") { type = NavType.StringType },
                        navArgument("path") { type = NavType.StringType },
                        navArgument("taken") { type = NavType.LongType },
                    ),
                ) { entry ->
                    EditorScreen(
                        uri = entry.arguments?.getString("uri").orEmpty(),
                        displayName = entry.arguments?.getString("name").orEmpty(),
                        relativePath = entry.arguments?.getString("path").orEmpty(),
                        takenMs = entry.arguments?.getLong("taken") ?: 0L,
                        onClose = { navController.popBackStackOnce() },
                    )
                }
                composable(ROUTE_CLEANUP) { CleanupScreen(onBack = { navController.popBackStackOnce() }) }
                composable(ROUTE_VAULT) {
                    VaultScreen(
                        onBack = { navController.popBackStackOnce() },
                        onOpenEntry = { uri, isVideo ->
                            navController.navigateOnce("vault/view?uri=${Uri.encode(uri)}&video=$isVideo")
                        },
                        pending = pendingVault,
                        onPendingHandled = { pendingVault = emptyList() },
                    )
                }
                composable(
                    route = ROUTE_VAULT_VIEW,
                    arguments = listOf(
                        navArgument("uri") { type = NavType.StringType },
                        navArgument("video") { type = NavType.BoolType },
                    ),
                ) { entry ->
                    ExternalViewerScreen(
                        uri = entry.arguments?.getString("uri").orEmpty(),
                        isVideo = entry.arguments?.getBoolean("video") ?: false,
                        onClose = { navController.popBackStackOnce() },
                    )
                }
                composable(ROUTE_SETTINGS) {
                    SettingsScreen(
                        onBack = { navController.popBackStackOnce() },
                        onOpenAppearance = { navController.navigateOnce(ROUTE_APPEARANCE) },
                        onOpenGallery = { navController.navigateOnce(ROUTE_GALLERY_SETTINGS) },
                        onOpenSystem = { navController.navigateOnce(ROUTE_SYSTEM) },
                        onOpenAbout = { navController.navigateOnce(ROUTE_ABOUT) },
                    )
                }
                composable(ROUTE_APPEARANCE) { AppearanceScreen(onBack = { navController.popBackStackOnce() }) }
                composable(ROUTE_GALLERY_SETTINGS) { GallerySettingsScreen(onBack = { navController.popBackStackOnce() }) }
                composable(ROUTE_SYSTEM) { SystemSettingsScreen(onBack = { navController.popBackStackOnce() }) }
                composable(ROUTE_ABOUT) {
                    AboutScreen(onBack = { navController.popBackStackOnce() })
                }
            }
            // A barra flutuante só aparece nas abas; telas de dentro ficam com a tela toda.
            AnimatedVisibility(
                visible = onTab,
                enter = slideInVertically(barSpec) { it } + fadeIn(fadeSpec),
                exit = slideOutVertically(barSpec) { it } + fadeOut(fadeSpec),
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                HelixFloatingNav(
                    items = navItems,
                    selectedIndex = selectedTab?.ordinal ?: 0,
                    onSelect = { openTab(Tab.entries[it]) },
                )
            }
            // Nas abas, voltar de Álbuns/Busca leva à aba Fotos antes de sair do app.
            BackHandler(enabled = onTab && selectedTab != null && selectedTab != Tab.Photos) { openTab(Tab.Photos) }
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
