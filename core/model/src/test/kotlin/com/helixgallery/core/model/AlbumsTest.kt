// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AlbumsTest {
    @Test
    fun agrupaPorPastaECameraVemPrimeiro() {
        val items = listOf(
            fakeMedia(3, bucketId = 2, bucketName = "Zap"),
            fakeMedia(2, bucketId = 1, bucketName = "Camera"),
            fakeMedia(1, bucketId = 2, bucketName = "Zap"),
        )
        val albums = buildAlbums(items)
        assertEquals(listOf("Camera", "Zap"), albums.map { it.name })
        assertEquals(2, albums[1].count)
        assertEquals(3L, albums[1].cover.id)
    }
}
