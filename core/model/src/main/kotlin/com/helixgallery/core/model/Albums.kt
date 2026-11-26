// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.core.model

/** Um álbum é uma pasta do aparelho (o "bucket" do Android). A capa é a foto mais nova dele. */
data class AlbumSummary(
    val bucketId: Long,
    val name: String,
    val count: Int,
    val cover: MediaItem,
)

/**
 * Agrupa por pasta. A pasta da câmera vem primeiro; as outras, da mais
 * recentemente mexida para a mais antiga. [items] já vem das mais novas para as mais antigas.
 */
fun buildAlbums(items: List<MediaItem>): List<AlbumSummary> =
    items.groupBy { it.bucketId }
        .map { (bucketId, group) ->
            AlbumSummary(
                bucketId = bucketId,
                name = group.first().bucketName.ifBlank { "?" },
                count = group.size,
                cover = group.first(),
            )
        }
        .sortedWith(
            compareByDescending<AlbumSummary> { it.name.equals("Camera", ignoreCase = true) }
                .thenByDescending { it.cover.takenMs },
        )
