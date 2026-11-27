// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.data.media

import android.content.Context
import android.graphics.ImageDecoder
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/** Lê o texto que aparece numa imagem, no próprio aparelho (nada sai dele). */
interface TextReader {
    /** O texto reconhecido em [uri] (vazio se não há texto) ou o motivo de não ter dado para ler. Nunca lança. */
    suspend fun read(uri: String): TextReadResult
}

sealed interface TextReadResult {
    data class Text(val text: String) : TextReadResult

    /** [detail] é o tipo e a mensagem do erro, para diagnóstico. */
    data class Failure(val detail: String) : TextReadResult
}

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
                suspendCancellableCoroutine<TextReadResult> { cont ->
                    recognizer.process(InputImage.fromBitmap(bitmap, 0))
                        .addOnSuccessListener { result -> cont.resume(TextReadResult.Text(result.text)) }
                        .addOnFailureListener { error -> cont.resume(TextReadResult.Failure(error.detail())) }
                }
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
