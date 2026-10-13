package com.armo.poster.domain

import com.armo.poster.domain.model.DerivedValues
import com.armo.poster.domain.model.PlayerBlock
import org.junit.Assert.assertEquals
import org.junit.Test

class DerivedValuesTest {

    @Test
    fun calculateBarWidths_withUnequalXg_scalesProportionally() {
        val (homePct, awayPct) = DerivedValues.calculateBarWidths(homeXg = 1.10, awayXg = 1.38)
        assertEquals(79.7, homePct, 0.1)
        assertEquals(100.0, awayPct, 0.0)
    }

    @Test
    fun calculateBarWidths_withEqualXg_returnsBothHundredPercent() {
        val (homePct, awayPct) = DerivedValues.calculateBarWidths(homeXg = 2.0, awayXg = 2.0)
        assertEquals(100.0, homePct, 0.0)
        assertEquals(100.0, awayPct, 0.0)
    }

    @Test
    fun calculateBarWidths_withZeroXg_handlesGracefully() {
        val (homePct, awayPct) = DerivedValues.calculateBarWidths(homeXg = 0.0, awayXg = 1.50)
        assertEquals(0.0, homePct, 0.0)
        assertEquals(100.0, awayPct, 0.0)
    }

    @Test
    fun pairPlayerBlocks_asymmetricLists_padsShorterList() {
        val home = listOf(
            PlayerBlock(name = "Rogers", notes = listOf("Note 1")),
            PlayerBlock(name = "Palmer", notes = listOf("Note 2")),
            PlayerBlock(name = "Henderson", notes = listOf("Note 3")),
            PlayerBlock(name = "Pedro", notes = listOf("Note 4"))
        )
        val away = listOf(
            PlayerBlock(name = "Tavernier", notes = listOf("Note A")),
            PlayerBlock(name = "Rayan", notes = listOf("Note B")),
            PlayerBlock(name = "Evanilson", notes = listOf("Note C"))
        )

        val paired = DerivedValues.pairPlayerBlocks(home, away)
        assertEquals(4, paired.size)
        assertEquals("Rogers", paired[0].left?.name)
        assertEquals("Tavernier", paired[0].right?.name)
        assertEquals("Pedro", paired[3].left?.name)
        assertEquals(null, paired[3].right)
    }

    @Test
    fun formatHeadline_capsAbbreviationAt3Chars() {
        val headline = DerivedValues.formatHeadline(homeAbbr = "CHELSEA", awayAbbr = "BOURNEMOUTH")
        assertEquals("CHE vs BOU", headline)
    }

    @Test
    fun formatPanelLabel_formatsCorrectly() {
        val label = DerivedValues.formatPanelLabel("Chelsea", "3-4-2-1")
        assertEquals("Chelsea (3-4-2-1)", label)

        val labelWithoutFormation = DerivedValues.formatPanelLabel("Chelsea", "")
        assertEquals("Chelsea", labelWithoutFormation)
    }
}
