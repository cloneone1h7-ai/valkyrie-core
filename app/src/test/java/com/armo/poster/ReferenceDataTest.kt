package com.armo.poster

import com.armo.poster.data.SampleData
import com.armo.poster.domain.model.DerivedValues
import com.armo.poster.renderer.FitLadder
import com.armo.poster.renderer.FitMetrics
import com.armo.poster.renderer.HtmlBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReferenceDataTest {

    @Test
    fun referenceData_matchesSuccessCriteria() {
        val sample = SampleData.cheVsBou

        // 1. Bar widths
        val (homePct, awayPct) = DerivedValues.calculateBarWidths(sample.homeXg, sample.awayXg)
        assertEquals(79.7, homePct, 0.1)
        assertEquals(100.0, awayPct, 0.0)

        // 2. Headline format
        val headline = DerivedValues.formatHeadline(sample.home.abbr, sample.away.abbr)
        assertEquals("CHE vs BOU", headline)

        // 3. Pairing
        val paired = DerivedValues.pairPlayerBlocks(sample.homeBlocks, sample.awayBlocks)
        assertEquals(4, paired.size)
        assertEquals("Rogers", paired[0].left?.name)
        assertEquals("Tavernier", paired[0].right?.name)
        assertEquals("Joao Pedro", paired[3].left?.name)
        assertEquals(null, paired[3].right)

        // 4. Payload generation
        val payload = HtmlBuilder.buildPayload(sample)
        assertTrue(payload.contains("CHE"))
        assertTrue(payload.contains("BOU"))
        assertTrue(payload.contains("79.7"))
        assertTrue(payload.contains("100.0"))

        // 5. Fit test simulation (Reference data fits cleanly within inner frame)
        val mockReferenceMetrics = FitMetrics(innerH = 1112, contentH = 1084)
        assertTrue(FitLadder.isFit(mockReferenceMetrics))
    }
}
