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

    @Test
    fun areaEmPixelsFicaDentroDaImagem() {
        assertEquals(listOf(100, 200, 400, 300), regionPixels(1000, 1000, TextRegion(0.1f, 0.2f, 0.5f, 0.5f)).toList())
        // Área colada na borda direita/baixo não passa do tamanho.
        assertEquals(listOf(900, 900, 100, 100), regionPixels(1000, 1000, TextRegion(0.9f, 0.9f, 1f, 1f)).toList())
    }

    @Test
    fun moverNaoSaiDaImagemENaoMudaOTamanho() {
        val moved = TextRegion(0.2f, 0.2f, 0.6f, 0.5f).movedBy(0.9f, -0.9f)
        assertEquals(TextRegion(0.6f, 0f, 1f, 0.3f).width, moved.width, 0.0001f)
        assertEquals(1f, moved.right, 0.0001f)
        assertEquals(0f, moved.top, 0.0001f)
    }

    @Test
    fun cantoNaoPassaDoTamanhoMinimoNemDaImagem() {
        val region = TextRegion(0.2f, 0.2f, 0.6f, 0.6f)
        val shrunk = region.withCorner(RegionCorner.TopLeft, 0.9f, 0.9f)
        assertEquals(0.6f - TextRegion.MIN_SIZE, shrunk.left, 0.0001f)
        assertEquals(0.6f - TextRegion.MIN_SIZE, shrunk.top, 0.0001f)
        val grown = region.withCorner(RegionCorner.BottomRight, 2f, -3f)
        assertEquals(1f, grown.right, 0.0001f)
        assertEquals(0.2f + TextRegion.MIN_SIZE, grown.bottom, 0.0001f)
    }

    @Test
    fun aumentarEDiminuirEmVoltaDoCentro() {
        val region = TextRegion(0.25f, 0.25f, 0.75f, 0.75f)
        val grown = region.scaledAroundCenter(1.5f)
        assertEquals(0.75f, grown.width, 0.0001f)
        assertEquals(0.125f, grown.left, 0.0001f)
        assertEquals(true, region.scaledAroundCenter(10f).isFull)
        assertEquals(TextRegion.MIN_SIZE, region.scaledAroundCenter(0.01f).width, 0.0001f)
    }

    @Test
    fun areaInteiraEReconhecida() {
        assertEquals(true, TextRegion.Full.isFull)
        assertEquals(false, TextRegion(0f, 0f, 1f, 0.9f).isFull)
    }

    @Test
    fun amostragemComAreaPrefereDetalheMasLimitaAImagemInteira() {
        val small = TextRegion(0f, 0f, 0.1f, 0.1f)
        // Foto de 4000 px: com a área pequena continua sem reduzir; a imagem inteira já cabe.
        assertEquals(1, decodeSampleSize(4000, 3000, small))
        // Foto enorme de 12000 px: mesmo com área pequena, a imagem toda é reduzida (2 => 6000 px; limite ~4096 => fator 2).
        assertEquals(2, decodeSampleSize(12000, 9000, small))
        // Sem área, vale o fator da imagem inteira.
        assertEquals(sampleSizeFor(12000, 9000), decodeSampleSize(12000, 9000, null))
    }
}
