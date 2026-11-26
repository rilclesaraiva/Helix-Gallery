// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.features.settings

import com.helixgallery.data.updates.UpdatePromptState
import org.junit.Assert.assertEquals
import org.junit.Test

class UpdatePromptModeTest {
    private val now = 1_000_000_000L

    @Test fun nothingSet_isAlways() {
        assertEquals(PromptMode.Always, promptMode(UpdatePromptState(), now))
    }

    @Test fun snoozeUpToAWeek_isWeek() {
        assertEquals(PromptMode.Week, promptMode(UpdatePromptState(snoozedUntilMs = now + WEEK_MS), now))
    }

    @Test fun longerSnooze_isMonth() {
        assertEquals(PromptMode.Month, promptMode(UpdatePromptState(snoozedUntilMs = now + MONTH_MS), now))
    }

    @Test fun expiredSnooze_isAlways() {
        assertEquals(PromptMode.Always, promptMode(UpdatePromptState(snoozedUntilMs = now - 1), now))
    }

    @Test fun skippedVersion_isSkipped() {
        assertEquals(PromptMode.Skipped, promptMode(UpdatePromptState(skippedVersion = "v1.0.0"), now))
    }
}
