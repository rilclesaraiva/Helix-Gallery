// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.core.model

import java.time.LocalDate
import java.time.ZoneId

/**
 * Um cartão da fileira de lembranças no topo da aba Fotos (estilo "stories"). Tudo é montado só com o que já
 * está no aparelho (datas, pastas e favoritos), sem reconhecer nada nas fotos. [source] é a lista que abre ao tocar.
 */
sealed interface Story {
    val id: String
    val cover: MediaItem
    val count: Int
    val source: MediaSource

    /** As fotos tiradas neste mesmo dia (mês e dia) há [yearsAgo] anos. */
    data class OnThisDay(
        val yearsAgo: Int,
        override val cover: MediaItem,
        override val count: Int,
        override val source: MediaSource.Day,
    ) : Story {
        override val id = "day_${source.epochDay}"
    }

    /** Álbum rápido: uma das pastas mexidas há menos tempo. */
    data class QuickAlbum(
        val name: String,
        override val cover: MediaItem,
        override val count: Int,
        override val source: MediaSource.Album,
    ) : Story {
        override val id = "album_${source.bucketId}"
    }

    /** As favoritas, com a mais nova de capa. */
    data class Favorites(
        override val cover: MediaItem,
        override val count: Int,
    ) : Story {
        override val id = "favorites"
        override val source: MediaSource = MediaSource.Favorites
    }
}

/**
 * Monta as lembranças: primeiro "neste dia" (até [maxOnThisDay] anos, do mais recente ao mais antigo, só os
 * que têm fotos), depois os [maxQuickAlbums] álbuns rápidos (pastas mexidas há menos tempo, sem as capturas de
 * tela) e, por fim, as favoritas. [items] vem das mais novas para as mais antigas. A capa prefere foto a vídeo.
 */
fun buildStories(
    items: List<MediaItem>,
    today: LocalDate,
    zone: ZoneId = ZoneId.systemDefault(),
    maxOnThisDay: Int = 3,
    maxQuickAlbums: Int = 5,
): List<Story> {
    if (items.isEmpty()) return emptyList()
    val stories = mutableListOf<Story>()

    val byDay = items.groupBy { it.epochDay(zone) }
    var found = 0
    var yearsAgo = 1
    while (found < maxOnThisDay && yearsAgo <= MAX_YEARS_BACK) {
        val date = today.minusYears(yearsAgo.toLong())
        // Em 29 de fevereiro, anos sem o dia 29 caem no 28: não é o mesmo dia, então esse ano não conta.
        if (date.dayOfMonth == today.dayOfMonth) {
            val epoch = date.toEpochDay()
            byDay[epoch]?.let { group ->
                stories += Story.OnThisDay(yearsAgo, group.coverItem(), group.size, MediaSource.Day(epoch))
                found++
            }
        }
        yearsAgo++
    }

    buildAlbums(items)
        .filter { album -> !album.cover.isScreenshot() }
        .sortedByDescending { it.cover.takenMs }
        .take(maxQuickAlbums)
        .forEach { album ->
            stories += Story.QuickAlbum(album.name, album.cover, album.count, MediaSource.Album(album.bucketId))
        }

    val favorites = items.filter { it.isFavorite }
    if (favorites.isNotEmpty()) stories += Story.Favorites(favorites.coverItem(), favorites.size)

    return stories
}

private const val MAX_YEARS_BACK = 20

private fun List<MediaItem>.coverItem(): MediaItem = firstOrNull { !it.isVideo } ?: first()
