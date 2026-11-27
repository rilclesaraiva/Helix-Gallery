// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.core.model

import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StoriesTest {
    private val today = LocalDate.of(2026, 11, 26)

    @Test
    fun semFotosNaoHaLembrancas() {
        assertTrue(buildStories(emptyList(), today, ZoneOffset.UTC).isEmpty())
    }

    @Test
    fun nesteDiaAgrupaPorAnoDoMaisRecenteAoMaisAntigo() {
        val items = listOf(
            fakeMedia(3, date = LocalDate.of(2026, 11, 26)),
            fakeMedia(2, date = LocalDate.of(2024, 11, 26)),
            fakeMedia(1, date = LocalDate.of(2025, 11, 26)),
            fakeMedia(0, date = LocalDate.of(2025, 11, 26)),
        )
        val days = buildStories(items, today, ZoneOffset.UTC).filterIsInstance<Story.OnThisDay>()
        assertEquals(listOf(1, 2), days.map { it.yearsAgo })
        assertEquals(2, days[0].count)
        assertEquals(MediaSource.Day(LocalDate.of(2025, 11, 26).toEpochDay()), days[0].source)
    }

    @Test
    fun albunsRapidosSemCapturasECapaFavoritas() {
        val items = listOf(
            fakeMedia(5, bucketId = 1, bucketName = "Camera", isFavorite = true),
            fakeMedia(4, bucketId = 2, bucketName = "Screenshots", relativePath = "Pictures/Screenshots/"),
            fakeMedia(3, bucketId = 3, bucketName = "WhatsApp"),
        )
        val stories = buildStories(items, today, ZoneOffset.UTC)
        val albums = stories.filterIsInstance<Story.QuickAlbum>()
        assertEquals(listOf("Camera", "WhatsApp"), albums.map { it.name })
        assertTrue(stories.last() is Story.Favorites)
        assertEquals(1, (stories.last() as Story.Favorites).count)
    }

    @Test
    fun capaPrefereFotoAVideo() {
        val items = listOf(
            fakeMedia(2, date = LocalDate.of(2025, 11, 26), isVideo = true),
            fakeMedia(1, date = LocalDate.of(2025, 11, 26)),
        )
        val day = buildStories(items, today, ZoneOffset.UTC).filterIsInstance<Story.OnThisDay>().single()
        assertEquals(1L, day.cover.id)
    }
}
