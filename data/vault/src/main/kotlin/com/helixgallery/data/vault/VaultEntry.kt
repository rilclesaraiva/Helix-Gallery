// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.data.vault

/** Um arquivo guardado no cofre. [name] é o caminho dentro do ZIP: "pasta/arquivo.jpg". */
data class VaultEntry(
    val name: String,
    val sizeBytes: Long,
    /** Quando foi tirada/modificada (a data guardada junto ao arquivo). */
    val takenMs: Long,
) {
    val displayName: String get() = name.substringAfterLast('/')

    /** Pasta de origem, com a barra no fim ("Pictures/Screenshots/"), ou vazio. */
    val folder: String get() = if ('/' in name) name.substringBeforeLast('/') + "/" else ""

    val isVideo: Boolean get() = mimeTypeFor(displayName).startsWith("video/")
}

/** Tipo do arquivo pela extensão (o ZIP não guarda o tipo). */
fun mimeTypeFor(fileName: String): String = when (fileName.substringAfterLast('.', "").lowercase()) {
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
    else -> "application/octet-stream"
}

/**
 * Nome que ainda não existe no cofre: se [wanted] já existe, vira "nome (1).ext", "nome (2).ext"…
 * (dois arquivos com o mesmo nome em pastas iguais não se sobrescrevem).
 */
fun uniqueEntryName(existing: Set<String>, wanted: String): String {
    if (wanted !in existing) return wanted
    val dot = wanted.lastIndexOf('.').takeIf { it > wanted.lastIndexOf('/') + 1 } ?: wanted.length
    val base = wanted.substring(0, dot)
    val ext = wanted.substring(dot)
    var n = 1
    while ("$base ($n)$ext" in existing) n++
    return "$base ($n)$ext"
}
