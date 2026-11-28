// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Maior lado, em pixels, da imagem usada no seletor de cor (mais detalhe que isso pesa na memória sem ajudar). */
private const val PICKER_MAX_SIDE = 4096

/** Carrega a imagem [uri] para o seletor de cor (por software, para dar para ler os pixels). Null se não abrir. */
suspend fun loadPickerBitmap(context: Context, uri: String): Bitmap? = withContext(Dispatchers.Default) {
    try {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, Uri.parse(uri))) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.setTargetSampleSize(sampleSizeFor(info.size.width, info.size.height, PICKER_MAX_SIDE))
        }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Throwable) {
        null
    }
}

/**
 * A cor ao redor do ponto ([x], [y]) de [bitmap]: média dos pixels de um quadrado de lado `2 * radius + 1`
 * (ruído e granulação da foto não mudam o resultado). Devolve RGB (sem transparência), como 0xRRGGBB.
 */
fun sampleColor(bitmap: Bitmap, x: Int, y: Int, radius: Int = 2): Int {
    val left = (x - radius).coerceIn(0, bitmap.width - 1)
    val top = (y - radius).coerceIn(0, bitmap.height - 1)
    val right = (x + radius).coerceIn(left, bitmap.width - 1)
    val bottom = (y + radius).coerceIn(top, bitmap.height - 1)
    val w = right - left + 1
    val h = bottom - top + 1
    val pixels = IntArray(w * h)
    bitmap.getPixels(pixels, 0, w, left, top, w, h)
    return averageRgb(pixels)
}

/** Média, canal a canal, dos pixels ARGB em [pixels] (o alfa é ignorado). Vazio = preto. */
fun averageRgb(pixels: IntArray): Int {
    if (pixels.isEmpty()) return 0
    var r = 0L
    var g = 0L
    var b = 0L
    for (p in pixels) {
        r += (p shr 16) and 0xFF
        g += (p shr 8) and 0xFF
        b += p and 0xFF
    }
    val n = pixels.size
    return ((r / n).toInt() shl 16) or ((g / n).toInt() shl 8) or (b / n).toInt()
}

/** O RGB 0xRRGGBB como texto, em maiúsculas: `#716E6F`. */
fun hexOf(rgb: Int): String = "#%06X".format(rgb and 0xFFFFFF)
