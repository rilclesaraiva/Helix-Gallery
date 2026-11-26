// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.helixgallery.core.designsystem.theme.helixFastSpatialSpec

private const val BEAT_BIG = 1.4f
private const val BEAT_SMALL = 1.28f

/**
 * O "tum tum" do coração ao favoritar (porte do `heartbeat` do BoomListener): expande no lugar, duas
 * batidas, a segunda um pouco menor. Dispara quando [trigger] (um contador) muda para um valor maior que zero.
 */
@Composable
fun Modifier.heartbeat(trigger: Int): Modifier {
    val scale = remember { Animatable(1f) }
    val spec = helixFastSpatialSpec<Float>()
    // Só bate quando o contador muda depois de entrar na tela (não ao reaparecer com valor antigo).
    val seen = remember { intArrayOf(trigger) }
    LaunchedEffect(trigger) {
        if (trigger <= 0 || trigger == seen[0]) return@LaunchedEffect
        seen[0] = trigger
        scale.animateTo(BEAT_BIG, spec)
        scale.animateTo(1f, spec)
        scale.animateTo(BEAT_SMALL, spec)
        scale.animateTo(1f, spec)
    }
    return this.graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}
