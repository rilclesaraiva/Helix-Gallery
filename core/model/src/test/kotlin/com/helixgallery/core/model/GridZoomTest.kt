// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.core.model

import org.junit.Test
import org.junit.Assert.assertEquals

class GridZoomTest {
    @Test
    fun zoomOutWalksFromDayToMonth() {
        var zoom = GridZoom(GridLayout.Day, 3)
        zoom = nextGridZoom(zoom, zoomOut = true)
        assertEquals(GridZoom(GridLayout.Day, 4), zoom)
        zoom = nextGridZoom(zoom, zoomOut = true)
        assertEquals(GridZoom(GridLayout.Day, 5), zoom)
        zoom = nextGridZoom(zoom, zoomOut = true)
        assertEquals(GridZoom(GridLayout.Month, 5), zoom)
    }

    @Test
    fun endsStayPut() {
        assertEquals(GridZoom(GridLayout.Month, 5), nextGridZoom(GridZoom(GridLayout.Month, 5), zoomOut = true))
        assertEquals(GridZoom(GridLayout.Day, 3), nextGridZoom(GridZoom(GridLayout.Day, 3), zoomOut = false))
    }

    @Test
    fun zoomInFromMonthGoesBackToDay() {
        assertEquals(GridZoom(GridLayout.Day, 5), nextGridZoom(GridZoom(GridLayout.Month, 5), zoomOut = false))
    }

    @Test
    fun regularOnlyChangesColumns() {
        assertEquals(GridZoom(GridLayout.Regular, 5), nextGridZoom(GridZoom(GridLayout.Regular, 4), zoomOut = true))
        assertEquals(GridZoom(GridLayout.Regular, 3), nextGridZoom(GridZoom(GridLayout.Regular, 3), zoomOut = false))
    }

    @Test
    fun monthWithOtherColumnsCountsAsLastStep() {
        assertEquals(GridZoom(GridLayout.Day, 5), nextGridZoom(GridZoom(GridLayout.Month, 3), zoomOut = false))
    }
}
