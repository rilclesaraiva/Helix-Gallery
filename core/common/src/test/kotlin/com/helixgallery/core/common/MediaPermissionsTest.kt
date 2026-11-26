// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.core.common

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaPermissionsTest {
    @Test
    fun android13PedeAsDuasPermissoesDeMidia() {
        assertEquals(
            listOf("android.permission.READ_MEDIA_IMAGES", "android.permission.READ_MEDIA_VIDEO"),
            requiredMediaPermissions(33),
        )
    }

    @Test
    fun android12PedeArmazenamento() {
        assertEquals(listOf("android.permission.READ_EXTERNAL_STORAGE"), requiredMediaPermissions(31))
    }
}
