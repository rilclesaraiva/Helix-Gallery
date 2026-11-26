// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.core.model

import java.time.Instant
import java.time.ZoneId

/** Como a grade de fotos se divide: sem títulos, por dia ou por mês (Ajustes → Aparência). */
enum class GridLayout { Regular, Day, Month }

/** Uma linha da grade: o título de uma seção (dia ou mês) ou uma foto. */
sealed interface GridEntry {
    /** [epochDay] é o primeiro dia da seção; o texto (data por extenso) é montado na tela, no idioma dela. */
    data class Header(val epochDay: Long, val layout: GridLayout) : GridEntry

    data class Media(val item: MediaItem) : GridEntry
}

/** O dia (contado desde 1970) em que a foto foi tirada, no fuso do aparelho. */
fun MediaItem.epochDay(zone: ZoneId = ZoneId.systemDefault()): Long =
    Instant.ofEpochMilli(takenMs).atZone(zone).toLocalDate().toEpochDay()

/** O dia da linha: o do título, ou o da foto. */
fun GridEntry.epochDay(zone: ZoneId = ZoneId.systemDefault()): Long = when (this) {
    is GridEntry.Header -> epochDay
    is GridEntry.Media -> item.epochDay(zone)
}

private fun sectionKey(item: MediaItem, layout: GridLayout, zone: ZoneId): Long {
    val date = Instant.ofEpochMilli(item.takenMs).atZone(zone).toLocalDate()
    return when (layout) {
        GridLayout.Regular -> 0L
        GridLayout.Day -> date.toEpochDay()
        GridLayout.Month -> date.year * 12L + date.monthValue
    }
}

/**
 * Monta as linhas da grade a partir da lista (mais novas primeiro). No layout
 * Regular não há títulos; nos outros, cada dia ou mês ganha um.
 */
fun buildEntries(
    items: List<MediaItem>,
    layout: GridLayout,
    zone: ZoneId = ZoneId.systemDefault(),
): List<GridEntry> {
    if (layout == GridLayout.Regular) return items.map { GridEntry.Media(it) }
    val entries = ArrayList<GridEntry>(items.size + items.size / 8)
    var lastKey: Long? = null
    for (item in items) {
        val key = sectionKey(item, layout, zone)
        if (key != lastKey) {
            entries += GridEntry.Header(item.epochDay(zone), layout)
            lastKey = key
        }
        entries += GridEntry.Media(item)
    }
    return entries
}
