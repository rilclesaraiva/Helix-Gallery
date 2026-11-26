// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.core.designsystem.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** Ícones que o conjunto básico do Material não traz (traçados do Material Symbols, 24 dp). */
object HelixIcons {
    private fun vec(name: String, path: String): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).addPath(
            pathData = PathParser().parsePathString(path).toNodes(),
            fill = SolidColor(Color.Black),
        ).build()

    val Photo: ImageVector by lazy {
        vec(
            "Photo",
            "M21 19V5c0-1.1-0.9-2-2-2H5c-1.1 0-2 0.9-2 2v14c0 1.1 0.9 2 2 2h14c1.1 0 2-0.9 2-2zM8.5 13.5l2.5 3.01L14.5 12l4.5 6H5l3.5-4.5z",
        )
    }

    val Collections: ImageVector by lazy {
        vec(
            "Collections",
            "M22 16V4c0-1.1-0.9-2-2-2H8c-1.1 0-2 0.9-2 2v12c0 1.1 0.9 2 2 2h12c1.1 0 2-0.9 2-2zm-11-4l2.03 2.71L16 11l4 5H8l3-4zM2 6v14c0 1.1 0.9 2 2 2h14v-2H4V6H2z",
        )
    }

    val Videocam: ImageVector by lazy {
        vec(
            "Videocam",
            "M17 10.5V7c0-0.55-0.45-1-1-1H4c-0.55 0-1 0.45-1 1v10c0 0.55 0.45 1 1 1h12c0.55 0 1-0.45 1-1v-3.5l4 4v-11l-4 4z",
        )
    }

    val Pause: ImageVector by lazy {
        vec("Pause", "M6 19h4V5H6v14zm8-14v14h4V5h-4z")
    }

    val VolumeUp: ImageVector by lazy {
        vec(
            "VolumeUp",
            "M3 9v6h4l5 5V4L7 9H3zm13.5 3c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-0.73 2.5-2.25 2.5-4.02zM14 3.23v2.06c2.89 0.86 5 3.54 5 6.71s-2.11 5.85-5 6.71v2.06c4.01-0.91 7-4.49 7-8.77s-2.99-7.86-7-8.77z",
        )
    }

    val VolumeOff: ImageVector by lazy {
        vec(
            "VolumeOff",
            "M16.5 12c0-1.77-1.02-3.29-2.5-4.03v2.21l2.45 2.45c0.03-0.2 0.05-0.41 0.05-0.63zm2.5 0c0 0.94-0.2 1.82-0.54 2.64l1.51 1.51C20.63 14.91 21 13.5 21 12c0-4.28-2.99-7.86-7-8.77v2.06c2.89 0.86 5 3.54 5 6.71zM4.27 3L3 4.27 7.73 9H3v6h4l5 5v-6.73l4.25 4.25c-0.67 0.52-1.42 0.93-2.25 1.18v2.06c1.38-0.31 2.63-0.95 3.69-1.81L19.73 21 21 19.73l-9-9L4.27 3zM12 4L9.91 6.09 12 8.18V4z",
        )
    }
}
