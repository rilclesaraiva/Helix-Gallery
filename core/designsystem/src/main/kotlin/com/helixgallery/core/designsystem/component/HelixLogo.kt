// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.helixgallery.core.designsystem.R

/** A Nebulosa da Hélice: o ícone do app, renderizado do SVG original (com o desfoque e a textura). */
@Composable
fun HelixLogo(modifier: Modifier = Modifier, size: Dp = 96.dp) {
    Image(painter = painterResource(R.drawable.helix_logo), contentDescription = null, modifier = modifier.size(size))
}
