// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.core.model

import java.time.LocalDate
import java.time.ZoneOffset

/** Foto de mentira para os testes. */
fun fakeMedia(
    id: Long,
    date: LocalDate = LocalDate.of(2026, 11, 20),
    name: String = "IMG_$id.jpg",
    bucketId: Long = 1,
    bucketName: String = "Camera",
    relativePath: String = "DCIM/Camera/",
    isVideo: Boolean = false,
    isFavorite: Boolean = false,
    mimeType: String = if (isVideo) "video/mp4" else "image/jpeg",
): MediaItem {
    val ms = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() + id
    return MediaItem(
        id = id,
        uri = "content://media/external/images/media/$id",
        isVideo = isVideo,
        displayName = name,
        mimeType = mimeType,
        takenMs = ms,
        addedMs = ms,
        width = 1000,
        height = 1000,
        durationMs = if (isVideo) 5_000 else 0,
        sizeBytes = 1_000,
        bucketId = bucketId,
        bucketName = bucketName,
        relativePath = relativePath,
        isFavorite = isFavorite,
    )
}
