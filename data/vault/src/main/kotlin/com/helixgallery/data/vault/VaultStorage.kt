// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.data.vault

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import java.io.File

/**
 * Onde o cofre mora: um arquivo na pasta pública `Documents/HelixGallery/` (pela MediaStore, sem
 * pedir permissão). Ele fica fora da pasta do app de propósito: sobrevive a desinstalar o app e
 * abre em qualquer programa de ZIP com a senha.
 */
internal class VaultStorage(context: Context) {
    private val resolver = context.contentResolver
    private val collection: Uri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

    /** O arquivo do cofre que este app criou, se existir. */
    fun find(): Uri? {
        resolver.query(
            collection,
            arrayOf(MediaStore.Files.FileColumns._ID),
            "${MediaStore.Files.FileColumns.DISPLAY_NAME} = ? AND ${MediaStore.Files.FileColumns.RELATIVE_PATH} = ?",
            arrayOf(FILE_NAME, FOLDER),
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                return android.content.ContentUris.withAppendedId(collection, cursor.getLong(0))
            }
        }
        return null
    }

    /** Copia o cofre guardado para [target] (a cópia de trabalho). */
    fun copyTo(uri: Uri, target: File) {
        target.parentFile?.mkdirs()
        resolver.openInputStream(uri)?.use { input -> target.outputStream().use { input.copyTo(it) } }
            ?: error("Cannot open vault")
    }

    /** Grava a cópia de trabalho de volta no cofre guardado (cria o arquivo na primeira vez). */
    fun writeBack(source: File): Uri {
        val uri = find() ?: resolver.insert(
            collection,
            ContentValues().apply {
                put(MediaStore.Files.FileColumns.DISPLAY_NAME, FILE_NAME)
                put(MediaStore.Files.FileColumns.MIME_TYPE, "application/zip")
                put(MediaStore.Files.FileColumns.RELATIVE_PATH, FOLDER)
            },
        ) ?: error("Cannot create vault")
        resolver.openOutputStream(uri, "wt")?.use { out -> source.inputStream().use { it.copyTo(out) } }
            ?: error("Cannot write vault")
        return uri
    }

    companion object {
        const val FILE_NAME = "HelixVault.zip"
        const val FOLDER = "Documents/HelixGallery/"
    }
}
