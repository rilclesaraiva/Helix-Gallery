// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class CleanupTest {
    private fun m(id: Long, size: Long, w: Int = 1000, video: Boolean = false, path: String = "DCIM/Camera/") =
        fakeMedia(id, isVideo = video, relativePath = path).copy(sizeBytes = size, width = w)

    @Test fun largeVideos_filtersAndSortsBySizeDesc() {
        val items = listOf(m(1, 200L * 1024 * 1024, video = true), m(2, 500L * 1024 * 1024, video = true), m(3, 10, video = true), m(4, 900L * 1024 * 1024))
        assertEquals(listOf(2L, 1L), largeVideosOf(items).map { it.id })
    }

    @Test fun screenshots_areOnlyFromScreenshotFolders() {
        val items = listOf(m(1, 10, path = "Pictures/Screenshots/"), m(2, 10))
        assertEquals(listOf(1L), screenshotsOf(items).map { it.id })
    }

    @Test fun candidates_groupBySizeAndDimensions() {
        val items = listOf(m(1, 50), m(2, 50), m(3, 50, w = 999), m(4, 70))
        val groups = duplicateCandidates(items)
        assertEquals(1, groups.size)
        assertEquals(listOf(1L, 2L), groups.single().map { it.id })
    }

    @Test fun exactGroups_needSameHash() {
        val groups = exactDuplicateGroups(listOf(listOf(m(1, 50), m(2, 50), m(3, 50)))) { if (it.id == 3L) "other" else "same" }
        assertEquals(listOf(listOf(1L, 2L)), groups.map { g -> g.map { it.id } })
    }

    @Test fun exactGroups_skipUnreadable() {
        val groups = exactDuplicateGroups(listOf(listOf(m(1, 50), m(2, 50)))) { null }
        assertEquals(emptyList<List<MediaItem>>(), groups)
    }

    @Test fun totalBytes_sums() {
        assertEquals(30L, totalBytes(listOf(m(1, 10), m(2, 20))))
    }
}
