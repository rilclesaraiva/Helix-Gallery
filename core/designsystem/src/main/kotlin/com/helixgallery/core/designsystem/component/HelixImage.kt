// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.core.designsystem.component

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter

/**
 * Imagem de uma foto ou de um quadro de vídeo. Só o design system conhece o Coil:
 * as telas passam o endereço (content://) e recebem a imagem já reduzida ao tamanho
 * em que aparece.
 */
@Composable
fun HelixImage(
    uri: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    contentDescription: String? = null,
    /** Chamado quando a imagem carrega, com o tamanho dela (largura, altura) como aparece na tela (já com a rotação). */
    onImageSize: ((width: Float, height: Float) -> Unit)? = null,
) {
    val parsed = remember(uri) { Uri.parse(uri) }
    Box(modifier) {
        AsyncImage(
            model = parsed,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = Modifier.matchParentSize(),
            onSuccess = onImageSize?.let { callback ->
                { state: AsyncImagePainter.State.Success -> callback(state.painter.intrinsicSize.width, state.painter.intrinsicSize.height) }
            },
        )
    }
}
