// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.data.media

import org.junit.Assert.assertEquals
import org.junit.Test

class TextReaderTest {
    @Test
    fun imagemPequenaNaoReduz() {
        assertEquals(1, sampleSizeFor(1080, 2400))
    }

    @Test
    fun fotoGrandeReduzPorPotenciaDeDois() {
        assertEquals(1, sampleSizeFor(4000, 3000)) // 4000/2 = 2000 < 2048
        assertEquals(2, sampleSizeFor(5000, 3000)) // 5000/2 = 2500 >= 2048
        assertEquals(4, sampleSizeFor(9000, 6000)) // 9000/4 = 2250 >= 2048, 9000/8 < 2048
    }

    @Test
    fun unePorBlocoELinhasEmUmParagrafo() {
        val blocks = listOf(
            listOf("Claude, simule 10 mil", "versões da minha vida"),
            listOf("Unir-se"),
        )
        assertEquals("Claude, simule 10 mil versões da minha vida\nUnir-se", flowText(blocks))
    }

    @Test
    fun ignoraLinhasEBlocosVazios() {
        assertEquals("a b", flowText(listOf(listOf(" a ", "", "b"), listOf(" "), emptyList())))
        assertEquals("", flowText(emptyList()))
    }

    @Test
    fun usaOLadoMaior() {
        assertEquals(sampleSizeFor(6000, 1000), sampleSizeFor(1000, 6000))
    }
}
