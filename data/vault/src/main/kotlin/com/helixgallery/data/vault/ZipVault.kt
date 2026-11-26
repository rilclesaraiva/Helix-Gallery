// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.data.vault

import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.CompressionLevel
import net.lingala.zip4j.model.enums.EncryptionMethod
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream

/**
 * O cofre como um arquivo ZIP comum com criptografia AES-256 (abre em qualquer programa de ZIP
 * com a senha). Só mexe num [file]; onde ele mora é com quem chama. O ZIP não pode ficar vazio,
 * então o cofre nasce com um item de marca ([MARKER]) que também serve para conferir a senha.
 */
internal class ZipVault(private val file: File, password: CharArray) {
    private val password = password.copyOf()

    private fun zip() = ZipFile(file, password)

    private fun parameters(name: String, modifiedMs: Long) = ZipParameters().apply {
        setEncryptFiles(true)
        setEncryptionMethod(EncryptionMethod.AES)
        setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_256)
        // Foto e vídeo já vêm comprimidos: o mínimo de compressão, para ser rápido.
        setCompressionLevel(CompressionLevel.FASTEST)
        setFileNameInZip(name)
        setLastModifiedFileTime(modifiedMs)
    }

    /** Cria o arquivo do cofre (apaga um que já exista). */
    fun create() {
        file.parentFile?.mkdirs()
        file.delete()
        MARKER_TEXT.toByteArray().inputStream().use { zip().addStream(it, parameters(MARKER, System.currentTimeMillis())) }
    }

    /** A senha abre o cofre? (Lê e confere o item de marca.) */
    fun verify(): Boolean = try {
        val z = zip()
        val header = z.getFileHeader(MARKER)
        header != null && z.getInputStream(header).use { it.readBytes() }.contentEquals(MARKER_TEXT.toByteArray())
    } catch (_: Exception) {
        false
    }

    fun list(): List<VaultEntry> =
        zip().fileHeaders
            .filter { !it.isDirectory && it.fileName != MARKER }
            .map { VaultEntry(name = it.fileName, sizeBytes = it.uncompressedSize, takenMs = it.lastModifiedTimeEpoch) }

    fun add(name: String, input: InputStream, modifiedMs: Long) {
        input.use { zip().addStream(it, parameters(name, modifiedMs)) }
    }

    /** Abre o conteúdo (já descriptografado) de [name]; quem chama fecha. */
    fun open(name: String): InputStream {
        val z = zip()
        val header = z.getFileHeader(name) ?: throw FileNotFoundException(name)
        return z.getInputStream(header)
    }

    fun remove(name: String) {
        zip().removeFile(name)
    }

    companion object {
        const val MARKER = ".helixvault"
        private const val MARKER_TEXT = "Helix Gallery vault"
    }
}
