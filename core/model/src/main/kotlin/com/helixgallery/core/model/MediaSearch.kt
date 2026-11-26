// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.core.model

import java.text.Normalizer
import java.time.Instant
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

private val ACCENTS = Regex("\\p{InCombiningDiacriticalMarks}+")

/** Minúsculas e sem acento: "Vídeo" casa com "video". */
fun normalizeSearchText(text: String): String =
    ACCENTS.replace(Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD), "")

/**
 * Procura por nome do arquivo, álbum, tipo (foto, vídeo, gif), ano e mês. Todas as
 * palavras digitadas precisam casar (sem acento nem maiúscula). Busca vazia não acha nada.
 */
fun searchMedia(
    items: List<MediaItem>,
    query: String,
    locale: Locale = Locale.getDefault(),
    zone: ZoneId = ZoneId.systemDefault(),
): List<MediaItem> {
    val words = normalizeSearchText(query).split(' ', '\t').filter { it.isNotBlank() }
    if (words.isEmpty()) return emptyList()
    return items.filter { item ->
        val date = Instant.ofEpochMilli(item.takenMs).atZone(zone).toLocalDate()
        val kind = when {
            item.isVideo -> "video vídeo movie"
            item.mimeType.endsWith("gif") -> "gif foto photo picture imagem"
            else -> "foto photo picture imagem"
        }
        val haystack = normalizeSearchText(
            listOf(
                item.displayName,
                item.bucketName,
                kind,
                date.year.toString(),
                date.month.getDisplayName(TextStyle.FULL, locale),
                date.month.getDisplayName(TextStyle.SHORT, locale),
                if (item.isFavorite) "favorito favorite" else "",
            ).joinToString(" "),
        )
        words.all { it in haystack }
    }
}
