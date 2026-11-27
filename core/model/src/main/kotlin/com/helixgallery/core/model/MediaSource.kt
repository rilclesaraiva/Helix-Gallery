// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.core.model

/**
 * De onde vem uma lista de fotos: tudo, um álbum, favoritos, vídeos, capturas
 * de tela ou a lixeira. A [key] vai na rota de navegação (o visualizador abre
 * a mesma lista da tela de onde veio).
 */
sealed interface MediaSource {
    val key: String

    data object All : MediaSource {
        override val key = "all"
    }

    data class Album(val bucketId: Long) : MediaSource {
        override val key = "album_$bucketId"
    }

    data object Favorites : MediaSource {
        override val key = "favorites"
    }

    data object Videos : MediaSource {
        override val key = "videos"
    }

    data object Screenshots : MediaSource {
        override val key = "screenshots"
    }

    /** Um dia só (contado desde 1970): as fotos tiradas nele, como as "neste dia" das lembranças. */
    data class Day(val epochDay: Long) : MediaSource {
        override val key = "day_$epochDay"
    }

    data object Trash : MediaSource {
        override val key = "trash"
    }

    companion object {
        /** Volta da chave da rota para a origem; chave desconhecida cai em [All]. */
        fun parse(key: String?): MediaSource = when {
            key == null -> All
            key == Favorites.key -> Favorites
            key == Videos.key -> Videos
            key == Screenshots.key -> Screenshots
            key == Trash.key -> Trash
            key.startsWith("day_") -> key.removePrefix("day_").toLongOrNull()?.let(::Day) ?: All
            key.startsWith("album_") -> key.removePrefix("album_").toLongOrNull()?.let(::Album) ?: All
            else -> All
        }
    }
}

/** Capturas de tela ficam na pasta "Screenshots" (ou "Capturas de tela" em alguns aparelhos). */
fun MediaItem.isScreenshot(): Boolean {
    val place = "$relativePath $bucketName".lowercase()
    return "screenshot" in place || "captura" in place
}

/** A lista que [source] mostra, na ordem em que as fotos já vêm (mais novas primeiro). */
fun selectMedia(source: MediaSource, state: MediaState): List<MediaItem> = when (source) {
    MediaSource.All -> state.items
    is MediaSource.Album -> state.items.filter { it.bucketId == source.bucketId }
    MediaSource.Favorites -> state.items.filter { it.isFavorite }
    MediaSource.Videos -> state.items.filter { it.isVideo }
    MediaSource.Screenshots -> state.items.filter { it.isScreenshot() }
    is MediaSource.Day -> state.items.filter { it.epochDay() == source.epochDay }
    MediaSource.Trash -> state.trashed
}
