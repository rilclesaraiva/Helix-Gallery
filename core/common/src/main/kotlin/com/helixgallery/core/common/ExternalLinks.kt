// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.core.common

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/** Abre um link no app que cuidar dele (navegador, GitHub…). */
fun Context.openUrl(url: String) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        // Sem navegador instalado: não há o que fazer.
    }
}

/**
 * Compartilha fotos e vídeos pelo seletor do Android. O tipo vira o de imagem, o de vídeo
 * ou o genérico, conforme o que foi escolhido; o app que recebe ganha leitura temporária.
 */
fun Context.shareMedia(uris: List<String>, mimeTypes: List<String>, title: String) {
    if (uris.isEmpty()) return
    val parsed = ArrayList(uris.map { Uri.parse(it) })
    val kinds = mimeTypes.map { it.substringBefore('/') }.toSet()
    val type = if (kinds.size == 1) "${kinds.first()}/*" else "*/*"
    val send = if (parsed.size == 1) {
        Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, parsed.first())
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM, parsed)
    }
    send.setType(type).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    try {
        startActivity(Intent.createChooser(send, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        // Sem nenhum app para compartilhar.
    }
}
