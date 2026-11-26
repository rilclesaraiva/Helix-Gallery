// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.features.vault

import org.junit.Assert.assertEquals
import org.junit.Test

class PasswordRulesTest {
    @Test fun shortPassword_isRejected() {
        assertEquals(PasswordCheck.TooShort, checkNewPassword("1234567", "1234567"))
    }

    @Test fun differentConfirmation_isRejected() {
        assertEquals(PasswordCheck.Mismatch, checkNewPassword("12345678", "12345679"))
    }

    @Test fun matchingLongPassword_isOk() {
        assertEquals(PasswordCheck.Ok, checkNewPassword("12345678", "12345678"))
    }

    @Test fun shortIsReportedBeforeMismatch() {
        assertEquals(PasswordCheck.TooShort, checkNewPassword("abc", "xyz"))
    }
}
