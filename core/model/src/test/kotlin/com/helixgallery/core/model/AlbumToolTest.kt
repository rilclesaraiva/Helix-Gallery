// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AlbumToolTest {
    @Test
    fun nothingStoredMeansDefault() {
        assertEquals(DEFAULT_ALBUM_TOOLS, parseAlbumTools(null))
    }

    @Test
    fun emptyTextMeansNonePinned() {
        assertEquals(emptyList<AlbumTool>(), parseAlbumTools(""))
    }

    @Test
    fun roundTripKeepsOrder() {
        val tools = listOf(AlbumTool.Vault, AlbumTool.Favorites)
        assertEquals(tools, parseAlbumTools(encodeAlbumTools(tools)))
    }

    @Test
    fun unknownAndRepeatedNamesAreDropped() {
        assertEquals(listOf(AlbumTool.Trash), parseAlbumTools("Nope,Trash,Trash"))
    }

    @Test
    fun movingStopsAtTheEnds() {
        val tools = listOf(AlbumTool.Vault, AlbumTool.Trash, AlbumTool.Videos)
        assertEquals(listOf(AlbumTool.Trash, AlbumTool.Vault, AlbumTool.Videos), moveAlbumTool(tools, AlbumTool.Trash, -1))
        assertEquals(tools, moveAlbumTool(tools, AlbumTool.Vault, -1))
        assertEquals(tools, moveAlbumTool(tools, AlbumTool.Videos, 1))
    }
}
