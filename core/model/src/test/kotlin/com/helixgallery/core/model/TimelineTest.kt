// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.core.model

import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineTest {
    private val utc = ZoneOffset.UTC

    private val items = listOf(
        fakeMedia(3, LocalDate.of(2026, 11, 20)),
        fakeMedia(2, LocalDate.of(2026, 11, 20)),
        fakeMedia(1, LocalDate.of(2026, 10, 5)),
    )

    @Test
    fun regularNaoTemTitulos() {
        val entries = buildEntries(items, GridLayout.Regular, utc)
        assertEquals(3, entries.size)
        assertTrue(entries.all { it is GridEntry.Media })
    }

    @Test
    fun porDiaCriaUmTituloPorDia() {
        val entries = buildEntries(items, GridLayout.Day, utc)
        assertEquals(5, entries.size)
        assertEquals(2, entries.count { it is GridEntry.Header })
        assertTrue(entries.first() is GridEntry.Header)
    }

    @Test
    fun porMesCriaUmTituloPorMes() {
        val more = items + fakeMedia(0, LocalDate.of(2026, 10, 1))
        val entries = buildEntries(more, GridLayout.Month, utc)
        assertEquals(2, entries.count { it is GridEntry.Header })
    }

    @Test
    fun listaVaziaNaoTemEntradas() {
        assertTrue(buildEntries(emptyList(), GridLayout.Day, utc).isEmpty())
    }
}
