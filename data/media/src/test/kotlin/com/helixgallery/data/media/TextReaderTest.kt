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

    private fun pass(text: String, confidence: Float) = OcrPass(text, text, confidence * text.length, text.length)

    @Test
    fun leituraBoaEImagemClaraNaoGastaSegundaPassada() {
        assertEquals(false, shouldTryInverted(pass("texto bem lido", 0.95f), luminance = 0.7f))
    }

    @Test
    fun tentaInvertidaQuandoVazioPoucoConfianteOuEscuro() {
        assertEquals(true, shouldTryInverted(pass("", 0f), luminance = 0.7f))
        assertEquals(true, shouldTryInverted(pass("texto duvidoso", 0.6f), luminance = 0.7f))
        assertEquals(true, shouldTryInverted(pass("texto bem lido", 0.95f), luminance = 0.2f))
    }

    @Test
    fun escolheAVarianteComMaisConfiancaTotal() {
        val original = pass("xq", 0.9f)
        val inverted = pass("Olá, mundo", 0.9f)
        assertEquals(inverted, pickBetter(original, inverted))
    }

    @Test
    fun poucasLetrasConfiantesNaoGanhamDeTextoLongoBomEmEmpateVaiOriginal() {
        val longo = pass("um texto longo e bem lido", 0.8f)
        val curto = pass("ab", 0.99f)
        assertEquals(longo, pickBetter(longo, curto))
        val a = pass("igual", 0.9f)
        assertEquals(a, pickBetter(a, pass("igual", 0.9f)))
    }
}
