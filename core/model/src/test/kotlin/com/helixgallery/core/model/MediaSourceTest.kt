// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaSourceTest {

    @Test
    fun parseVoltaDaChave() {
        listOf(
            MediaSource.All,
            MediaSource.Favorites,
            MediaSource.Videos,
            MediaSource.Screenshots,
            MediaSource.Trash,
            MediaSource.Album(42),
            MediaSource.Day(20_000),
        ).forEach { assertEquals(it, MediaSource.parse(it.key)) }
    }

    @Test
    fun chaveDesconhecidaCaiEmTudo() {
        assertEquals(MediaSource.All, MediaSource.parse(null))
        assertEquals(MediaSource.All, MediaSource.parse("lixo"))
        assertEquals(MediaSource.All, MediaSource.parse("album_abc"))
    }

    @Test
    fun selectMediaFiltraCadaOrigem() {
        val foto = fakeMedia(1)
        val favorita = fakeMedia(2, isFavorite = true)
        val video = fakeMedia(3, isVideo = true, bucketId = 2, bucketName = "Movies")
        val captura = fakeMedia(4, bucketId = 3, bucketName = "Screenshots", relativePath = "Pictures/Screenshots/")
        val apagada = fakeMedia(5)
        val state = MediaState(listOf(foto, favorita, video, captura), listOf(apagada), loaded = true)

        assertEquals(4, selectMedia(MediaSource.All, state).size)
        assertEquals(listOf(favorita), selectMedia(MediaSource.Favorites, state))
        assertEquals(listOf(video), selectMedia(MediaSource.Videos, state))
        assertEquals(listOf(captura), selectMedia(MediaSource.Screenshots, state))
        assertEquals(listOf(apagada), selectMedia(MediaSource.Trash, state))
        assertEquals(listOf(video), selectMedia(MediaSource.Album(2), state))
    }

    @Test
    fun capturaEmPortuguesTambemContaComoCaptura() {
        assertTrue(fakeMedia(1, bucketName = "Capturas de tela", relativePath = "Pictures/Capturas de tela/").isScreenshot())
    }
}
