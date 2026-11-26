// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.data.media

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Grava imagens novas na galeria (o editor salva uma cópia, nunca altera o original). */
interface MediaWriter {
    /** Salva [bitmap] como JPEG. Devolve false se não deu. A cópia fica na mesma pasta do original (se for DCIM ou Pictures). */
    suspend fun saveJpeg(bitmap: Bitmap, displayName: String, relativePath: String, takenMs: Long): Boolean

    /** Copia o arquivo [sourceUri] para a pasta [relativePath] (o original continua onde está). Devolve false se não deu. */
    suspend fun copyToFolder(
        sourceUri: String,
        displayName: String,
        mimeType: String,
        isVideo: Boolean,
        relativePath: String,
        takenMs: Long,
    ): Boolean
}

object MediaWriterData {
    fun writer(context: Context): MediaWriter = MediaStoreWriter(context.applicationContext)
}

/** "IMG_1.png" → "IMG_1_edit.jpg": o nome da cópia editada. */
fun editedFileName(displayName: String): String {
    val dot = displayName.lastIndexOf('.')
    val base = if (dot > 0) displayName.substring(0, dot) else displayName
    return base + "_edit.jpg"
}

private class MediaStoreWriter(private val context: Context) : MediaWriter {
    override suspend fun saveJpeg(bitmap: Bitmap, displayName: String, relativePath: String, takenMs: Long): Boolean =
        withContext(Dispatchers.IO) {
            val folder = restoreLocation(relativePath.trimEnd('/') + "/" + displayName, false)?.second
                ?: "Pictures/HelixGallery/"
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, editedFileName(displayName))
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, folder)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
                if (takenMs > 0) put(MediaStore.Images.Media.DATE_TAKEN, takenMs)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values)
                ?: return@withContext false
            try {
                val ok = resolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) } ?: false
                if (!ok) error("compress failed")
                resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
                true
            } catch (_: Exception) {
                resolver.delete(uri, null, null)
                false
            }
        }

    override suspend fun copyToFolder(
        sourceUri: String,
        displayName: String,
        mimeType: String,
        isVideo: Boolean,
        relativePath: String,
        takenMs: Long,
    ): Boolean = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val collection = if (isVideo) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
            if (takenMs > 0) put(MediaStore.MediaColumns.DATE_TAKEN, takenMs)
        }
        val target = try {
            resolver.insert(collection, values)
        } catch (_: Exception) {
            null
        } ?: return@withContext false
        try {
            val input = resolver.openInputStream(android.net.Uri.parse(sourceUri)) ?: error("no input")
            val output = resolver.openOutputStream(target) ?: error("no output")
            input.use { i -> output.use { o -> i.copyTo(o) } }
            resolver.update(target, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
            true
        } catch (_: Exception) {
            resolver.delete(target, null, null)
            false
        }
    }
}
