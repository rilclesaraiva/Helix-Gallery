// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.core.model

/**
 * Em que item de uma grade começa cada linha dela. A grade tem [headerCount] itens antes
 * das [entries] (ex.: o título da tela, cada um numa linha só); depois, cada título de dia
 * ou mês ocupa uma linha inteira e as fotos enchem linhas de [columns] colunas.
 *
 * Serve à barra de rolagem rápida: a altura de uma grade é a soma das alturas das linhas
 * (não dos itens), e títulos e linhas de fotos têm alturas bem diferentes.
 */
fun gridLineStarts(entries: List<GridEntry>, columns: Int, headerCount: Int = 0): IntArray {
    val cols = columns.coerceAtLeast(1)
    val starts = ArrayList<Int>(entries.size / cols + headerCount + 8)
    for (i in 0 until headerCount) starts.add(i)
    var used = 0
    entries.forEachIndexed { i, entry ->
        val index = headerCount + i
        when (entry) {
            is GridEntry.Header -> {
                starts.add(index)
                used = 0
            }
            is GridEntry.Media -> {
                if (used == 0) starts.add(index)
                used = (used + 1) % cols
            }
        }
    }
    return starts.toIntArray()
}
