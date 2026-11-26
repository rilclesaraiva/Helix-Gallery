// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.features.editor

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditMathTest {
    private val identity = floatArrayOf(
        1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f,
    )

    @Test fun defaultParams_giveIdentityMatrix() {
        assertArrayEquals(identity, colorMatrix(EditParams()), 1e-4f)
        assertTrue(EditParams().isUnchanged)
    }

    @Test fun brightness_addsToChannels() {
        val m = colorMatrix(EditParams(brightness = 0.5f))
        assertEquals(127.5f, m[4], 1e-3f)
        assertEquals(127.5f, m[9], 1e-3f)
        assertEquals(127.5f, m[14], 1e-3f)
        assertEquals(0f, m[19], 1e-6f)
    }

    @Test fun monoFilter_makesChannelsEqual() {
        val m = colorMatrix(EditParams(filter = EditFilter.Mono))
        for (j in 0 until 3) {
            assertEquals(m[j], m[5 + j], 1e-4f)
            assertEquals(m[j], m[10 + j], 1e-4f)
        }
    }

    @Test fun thenApply_withIdentityKeepsMatrix() {
        val m = filterMatrix(EditFilter.Sepia)
        assertArrayEquals(m, thenApply(m, identity), 1e-5f)
        assertArrayEquals(m, thenApply(identity, m), 1e-5f)
    }

    @Test fun cropRect_square_isCenteredOnWideImage() {
        val r = cropRect(400, 200, 1f, 1f)
        assertEquals(CropRect(100, 0, 200, 200), r)
    }

    @Test fun cropRect_original_withZoom() {
        val r = cropRect(400, 200, null, 2f)
        assertEquals(CropRect(100, 50, 200, 100), r)
    }

    @Test fun cropRect_tallTarget_limitsWidth() {
        val r = cropRect(300, 400, 16f / 9f, 1f)
        assertEquals(300, r.width)
        assertEquals(169, r.height)
        assertFalse(r.top < 0)
    }

    @Test fun cropRect_zoomBelowOne_isIgnored() {
        assertEquals(CropRect(0, 0, 100, 50), cropRect(100, 50, null, 0.5f))
    }
}
