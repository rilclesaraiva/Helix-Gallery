// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.features.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoEditTest {
    @Test
    fun untouchedVideoIsNotChanged() {
        assertFalse(VideoEdit(0, 14_000).isChanged(14_000))
    }

    @Test
    fun trimMuteOrRotationCountAsChanges() {
        assertTrue(VideoEdit(1_000, 14_000).isChanged(14_000))
        assertTrue(VideoEdit(0, 10_000).isChanged(14_000))
        assertTrue(VideoEdit(0, 14_000, mute = true).isChanged(14_000))
        assertTrue(VideoEdit(0, 14_000, rotationSteps = 1).isChanged(14_000))
        assertFalse(VideoEdit(0, 14_000, rotationSteps = 4).isChanged(14_000))
    }

    @Test
    fun namesAndFolders() {
        assertEquals("clip_edit.mp4", editedVideoName("clip.mov"))
        assertEquals("Movies/Trips/", videoFolder("Movies/Trips/"))
        assertEquals("Movies/HelixGallery/", videoFolder("Download/"))
    }
}
