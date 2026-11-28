// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.data.media

import org.junit.Assert.assertEquals
import org.junit.Test

class ColorSampleTest {
    @Test
    fun hexTemSeisDigitosEmMaiusculas() {
        assertEquals("#716E6F", hexOf(0x716E6F))
        assertEquals("#000000", hexOf(0))
        assertEquals("#00000A", hexOf(0x00000A))
        assertEquals("#FFFFFF", hexOf(0xFFFFFF))
    }

    @Test
    fun hexIgnoraOAlfa() {
        assertEquals("#102030", hexOf(0xFF102030.toInt()))
    }

    @Test
    fun mediaDosPixels() {
        val pixels = intArrayOf(0xFF000000.toInt(), 0xFFFFFFFF.toInt())
        // (0 + 255) / 2 = 127 em cada canal.
        assertEquals(0x7F7F7F, averageRgb(pixels))
    }

    @Test
    fun mediaDeUmPixelEOPixel() {
        assertEquals(0x3F3C3D, averageRgb(intArrayOf(0xFF3F3C3D.toInt())))
    }

    @Test
    fun mediaVaziaEPreto() {
        assertEquals(0, averageRgb(IntArray(0)))
    }

    @Test
    fun mediaNaoEstouraComMuitosPixelsClaros() {
        val pixels = IntArray(100_000) { 0xFFFFFFFF.toInt() }
        assertEquals(0xFFFFFF, averageRgb(pixels))
    }

    @Test
    fun sensibilidadeCaiComOZoomEFicaNosLimites() {
        assertEquals(0.6f, panSensitivity(1f), 0.0001f)
        assertEquals(0.6f, panSensitivity(0.5f), 0.0001f)
        assertEquals(0.3f, panSensitivity(4f), 0.0001f)
        assertEquals(0.15f, panSensitivity(16f), 0.0001f)
        assertEquals(0.1f, panSensitivity(40f), 0.0001f)
        assertEquals(true, panSensitivity(8f) < panSensitivity(2f))
    }
}
