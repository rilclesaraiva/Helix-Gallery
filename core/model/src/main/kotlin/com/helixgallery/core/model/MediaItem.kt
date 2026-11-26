// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.core.model

/**
 * Uma foto ou um vídeo do aparelho, como o Android (MediaStore) o descreve.
 * Classe pura: nenhum import de android.* (ver CLAUDE.md).
 */
data class MediaItem(
    val id: Long,
    /** Endereço `content://…` do arquivo (para abrir, compartilhar e pedir ao sistema). */
    val uri: String,
    val isVideo: Boolean,
    val displayName: String,
    val mimeType: String,
    /** Quando foi tirada; sem essa data, a data do arquivo. É por ela que a galeria ordena. */
    val takenMs: Long,
    val addedMs: Long,
    val width: Int,
    val height: Int,
    val durationMs: Long,
    val sizeBytes: Long,
    val bucketId: Long,
    val bucketName: String,
    /** Pasta dentro do armazenamento, ex.: "Pictures/Screenshots/". */
    val relativePath: String,
    val isFavorite: Boolean,
)

/** O que a galeria sabe do aparelho agora: o que aparece e o que está na lixeira do sistema. */
data class MediaState(
    val items: List<MediaItem> = emptyList(),
    val trashed: List<MediaItem> = emptyList(),
    /** Falso até a primeira leitura terminar. */
    val loaded: Boolean = false,
)
