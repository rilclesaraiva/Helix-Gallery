// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.data.media

import com.helixgallery.core.model.MediaItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MediaBackupTest {
    private fun item(name: String, path: String) = MediaItem(
        id = 1, uri = "content://x/1", isVideo = false, displayName = name, mimeType = "image/jpeg",
        takenMs = 0, addedMs = 0, width = 1, height = 1, durationMs = 0, sizeBytes = 1,
        bucketId = 1, bucketName = "b", relativePath = path, isFavorite = false,
    )

    @Test fun entryName_keepsFolder() {
        assertEquals("DCIM/Camera/a.jpg", backupEntryName(item("a.jpg", "DCIM/Camera/"), mutableSetOf()))
    }

    @Test fun entryName_numbersRepeats() {
        val used = mutableSetOf<String>()
        assertEquals("P/a.jpg", backupEntryName(item("a.jpg", "P/"), used))
        assertEquals("P/a (1).jpg", backupEntryName(item("a.jpg", "P/"), used))
        assertEquals("P/a (2).jpg", backupEntryName(item("a.jpg", "P/"), used))
    }

    @Test fun mime_onlyPhotosAndVideos() {
        assertEquals("image/jpeg", mediaMimeType("X.JPG"))
        assertEquals("video/mp4", mediaMimeType("v.mp4"))
        assertNull(mediaMimeType("notes.txt"))
    }

    @Test fun restoreLocation_keepsAllowedFolders() {
        assertEquals("a.jpg" to "Pictures/Trip/", restoreLocation("Pictures/Trip/a.jpg", false))
        assertEquals("v.mp4" to "Movies/Trip/", restoreLocation("Movies/Trip/v.mp4", true))
    }

    @Test fun restoreLocation_fallsBackForOtherFolders() {
        assertEquals("a.jpg" to "Pictures/HelixGallery/", restoreLocation("Download/a.jpg", false))
        assertEquals("v.mp4" to "Movies/HelixGallery/", restoreLocation("Pictures/v.mp4", true))
        assertEquals("a.jpg" to "Pictures/HelixGallery/", restoreLocation("a.jpg", false))
    }

    @Test fun restoreLocation_refusesDotDot() {
        assertNull(restoreLocation("Pictures/../../a.jpg", false))
    }
}

class EditedNameTest {
    @Test fun editedName_swapsExtension() {
        assertEquals("IMG_1_edit.jpg", editedFileName("IMG_1.png"))
        assertEquals("a.b_edit.jpg", editedFileName("a.b.heic"))
        assertEquals("noext_edit.jpg", editedFileName("noext"))
    }
}
