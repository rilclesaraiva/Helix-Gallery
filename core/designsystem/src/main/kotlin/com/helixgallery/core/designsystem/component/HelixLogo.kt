// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** A Nebulosa da Hélice desenhada em código: anel ciano, miolo âmbar e toques de magenta. */
@Composable
fun HelixLogo(modifier: Modifier = Modifier, size: Dp = 96.dp) {
    Canvas(modifier.size(size)) {
        val c = Offset(this.size.width / 2f, this.size.height / 2f)
        val r = this.size.minDimension / 2f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF0B1220), Color(0xFF0B1220)),
                center = c,
                radius = r,
            ),
            radius = r,
            center = c,
        )
        drawCircle(
            color = Color(0xFFFF8F3F).copy(alpha = 0.85f),
            radius = r * 0.30f,
            center = c,
        )
        drawCircle(
            color = Color(0xFF0B1220),
            radius = r * 0.16f,
            center = c,
        )
        drawCircle(
            color = Color(0xFF7FD7E8),
            radius = r * 0.62f,
            center = c,
            style = Stroke(width = r * 0.16f),
        )
        drawCircle(
            color = Color(0xFFFF4FD8).copy(alpha = 0.75f),
            radius = r * 0.80f,
            center = c,
            style = Stroke(width = r * 0.07f),
        )
    }
}
