package com.armo.poster.domain.model

import kotlin.math.max
import kotlin.math.round

object DerivedValues {

    /**
     * Calculates proportional bar widths for the xG comparison bars.
     * The larger xG gets 100%; the other gets (xg / max * 100) rounded to 1 decimal place.
     */
    fun calculateBarWidths(homeXg: Double, awayXg: Double): Pair<Double, Double> {
        if (homeXg <= 0.0 && awayXg <= 0.0) {
            return Pair(100.0, 100.0)
        }
        val maxXg = max(homeXg, awayXg)
        val homePct = if (maxXg > 0.0) round((homeXg / maxXg) * 1000.0) / 10.0 else 0.0
        val awayPct = if (maxXg > 0.0) round((awayXg / maxXg) * 1000.0) / 10.0 else 0.0
        return Pair(homePct, awayPct)
    }

    /**
     * Pairs home and away player blocks into rows.
     * The shorter list is padded with null entries so rows stay aligned.
     */
    fun pairPlayerBlocks(
        homeBlocks: List<PlayerBlock>,
        awayBlocks: List<PlayerBlock>
    ): List<PairedBlock> {
        val rowCount = max(homeBlocks.size, awayBlocks.size)
        return (0 until rowCount).map { i ->
            PairedBlock(
                left = homeBlocks.getOrNull(i),
                right = awayBlocks.getOrNull(i)
            )
        }
    }

    /**
     * Formats headline abbreviation string, e.g., "CHE vs BOU".
     * Abbreviations are capped at 3 uppercase characters.
     */
    fun formatHeadline(homeAbbr: String, awayAbbr: String): String {
        val h = homeAbbr.trim().take(3).uppercase()
        val a = awayAbbr.trim().take(3).uppercase()
        return "$h vs $a"
    }

    /**
     * Formats panel label: "Chelsea (3-4-2-1)" or just "Chelsea" if formation is empty.
     */
    fun formatPanelLabel(name: String, formation: String): String {
        val trimmedFormation = formation.trim()
        return if (trimmedFormation.isNotEmpty()) {
            "$name ($trimmedFormation)"
        } else {
            name
        }
    }
}
