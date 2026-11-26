// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.app

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.helixgallery.core.datastore.HelixPreferences
import com.helixgallery.core.designsystem.theme.HelixTheme
import com.helixgallery.core.model.UserPreferences
import com.helixgallery.features.viewer.ExternalViewerScreen
import java.io.File

/**
 * "Abrir com" o Helix Gallery numa foto ou vídeo de outro app. Tela própria, fora da navegação do
 * app: fechar volta direto ao app de onde veio o arquivo.
 */
class ExternalViewerActivity : ComponentActivity() {
    private var target by mutableStateOf<Pair<String, Boolean>?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        handle(intent)
        setContent {
            val prefs by HelixPreferences.repository(LocalContext.current).preferences
                .collectAsStateWithLifecycle(initialValue = UserPreferences())
            HelixTheme(themeMode = prefs.themeMode, dynamicColor = prefs.dynamicColor) {
                target?.let { (uri, isVideo) ->
                    ExternalViewerScreen(uri = uri, isVideo = isVideo, onClose = ::finish)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    /** `file://` dentro da pasta de dados do app: outro app não pode mandar abrir isso. */
    private fun pointsToOwnPrivateData(uri: Uri): Boolean {
        if (uri.scheme != "file") return false
        val path = runCatching { File(uri.path.orEmpty()).canonicalPath }.getOrNull() ?: return true
        return listOfNotNull(applicationInfo.dataDir, applicationInfo.deviceProtectedDataDir).any { root ->
            val base = runCatching { File(root).canonicalPath }.getOrNull() ?: return@any false
            path == base || path.startsWith(base + File.separator)
        }
    }

    private fun handle(intent: Intent?) {
        val uri = intent?.data ?: return finish()
        if (pointsToOwnPrivateData(uri)) return finish()
        val type = intent.type ?: runCatching { contentResolver.getType(uri) }.getOrNull()
        target = uri.toString() to (type?.startsWith("video/") == true)
    }
}
