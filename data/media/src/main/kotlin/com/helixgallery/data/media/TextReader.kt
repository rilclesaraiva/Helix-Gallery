// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Lê o texto que aparece numa imagem, no próprio aparelho (nada sai dele). */
interface TextReader {
    /** O texto reconhecido em [uri] (vazio se não há texto) ou o motivo de não ter dado para ler. Nunca lança. */
    suspend fun read(uri: String): TextReadResult
}

sealed interface TextReadResult {
    /** [flowed]: um parágrafo por bloco, com as linhas do bloco unidas; [original]: uma linha por linha da imagem. */
    data class Text(val flowed: String, val original: String) : TextReadResult

    /** [detail] é o tipo e a mensagem do erro, para diagnóstico. */
    data class Failure(val detail: String) : TextReadResult
}

/** Une as linhas de cada bloco (parágrafo) numa só, separadas por espaço, e separa os blocos por quebra de linha. */
internal fun flowText(blocks: List<List<String>>): String =
    blocks.map { lines -> lines.map { it.trim() }.filter { it.isNotEmpty() }.joinToString(" ") }
        .filter { it.isNotEmpty() }
        .joinToString("\n")

private fun Throwable.detail(): String = "${this::class.java.simpleName}: ${message.orEmpty()}".take(300)

object TextReaderData {
    fun reader(context: Context): TextReader = MlKitTextReader(context.applicationContext)
}

/**
 * Fator de redução ao decodificar: a maior potência de 2 que ainda deixa o lado maior com pelo menos [maxSide]
 * pixels (imagens grandes gastariam memória à toa; as menores ficam como estão).
 */
internal fun sampleSizeFor(width: Int, height: Int, maxSide: Int = 2048): Int {
    val longest = maxOf(width, height)
    var sample = 1
    while (longest / (sample * 2) >= maxSide) sample *= 2
    return sample
}

/** Uma leitura de uma variante da imagem: o texto e o quanto o reconhecedor confiou nele ([mass] = soma de confiança × letras). */
internal data class OcrPass(val flowed: String, val original: String, val mass: Float, val chars: Int) {
    val averageConfidence: Float get() = if (chars == 0) 0f else mass / chars
}

/** Fundo escuro: a segunda passada (cores invertidas) tem mais chance de ajudar. */
internal const val DARK_LUMINANCE = 0.4f

/** Abaixo disto a confiança média da leitura original é considerada baixa. */
internal const val LOW_CONFIDENCE = 0.85f

/** Só gasta a passada invertida quando a original veio vazia, pouco confiante ou de imagem escura. */
internal fun shouldTryInverted(first: OcrPass, luminance: Float): Boolean =
    first.chars == 0 || first.averageConfidence < LOW_CONFIDENCE || luminance < DARK_LUMINANCE

/** Fica com a leitura de maior "massa de confiança"; em empate vale a original. */
internal fun pickBetter(original: OcrPass, inverted: OcrPass): OcrPass =
    if (inverted.mass > original.mass) inverted else original

/** Luminância média (0 a 1) de [bitmap], medida numa miniatura. */
private fun averageLuminance(bitmap: Bitmap): Float {
    val small = Bitmap.createScaledBitmap(bitmap, 32, 32, true)
    val pixels = IntArray(32 * 32)
    small.getPixels(pixels, 0, 32, 0, 0, 32, 32)
    if (small !== bitmap) small.recycle()
    val sum = pixels.sumOf { (0.299 * Color.red(it) + 0.587 * Color.green(it) + 0.114 * Color.blue(it)) }
    return (sum / pixels.size / 255.0).toFloat()
}

/** Cópia com as cores invertidas (texto claro em fundo escuro vira escuro em fundo claro). */
private fun invert(bitmap: Bitmap): Bitmap {
    val out = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    val matrix = ColorMatrix(
        floatArrayOf(
            -1f, 0f, 0f, 0f, 255f,
            0f, -1f, 0f, 0f, 255f,
            0f, 0f, -1f, 0f, 255f,
            0f, 0f, 0f, 1f, 0f,
        ),
    )
    Canvas(out).drawBitmap(bitmap, 0f, 0f, Paint().apply { colorFilter = ColorMatrixColorFilter(matrix) })
    return out
}

private suspend fun TextRecognizer.recognize(bitmap: Bitmap): OcrPass = suspendCancellableCoroutine { cont ->
    process(InputImage.fromBitmap(bitmap, 0))
        .addOnSuccessListener { result ->
            val lines = result.textBlocks.flatMap { it.lines }
            val chars = lines.sumOf { it.text.length }
            val mass = lines.sumOf { (it.confidence * it.text.length).toDouble() }.toFloat()
            cont.resume(OcrPass(flowText(result.textBlocks.map { block -> block.lines.map { it.text } }), result.text, mass, chars))
        }
        .addOnFailureListener { error -> cont.resumeWithException(error) }
}

private class MlKitTextReader(private val context: Context) : TextReader {
    override suspend fun read(uri: String): TextReadResult = withContext(Dispatchers.Default) {
        // Qualquer erro (inclusive de código nativo ou de classe ausente) vira falha mostrada ao usuário, nunca crash.
        try {
            val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, Uri.parse(uri))) { decoder, info, _ ->
                // Por software: o ML Kit não lê bitmaps de hardware.
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.setTargetSampleSize(sampleSizeFor(info.size.width, info.size.height))
            }
            // Modelo latino (português, espanhol, inglês…), embutido no app: funciona sem rede.
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            try {
                val first = recognizer.recognize(bitmap)
                var best = first
                if (shouldTryInverted(first, averageLuminance(bitmap))) {
                    // Uma variante por vez: o bitmap invertido é liberado antes de qualquer outra coisa.
                    val inverted = invert(bitmap)
                    try {
                        best = pickBetter(first, recognizer.recognize(inverted))
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        // A segunda passada é só um reforço: se falhar, vale a leitura original.
                    } finally {
                        inverted.recycle()
                    }
                }
                TextReadResult.Text(best.flowed, best.original)
            } finally {
                recognizer.close()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            TextReadResult.Failure(t.detail())
        }
    }
}
