// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.features.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.helixgallery.core.designsystem.component.HelixFloatingBackButton
import com.helixgallery.core.designsystem.component.HelixLoading

/**
 * Passa de arquivo em arquivo com o gesto, como o visualizador da galeria, para listas cujos arquivos só
 * existem sob demanda (o cofre extrai cada um ao abrir). [resolve] devolve o endereço do arquivo da página
 * (ou null se não deu); fica fora do estado do app: não mexe em listas nem em seleção.
 */
@Composable
fun FilePagerViewer(
    count: Int,
    startIndex: Int,
    isVideo: (Int) -> Boolean,
    resolve: suspend (Int) -> String?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(initialPage = startIndex.coerceIn(0, (count - 1).coerceAtLeast(0)), pageCount = { count })
    var controlsVisible by remember { mutableStateOf(true) }
    var zoomed by remember { mutableStateOf(false) }
    LaunchedEffect(pagerState.currentPage) { zoomed = false }
    Box(modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = !zoomed,
            beyondViewportPageCount = 1,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            val uri by produceState<String?>(null, page) { value = resolve(page) }
            val resolved = uri
            when {
                resolved == null -> HelixLoading()
                isVideo(page) -> VideoPage(
                    uri = resolved,
                    isCurrent = page == pagerState.currentPage,
                    controlsVisible = controlsVisible,
                    onTap = { controlsVisible = !controlsVisible },
                    modifier = Modifier.fillMaxSize(),
                )
                else -> ZoomableImage(
                    uri = resolved,
                    onTap = { controlsVisible = !controlsVisible },
                    onZoomChanged = { if (page == pagerState.currentPage) zoomed = it },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        if (controlsVisible) HelixFloatingBackButton(onClose, Modifier.align(Alignment.TopStart))
    }
}
