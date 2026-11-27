// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.data.media

/** Os quatro cantos de uma [TextRegion] que dá para arrastar. */
enum class RegionCorner { TopLeft, TopRight, BottomLeft, BottomRight }

/**
 * Área retangular de uma imagem, em frações de 0 a 1 do tamanho dela (não em pixels, assim vale para qualquer
 * resolução e para a foto como aparece na tela). Usada para ler o texto de uma parte só da imagem.
 */
data class TextRegion(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top

    /** A imagem inteira (ou quase): ler essa área é o mesmo que ler tudo. */
    val isFull: Boolean get() = left <= 0.001f && top <= 0.001f && right >= 0.999f && bottom >= 0.999f

    /** Move a área por ([dx], [dy]) sem deixá-la sair da imagem (o tamanho não muda). */
    fun movedBy(dx: Float, dy: Float): TextRegion {
        val x = dx.coerceIn(-left, 1f - right)
        val y = dy.coerceIn(-top, 1f - bottom)
        return TextRegion(left + x, top + y, right + x, bottom + y)
    }

    /** Leva o canto [corner] para ([x], [y]), dentro da imagem e sem passar do tamanho mínimo [minSize]. */
    fun withCorner(corner: RegionCorner, x: Float, y: Float, minSize: Float = MIN_SIZE): TextRegion {
        val px = x.coerceIn(0f, 1f)
        val py = y.coerceIn(0f, 1f)
        val isLeft = corner == RegionCorner.TopLeft || corner == RegionCorner.BottomLeft
        val isTop = corner == RegionCorner.TopLeft || corner == RegionCorner.TopRight
        return TextRegion(
            left = if (isLeft) px.coerceAtMost(right - minSize) else left,
            top = if (isTop) py.coerceAtMost(bottom - minSize) else top,
            right = if (!isLeft) px.coerceAtLeast(left + minSize) else right,
            bottom = if (!isTop) py.coerceAtLeast(top + minSize) else bottom,
        )
    }

    /** Aumenta ou diminui a área em volta do centro (fator > 1 aumenta), dentro da imagem e do tamanho mínimo. */
    fun scaledAroundCenter(factor: Float, minSize: Float = MIN_SIZE): TextRegion {
        val cx = (left + right) / 2f
        val cy = (top + bottom) / 2f
        val w = (width * factor).coerceIn(minSize, 1f)
        val h = (height * factor).coerceIn(minSize, 1f)
        val l = (cx - w / 2f).coerceIn(0f, 1f - w)
        val t = (cy - h / 2f).coerceIn(0f, 1f - h)
        return TextRegion(l, t, l + w, t + h)
    }

    companion object {
        /** Menor lado permitido (fração da imagem): evita uma área minúscula demais para conter texto. */
        const val MIN_SIZE = 0.06f
        val Full = TextRegion(0f, 0f, 1f, 1f)
    }
}

/** A área [region] em pixels de uma imagem [width]×[height]: x, y, largura e altura (pelo menos 1 pixel, dentro da imagem). */
internal fun regionPixels(width: Int, height: Int, region: TextRegion): IntArray {
    val x = (region.left * width).toInt().coerceIn(0, width - 1)
    val y = (region.top * height).toInt().coerceIn(0, height - 1)
    val w = (region.width * width).toInt().coerceIn(1, width - x)
    val h = (region.height * height).toInt().coerceIn(1, height - y)
    return intArrayOf(x, y, w, h)
}
