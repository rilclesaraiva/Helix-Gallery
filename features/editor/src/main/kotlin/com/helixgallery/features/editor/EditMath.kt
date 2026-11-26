// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.features.editor

import kotlin.math.max
import kotlin.math.roundToInt

internal enum class EditFilter { None, Mono, Sepia, Vivid, Cool, Warm, Fade }

/** Proporção do corte; [ratio] = largura / altura (null = a da foto). */
internal enum class CropAspect(val ratio: Float?) {
    Original(null), Square(1f), FourThree(4f / 3f), ThreeFour(3f / 4f), SixteenNine(16f / 9f)
}

internal data class EditParams(
    /** Quartos de volta no sentido horário (0 a 3). */
    val rotationSteps: Int = 0,
    val flipHorizontal: Boolean = false,
    val aspect: CropAspect = CropAspect.Original,
    /** 1 = sem aproximar. */
    val zoom: Float = 1f,
    /** -1 a 1. */
    val brightness: Float = 0f,
    /** 0,5 a 1,5 (1 = igual). */
    val contrast: Float = 1f,
    /** 0 a 2 (1 = igual). */
    val saturation: Float = 1f,
    val filter: EditFilter = EditFilter.None,
) {
    val isUnchanged: Boolean get() = this == EditParams()
}

internal data class CropRect(val left: Int, val top: Int, val width: Int, val height: Int)

/** O retângulo cortado ao centro: o maior com a proporção pedida, aproximado por [zoom]. */
internal fun cropRect(width: Int, height: Int, aspect: Float?, zoom: Float): CropRect {
    val baseW: Int
    val baseH: Int
    if (aspect == null) {
        baseW = width
        baseH = height
    } else if (width.toFloat() / height > aspect) {
        baseH = height
        baseW = (height * aspect).roundToInt().coerceIn(1, width)
    } else {
        baseW = width
        baseH = (width / aspect).roundToInt().coerceIn(1, height)
    }
    val z = zoom.coerceAtLeast(1f)
    val w = max(1, (baseW / z).roundToInt())
    val h = max(1, (baseH / z).roundToInt())
    return CropRect((width - w) / 2, (height - h) / 2, w, h)
}

private val IDENTITY = floatArrayOf(
    1f, 0f, 0f, 0f, 0f,
    0f, 1f, 0f, 0f, 0f,
    0f, 0f, 1f, 0f, 0f,
    0f, 0f, 0f, 1f, 0f,
)

/** A matriz que aplica [a] e depois [b] (formato 4x5 do Android: linhas R, G, B, A; a quinta coluna soma). */
internal fun thenApply(a: FloatArray, b: FloatArray): FloatArray {
    val out = FloatArray(20)
    for (i in 0 until 4) {
        for (j in 0 until 5) {
            var sum = 0f
            for (k in 0 until 4) sum += b[i * 5 + k] * a[k * 5 + j]
            if (j == 4) sum += b[i * 5 + 4]
            out[i * 5 + j] = sum
        }
    }
    return out
}

internal fun saturationMatrix(s: Float): FloatArray = floatArrayOf(
    0.213f + 0.787f * s, 0.715f - 0.715f * s, 0.072f - 0.072f * s, 0f, 0f,
    0.213f - 0.213f * s, 0.715f + 0.285f * s, 0.072f - 0.072f * s, 0f, 0f,
    0.213f - 0.213f * s, 0.715f - 0.715f * s, 0.072f + 0.928f * s, 0f, 0f,
    0f, 0f, 0f, 1f, 0f,
)

private fun contrastMatrix(c: Float): FloatArray {
    val t = 128f * (1f - c)
    return floatArrayOf(
        c, 0f, 0f, 0f, t,
        0f, c, 0f, 0f, t,
        0f, 0f, c, 0f, t,
        0f, 0f, 0f, 1f, 0f,
    )
}

private fun brightnessMatrix(b: Float): FloatArray {
    val t = b * 255f
    return floatArrayOf(
        1f, 0f, 0f, 0f, t,
        0f, 1f, 0f, 0f, t,
        0f, 0f, 1f, 0f, t,
        0f, 0f, 0f, 1f, 0f,
    )
}

private fun scaleMatrix(r: Float, g: Float, b: Float): FloatArray = floatArrayOf(
    r, 0f, 0f, 0f, 0f,
    0f, g, 0f, 0f, 0f,
    0f, 0f, b, 0f, 0f,
    0f, 0f, 0f, 1f, 0f,
)

internal fun filterMatrix(filter: EditFilter): FloatArray = when (filter) {
    EditFilter.None -> IDENTITY
    EditFilter.Mono -> saturationMatrix(0f)
    EditFilter.Sepia -> floatArrayOf(
        0.393f, 0.769f, 0.189f, 0f, 0f,
        0.349f, 0.686f, 0.168f, 0f, 0f,
        0.272f, 0.534f, 0.131f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f,
    )
    EditFilter.Vivid -> thenApply(saturationMatrix(1.4f), contrastMatrix(1.1f))
    EditFilter.Cool -> scaleMatrix(0.92f, 1f, 1.1f)
    EditFilter.Warm -> scaleMatrix(1.1f, 1f, 0.9f)
    EditFilter.Fade -> thenApply(thenApply(saturationMatrix(0.8f), contrastMatrix(0.85f)), brightnessMatrix(0.05f))
}

/** O filtro e depois saturação, contraste e brilho, numa matriz só. */
internal fun colorMatrix(p: EditParams): FloatArray {
    var m = filterMatrix(p.filter)
    m = thenApply(m, saturationMatrix(p.saturation))
    m = thenApply(m, contrastMatrix(p.contrast))
    m = thenApply(m, brightnessMatrix(p.brightness))
    return m
}
