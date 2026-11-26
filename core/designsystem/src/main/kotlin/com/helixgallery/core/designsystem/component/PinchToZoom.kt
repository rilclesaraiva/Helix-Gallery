// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.core.designsystem.component

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.runtime.getValue
import androidx.compose.ui.composed
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput

/** Quanto a pinça precisa fechar/abrir (1 = nada) para valer um passo. */
private const val PINCH_STEP = 1.3f

/**
 * Gesto de pinça (dois dedos) sobre a lista: cada vez que a pinça se afasta ou se aproxima o
 * bastante, chama [onStep] (`zoomOut` true quando os dedos se juntam, as fotos ficam menores).
 * Com um dedo só, não mexe em nada: a rolagem segue normal. Com dois, o gesto fica para a pinça.
 */
fun Modifier.pinchToZoom(onStep: (zoomOut: Boolean) -> Unit): Modifier = composed {
    val step by rememberUpdatedState(onStep)
    pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            var accumulated = 1f
            do {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.changes.count { it.pressed } >= 2) {
                    accumulated *= event.calculateZoom()
                    event.changes.forEach { it.consume() }
                    if (accumulated >= PINCH_STEP) {
                        step(false)
                        accumulated = 1f
                    } else if (accumulated <= 1f / PINCH_STEP) {
                        step(true)
                        accumulated = 1f
                    }
                }
            } while (event.changes.any { it.pressed })
        }
    }
}
