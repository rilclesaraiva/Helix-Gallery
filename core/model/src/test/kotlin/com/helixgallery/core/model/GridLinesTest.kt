// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.core.model

import org.junit.Assert.assertArrayEquals
import org.junit.Test

class GridLinesTest {
    private fun media(id: Long) = GridEntry.Media(fakeMedia(id))
    private fun header(day: Long) = GridEntry.Header(day, GridLayout.Day)

    @Test
    fun headersTakeALineAndPhotosFillRows() {
        val entries = listOf(header(1), media(1), media(2), media(3), media(4), media(5), header(0), media(6))
        // item 0 = título da tela; 1 = dia; 2-4 e 5-6 = linhas de 3; 7 = dia; 8 = foto
        assertArrayEquals(intArrayOf(0, 1, 2, 5, 7, 8), gridLineStarts(entries, columns = 3, headerCount = 1))
    }

    @Test
    fun withoutPageHeader() {
        val entries = listOf(media(1), media(2), media(3), media(4))
        assertArrayEquals(intArrayOf(0, 2), gridLineStarts(entries, columns = 2))
    }

    @Test
    fun emptyGridHasOnlyTheHeaderLines() {
        assertArrayEquals(intArrayOf(0), gridLineStarts(emptyList(), columns = 4, headerCount = 1))
    }
}
