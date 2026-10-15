package com.armo.poster.renderer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FitLadderTest {

    @Test
    fun isFit_whenContentHIsSmallerOrEqual_returnsTrue() {
        val metrics = FitMetrics(innerH = 1112, contentH = 1050)
        assertTrue(FitLadder.isFit(metrics))

        val equalMetrics = FitMetrics(innerH = 1112, contentH = 1112)
        assertTrue(FitLadder.isFit(equalMetrics))
    }

    @Test
    fun isFit_whenContentHIsLarger_returnsFalse() {
        val metrics = FitMetrics(innerH = 1112, contentH = 1150)
        assertFalse(FitLadder.isFit(metrics))
        assertEquals(38, metrics.overflowPx)
    }

    @Test
    fun maxLevel_isTen() {
        assertEquals(10, FitLadder.MAX_LEVEL)
    }
}
