// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.features.vault

internal const val MIN_PASSWORD_LENGTH = 8

internal enum class PasswordCheck { Ok, TooShort, Mismatch }

/** A senha nova serve? Pelo menos [MIN_PASSWORD_LENGTH] caracteres e igual na confirmação. */
internal fun checkNewPassword(password: String, confirmation: String): PasswordCheck = when {
    password.length < MIN_PASSWORD_LENGTH -> PasswordCheck.TooShort
    password != confirmation -> PasswordCheck.Mismatch
    else -> PasswordCheck.Ok
}
