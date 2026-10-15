package com.armo.poster.domain.model

import kotlin.math.max
import kotlin.math.round

object DerivedValues {

    /**
     * Calculates proportional bar widths for the xG comparison bars on a 0.0 to 10.0 scale.
     * Minimum width is 22.0% (so label "xG TEAMNAME" fits comfortably).
     * Maximum width is 80.0% (leaving 20% for the xG number so it never overflows the panel).
     * Linear/subtle scale across 0.0 .. 10.0 xG so 10.0 is near the end.
     */
    fun calculateBarWidths(homeXg: Double, awayXg: Double, layout: String = "center_out"): Pair<Double, Double> {
        val (minPct, maxPct, maxScale) = when (layout) {
            "center_out" -> Triple(25.0, 100.0, 4.0)
            "side_by_side" -> Triple(42.0, 76.0, 10.0)
            else -> Triple(22.0, 80.0, 10.0) // "stacked"
        }
        val range = maxPct - minPct

        val clampedHome = homeXg.coerceIn(0.0, maxScale)
        val clampedAway = awayXg.coerceIn(0.0, maxScale)

        val homePct = minPct + (clampedHome / maxScale) * range
        val awayPct = minPct + (clampedAway / maxScale) * range

        return Pair(
            round(homePct * 10.0) / 10.0,
            round(awayPct * 10.0) / 10.0
        )
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
