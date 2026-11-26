// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.core.model

/** Ferramentas que a aba Álbuns mostra no topo ("Fixadas"). A pessoa escolhe quais e em que ordem. */
enum class AlbumTool { Favorites, Videos, Screenshots, Trash, Vault, Cleanup }

/** Padrão: todas, nesta ordem. */
val DEFAULT_ALBUM_TOOLS: List<AlbumTool> = AlbumTool.entries.toList()

/** Lê o que foi guardado ("Vault,Trash"). Sem nada guardado (null) vale o padrão; texto vazio = nenhuma fixada. */
fun parseAlbumTools(raw: String?): List<AlbumTool> {
    if (raw == null) return DEFAULT_ALBUM_TOOLS
    return raw.split(',')
        .mapNotNull { name -> AlbumTool.entries.firstOrNull { it.name == name } }
        .distinct()
}

fun encodeAlbumTools(tools: List<AlbumTool>): String = tools.joinToString(",") { it.name }

/** Move [tool] [delta] posições (−1 sobe, +1 desce); nas pontas, não muda nada. */
fun moveAlbumTool(tools: List<AlbumTool>, tool: AlbumTool, delta: Int): List<AlbumTool> {
    val from = tools.indexOf(tool)
    val to = from + delta
    if (from < 0 || to !in tools.indices) return tools
    return tools.toMutableList().apply {
        removeAt(from)
        add(to, tool)
    }
}
