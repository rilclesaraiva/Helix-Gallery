// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.core.model

/** Vídeos a partir deste tamanho entram na lista de "vídeos grandes" da limpeza (100 MB). */
const val LARGE_VIDEO_BYTES: Long = 100L * 1024 * 1024

/** Capturas de tela, das mais novas para as mais antigas. */
fun screenshotsOf(items: List<MediaItem>): List<MediaItem> =
    items.filter { it.isScreenshot() }.sortedByDescending { it.takenMs }

/** Vídeos grandes, dos maiores para os menores. */
fun largeVideosOf(items: List<MediaItem>, thresholdBytes: Long = LARGE_VIDEO_BYTES): List<MediaItem> =
    items.filter { it.isVideo && it.sizeBytes >= thresholdBytes }.sortedByDescending { it.sizeBytes }

/**
 * Candidatas a duplicadas: arquivos com o mesmo tamanho e as mesmas dimensões. Só isso não prova
 * nada; quem confirma é o conteúdo ([exactDuplicateGroups]). Cada grupo vem com a mais antiga primeiro.
 */
fun duplicateCandidates(items: List<MediaItem>): List<List<MediaItem>> =
    items.filter { it.sizeBytes > 0 }
        .groupBy { Triple(it.sizeBytes, it.width, it.height) }
        .values
        .filter { it.size > 1 }
        .map { group -> group.sortedBy { it.addedMs } }

/**
 * Dos [candidates], os que de fato têm o mesmo conteúdo ([hashOf] devolve uma assinatura do
 * conteúdo; null = não deu para ler, fica de fora). A primeira de cada grupo (a mais antiga) é a
 * que se mantém; as outras são as "sobras".
 */
fun exactDuplicateGroups(
    candidates: List<List<MediaItem>>,
    hashOf: (MediaItem) -> String?,
): List<List<MediaItem>> =
    candidates.flatMap { group ->
        group.mapNotNull { item -> hashOf(item)?.let { it to item } }
            .groupBy({ it.first }, { it.second })
            .values
            .filter { it.size > 1 }
    }

/** Total de bytes de [items]. */
fun totalBytes(items: Collection<MediaItem>): Long = items.sumOf { it.sizeBytes }
