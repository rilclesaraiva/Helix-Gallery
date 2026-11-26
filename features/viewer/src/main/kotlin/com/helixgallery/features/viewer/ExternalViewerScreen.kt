// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.features.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.helixgallery.core.designsystem.component.HelixFloatingBackButton

/**
 * "Abrir com": uma foto ou um vídeo vindo de outro app, sem passar pela galeria. Não mexe em nada
 * do estado do app (listas, seleção): só mostra o arquivo, com zoom nas fotos e os controles nos vídeos.
 */
@Composable
fun ExternalViewerScreen(
    uri: String,
    isVideo: Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var controlsVisible by remember { mutableStateOf(true) }
    Box(modifier.fillMaxSize().background(Color.Black)) {
        if (isVideo) {
            VideoPage(
                uri = uri,
                isCurrent = true,
                controlsVisible = controlsVisible,
                onTap = { controlsVisible = !controlsVisible },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            ZoomableImage(
                uri = uri,
                onTap = { controlsVisible = !controlsVisible },
                onZoomChanged = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (controlsVisible) HelixFloatingBackButton(onClose, Modifier.align(Alignment.TopStart))
    }
}
