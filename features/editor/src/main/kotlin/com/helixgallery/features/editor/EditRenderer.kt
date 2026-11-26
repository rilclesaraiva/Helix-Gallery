// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.features.editor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect

/**
 * Aplica as edições: gira e espelha, corta ao centro e passa a matriz de cor. A prévia e o arquivo
 * salvo usam este mesmo código (o que se vê é o que se salva). [maxSide] limita o lado maior do resultado.
 */
internal fun renderEdit(source: Bitmap, params: EditParams, maxSide: Int? = null): Bitmap {
    val matrix = Matrix().apply {
        postRotate(90f * (params.rotationSteps.mod(4)))
        if (params.flipHorizontal) postScale(-1f, 1f)
    }
    val rotated = if (matrix.isIdentity) {
        source
    } else {
        Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }
    val crop = cropRect(rotated.width, rotated.height, params.aspect.ratio, params.zoom)
    var outW = crop.width
    var outH = crop.height
    if (maxSide != null && maxOf(outW, outH) > maxSide) {
        val scale = maxSide.toFloat() / maxOf(outW, outH)
        outW = (outW * scale).toInt().coerceAtLeast(1)
        outH = (outH * scale).toInt().coerceAtLeast(1)
    }
    val out = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
    val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
        colorFilter = ColorMatrixColorFilter(ColorMatrix(colorMatrix(params)))
    }
    Canvas(out).drawBitmap(
        rotated,
        Rect(crop.left, crop.top, crop.left + crop.width, crop.top + crop.height),
        Rect(0, 0, outW, outH),
        paint,
    )
    return out
}
