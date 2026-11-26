// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.features.settings

import com.helixgallery.data.updates.UpdatePromptState

/** Como ficam os avisos automáticos de versão nova (Ajustes → Sistema → Atualizações). */
internal enum class PromptMode { Always, Week, Month, Skipped }

internal const val WEEK_MS = 7L * 24 * 60 * 60 * 1000
internal const val MONTH_MS = 30L * 24 * 60 * 60 * 1000

/**
 * O estado guardado não lembra o período escolhido: restando até uma semana é "uma semana",
 * mais que isso é "um mês". Pular uma versão só vale se não houver adiamento ativo.
 */
internal fun promptMode(state: UpdatePromptState, nowMs: Long): PromptMode {
    val remaining = state.snoozedUntilMs - nowMs
    return when {
        remaining > WEEK_MS -> PromptMode.Month
        remaining > 0 -> PromptMode.Week
        state.skippedVersion != null -> PromptMode.Skipped
        else -> PromptMode.Always
    }
}
