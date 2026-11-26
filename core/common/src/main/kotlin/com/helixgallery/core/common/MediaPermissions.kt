// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.core.common

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * O que o Android exige para ler fotos e vídeos: no 13+, duas permissões de
 * mídia; no 12, a de armazenamento. Quem usa o app dá acesso a tudo (o acesso
 * parcial do Android 14 não é usado: a galeria precisa ver todas as fotos).
 */
fun requiredMediaPermissions(sdkInt: Int = Build.VERSION.SDK_INT): List<String> =
    if (sdkInt >= 33) {
        listOf("android.permission.READ_MEDIA_IMAGES", "android.permission.READ_MEDIA_VIDEO")
    } else {
        listOf("android.permission.READ_EXTERNAL_STORAGE")
    }

fun hasMediaAccess(context: Context): Boolean =
    requiredMediaPermissions().all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
