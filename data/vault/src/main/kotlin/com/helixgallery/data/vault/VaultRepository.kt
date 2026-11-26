// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.data.vault

import android.content.Context
import java.io.File

/** Uma foto ou vídeo a guardar no cofre. [folder] é a pasta de origem ("Pictures/Trip/"), ou vazio. */
data class VaultSource(val uri: String, val displayName: String, val folder: String, val takenMs: Long)

/** O que aconteceu ao guardar: [stored] são os `uri` que entraram (e estão gravados); só esses podem ser apagados do aparelho. */
data class VaultAddResult(val stored: List<String>, val failed: Int)

/**
 * O cofre: um ZIP com AES-256 em `Documents/HelixGallery/`. Aberto, o conteúdo fica numa cópia de
 * trabalho no cache do app; ao trancar, essa cópia e tudo que foi extraído para ver são apagados.
 */
interface VaultRepository {
    /** Já existe um cofre criado? */
    fun exists(): Boolean

    /** Cria o cofre com [password] e já o deixa aberto. */
    suspend fun create(password: CharArray)

    /** Abre com [password]; false se a senha estiver errada. */
    suspend fun unlock(password: CharArray): Boolean

    fun isUnlocked(): Boolean

    suspend fun entries(): List<VaultEntry>

    /** Guarda os arquivos e grava o cofre. Nada sai do aparelho: só entra no ZIP. */
    suspend fun add(sources: List<VaultSource>): VaultAddResult

    /** O arquivo já descriptografado de [name], numa pasta temporária (para mostrar). */
    suspend fun extractForViewing(name: String): File

    /** Devolve [names] à galeria e os tira do cofre. Retorna quantos voltaram. */
    suspend fun restore(names: List<String>): Int

    /** Apaga [names] do cofre, de vez. */
    suspend fun delete(names: List<String>)

    /** Tranca: apaga a senha da memória, a cópia de trabalho e o que foi extraído. */
    fun lock()

    /** Este aparelho tem tela de bloqueio/biometria para o desbloqueio rápido? */
    fun quickUnlockSupported(): Boolean

    fun quickUnlockEnabled(): Boolean

    /** Liga o desbloqueio rápido guardando a senha do cofre aberto. Exige identificação recente. */
    fun enableQuickUnlock(): Boolean

    fun disableQuickUnlock()

    /** A senha do desbloqueio rápido (exige identificação recente), ou null. */
    fun quickUnlockPassword(): CharArray?
}

object VaultData {
    @Volatile
    private var instance: VaultRepository? = null

    fun repository(context: Context): VaultRepository =
        instance ?: synchronized(this) {
            instance ?: ZipVaultRepository(context.applicationContext).also { instance = it }
        }
}
