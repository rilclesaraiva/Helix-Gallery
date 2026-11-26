// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.core.model

/** Como a grade está organizada: o layout (por dia, por mês…) e quantas colunas. */
data class GridZoom(val layout: GridLayout, val columns: Int)

/**
 * Os passos da pinça, do mais perto (fotos grandes) ao mais longe: por dia com 3, 4 e 5 colunas
 * e, no fim, por mês. No layout Regular a pinça só muda as colunas.
 */
private val DAY_STEPS: List<GridZoom> = listOf(
    GridZoom(GridLayout.Day, 3),
    GridZoom(GridLayout.Day, 4),
    GridZoom(GridLayout.Day, 5),
    GridZoom(GridLayout.Month, 5),
)

/**
 * Próximo passo da pinça a partir de [current]: [zoomOut] true afasta (fotos menores, mais
 * por tela), false aproxima. No primeiro/último passo fica onde está.
 */
fun nextGridZoom(current: GridZoom, zoomOut: Boolean): GridZoom {
    if (current.layout == GridLayout.Regular) {
        val options = GRID_COLUMN_OPTIONS
        val at = options.indexOfFirst { it >= current.columns }.let { if (it < 0) options.lastIndex else it }
        val next = (if (zoomOut) at + 1 else at - 1).coerceIn(0, options.lastIndex)
        return GridZoom(GridLayout.Regular, options[next])
    }
    val at = DAY_STEPS.indexOfFirst { it == current }.takeIf { it >= 0 }
        ?: if (current.layout == GridLayout.Month) DAY_STEPS.lastIndex
        else DAY_STEPS.indexOfFirst { it.layout == GridLayout.Day && it.columns >= current.columns }
            .let { if (it < 0) 2 else it }
    return DAY_STEPS[(if (zoomOut) at + 1 else at - 1).coerceIn(0, DAY_STEPS.lastIndex)]
}
