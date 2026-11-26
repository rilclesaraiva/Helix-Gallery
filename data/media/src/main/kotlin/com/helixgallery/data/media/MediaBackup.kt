// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.data.media

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.helixgallery.core.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Resultado de importar um backup: [imported] entraram, [skipped] já existiam ou não eram foto/vídeo. */
data class BackupImportResult(val imported: Int, val skipped: Int)

/**
 * Backup local em ZIP (sem criptografia; para guardar em pasta, cartão ou nuvem que a pessoa escolher
 * pelo seletor de arquivos do Android). Nada é enviado pelo app.
 */
interface MediaBackup {
    /** Grava [items] em [output] como ZIP, mantendo as pastas. [onProgress] recebe (feitos, total). */
    suspend fun export(items: List<MediaItem>, output: OutputStream, onProgress: (done: Int, total: Int) -> Unit): Int

    /** Devolve à galeria o que está no ZIP de [input]. Quem já existe (mesmo nome e pasta) é pulado. */
    suspend fun import(input: InputStream, onProgress: (done: Int) -> Unit): BackupImportResult
}

object MediaBackupData {
    fun backup(context: Context): MediaBackup = ZipMediaBackup(context.applicationContext)
}

/** Caminho do arquivo dentro do ZIP: "Pictures/Trip/a.jpg" (repetidos ganham " (1)"). */
internal fun backupEntryName(item: MediaItem, used: MutableSet<String>): String {
    val wanted = item.relativePath.trimStart('/') + item.displayName
    var name = wanted
    var n = 1
    while (!used.add(name)) {
        val dot = wanted.lastIndexOf('.').takeIf { it > wanted.lastIndexOf('/') + 1 } ?: wanted.length
        name = wanted.substring(0, dot) + " ($n)" + wanted.substring(dot)
        n++
    }
    return name
}

/** Tipo pela extensão: "image/…", "video/…" ou null (outros arquivos do ZIP são ignorados). */
internal fun mediaMimeType(fileName: String): String? = when (fileName.substringAfterLast('.', "").lowercase()) {
    "jpg", "jpeg" -> "image/jpeg"
    "png" -> "image/png"
    "gif" -> "image/gif"
    "webp" -> "image/webp"
    "heic" -> "image/heic"
    "heif" -> "image/heif"
    "avif" -> "image/avif"
    "bmp" -> "image/bmp"
    "mp4", "m4v" -> "video/mp4"
    "mkv" -> "video/x-matroska"
    "webm" -> "video/webm"
    "3gp" -> "video/3gpp"
    "mov" -> "video/quicktime"
    else -> null
}

/**
 * Onde devolver [entryName] (nome, pasta). Pasta fora de DCIM/Pictures (fotos) ou DCIM/Movies (vídeos)
 * cai em Pictures/HelixGallery/ ou Movies/HelixGallery/. Caminho com ".." é recusado (null).
 */
internal fun restoreLocation(entryName: String, isVideo: Boolean): Pair<String, String>? {
    val parts = entryName.replace('\\', '/').split('/').filter { it.isNotEmpty() }
    if (parts.isEmpty() || parts.any { it == ".." || it == "." }) return null
    val name = parts.last()
    val folder = parts.dropLast(1).joinToString("/").let { if (it.isEmpty()) "" else "$it/" }
    val allowed = if (isVideo) listOf("DCIM/", "Movies/") else listOf("DCIM/", "Pictures/")
    val target = folder.takeIf { f -> allowed.any { f.startsWith(it) } }
        ?: if (isVideo) "Movies/HelixGallery/" else "Pictures/HelixGallery/"
    return name to target
}

private class ZipMediaBackup(private val context: Context) : MediaBackup {
    private val resolver = context.contentResolver

    override suspend fun export(
        items: List<MediaItem>,
        output: OutputStream,
        onProgress: (done: Int, total: Int) -> Unit,
    ): Int = withContext(Dispatchers.IO) {
        val used = mutableSetOf<String>()
        var written = 0
        ZipOutputStream(output.buffered()).use { zip ->
            // Foto e vídeo já vêm comprimidos: o mínimo de compressão, para ser rápido.
            zip.setLevel(Deflater.BEST_SPEED)
            items.forEachIndexed { index, item ->
                try {
                    resolver.openInputStream(Uri.parse(item.uri))?.use { input ->
                        zip.putNextEntry(ZipEntry(backupEntryName(item, used)).apply { time = item.takenMs })
                        input.copyTo(zip)
                        zip.closeEntry()
                        written++
                    }
                } catch (_: Exception) {
                    // Um arquivo ilegível não derruba o backup inteiro.
                }
                onProgress(index + 1, items.size)
            }
        }
        written
    }

    override suspend fun import(input: InputStream, onProgress: (done: Int) -> Unit): BackupImportResult =
        withContext(Dispatchers.IO) {
            var imported = 0
            var skipped = 0
            ZipInputStream(input.buffered()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (entry.isDirectory) continue
                    val mime = mediaMimeType(entry.name)
                    val video = mime?.startsWith("video/") == true
                    val location = if (mime == null) null else restoreLocation(entry.name, video)
                    if (mime == null || location == null) {
                        skipped++
                        continue
                    }
                    val (name, folder) = location
                    if (exists(name, folder, video)) {
                        skipped++
                        continue
                    }
                    try {
                        insert(zip, name, folder, mime, video, entry.time)
                        imported++
                    } catch (_: Exception) {
                        skipped++
                    }
                    onProgress(imported)
                }
            }
            BackupImportResult(imported, skipped)
        }

    private fun collection(video: Boolean): Uri =
        if (video) MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        else MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

    private fun exists(name: String, folder: String, video: Boolean): Boolean =
        resolver.query(
            collection(video),
            arrayOf(MediaStore.MediaColumns._ID),
            "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} = ?",
            arrayOf(name, folder),
            null,
        )?.use { it.moveToFirst() } ?: false

    private fun insert(source: InputStream, name: String, folder: String, mime: String, video: Boolean, timeMs: Long) {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, folder)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
            if (timeMs > 0) {
                put(MediaStore.MediaColumns.DATE_MODIFIED, timeMs / 1000)
                put(if (video) MediaStore.Video.Media.DATE_TAKEN else MediaStore.Images.Media.DATE_TAKEN, timeMs)
            }
        }
        val uri = resolver.insert(collection(video), values) ?: error("Cannot insert")
        try {
            resolver.openOutputStream(uri)?.use { out -> source.copyTo(out) } ?: error("Cannot write")
            resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
    }
}
