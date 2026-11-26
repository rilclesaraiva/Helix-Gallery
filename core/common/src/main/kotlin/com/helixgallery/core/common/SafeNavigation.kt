// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.core.common

import androidx.navigation.NavController
import androidx.navigation.NavGraph
import androidx.navigation.NavOptionsBuilder

/**
 * Só o que é de fato um toque duplo é ignorado: dois pedidos de navegação com menos de
 * [DOUBLE_TAP_MS] entre si. Mesma regra do BoomListener.
 */
private const val DOUBLE_TAP_MS = 180L

private fun nowMs(): Long = System.nanoTime() / 1_000_000L

private val lastNavigation = java.util.WeakHashMap<NavController, Long>()

private fun NavController.isDoubleTap(): Boolean {
    val last = lastNavigation[this] ?: return false
    return nowMs() - last < DOUBLE_TAP_MS
}

/** A última tela aberta por [navigateOnce] em cada NavController (o app tem um só). */
private val lastOpened = java.util.WeakHashMap<NavController, String>()

/**
 * Abre [route] uma vez só: um toque duplo (menos de 180 ms) na tela que acabou de
 * abrir é ignorado (abriria a mesma tela por cima). Toda navegação do app passa
 * por aqui (nunca `navigate` direto, fora a troca de abas).
 */
fun NavController.navigateOnce(route: String, builder: NavOptionsBuilder.() -> Unit = {}) {
    val current = currentBackStackEntry
    if (current != null && current.id == lastOpened[this] && isDoubleTap()) return
    navigate(route, builder)
    lastNavigation[this] = nowMs()
    currentBackStackEntry?.let { lastOpened[this] = it.id }
}

/**
 * "Voltar" de uma tela de dentro, uma vez só (dois toques não fecham duas telas).
 * Recusa quando resta uma tela só: voltar nela esvaziaria a pilha (tela vazia),
 * o que o BoomListener provou acontecer quando a tela que está saindo pede voltar de novo.
 */
fun NavController.popBackStackOnce(): Boolean {
    if (isDoubleTap()) return false
    if (currentBackStack.value.count { it.destination !is NavGraph } <= 1) return false
    lastNavigation[this] = nowMs()
    return popBackStack()
}
