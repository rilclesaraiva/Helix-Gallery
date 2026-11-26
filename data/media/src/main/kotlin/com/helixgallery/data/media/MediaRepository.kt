// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.data.media

import android.content.Context
import android.content.IntentSender
import com.helixgallery.core.model.MediaState
import kotlinx.coroutines.flow.Flow

/**
 * Fonte das fotos e vídeos do aparelho (a MediaStore do Android). Favoritos e lixeira
 * são os do sistema: valem também em outros apps de galeria.
 */
interface MediaRepository {
    /** A lista atual; se atualiza sozinha quando algo muda no aparelho. */
    val state: Flow<MediaState>

    /** Pede ao Android para mover para a lixeira (ou restaurar); a pessoa confirma numa janela do sistema. */
    fun trashRequest(uris: List<String>, trash: Boolean): IntentSender

    /** Pede ao Android para apagar de vez; a pessoa confirma numa janela do sistema. */
    fun deleteRequest(uris: List<String>): IntentSender

    /** Pede ao Android para marcar ou desmarcar como favorita. */
    fun favoriteRequest(uris: List<String>, favorite: Boolean): IntentSender

    /** Se a pessoa liberou o acesso especial "Gerenciar mídia" (apaga sem perguntar toda vez). */
    fun canManageMedia(): Boolean
}

/** Ponto único de acesso ao repositório (um por processo). */
object MediaData {
    @Volatile
    private var instance: MediaRepository? = null

    fun repository(context: Context): MediaRepository =
        instance ?: synchronized(this) {
            instance ?: MediaStoreRepository(context.applicationContext).also { instance = it }
        }
}
