// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.core.model

import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaSearchTest {
    private val utc = ZoneOffset.UTC
    private val locale = Locale.ENGLISH

    private val foto = fakeMedia(1, name = "Praia.jpg", bucketName = "Viagem")
    private val video = fakeMedia(2, name = "Show.mp4", isVideo = true)

    @Test
    fun buscaVaziaNaoAchaNada() {
        assertTrue(searchMedia(listOf(foto), "  ", locale, utc).isEmpty())
    }

    @Test
    fun ignoraAcentoEMaiuscula() {
        assertEquals(listOf(video), searchMedia(listOf(foto, video), "VÍDEO", locale, utc))
    }

    @Test
    fun achaPorNomeEPasta() {
        assertEquals(listOf(foto), searchMedia(listOf(foto, video), "praia", locale, utc))
        assertEquals(listOf(foto), searchMedia(listOf(foto, video), "viagem", locale, utc))
    }

    @Test
    fun todasAsPalavrasPrecisamCasar() {
        assertTrue(searchMedia(listOf(foto), "praia inexistente", locale, utc).isEmpty())
    }

    @Test
    fun normalizaTexto() {
        assertEquals("video", normalizeSearchText("Vídeo"))
    }
}
