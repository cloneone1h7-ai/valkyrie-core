package com.armo.poster.renderer

import com.armo.poster.domain.model.MatchData
import com.armo.poster.domain.model.PlayerBlock
import com.armo.poster.domain.model.TeamRef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HtmlBuilderTest {

    @Test
    fun buildPayload_generatesValidJsonWithDerivedValues() {
        val match = MatchData(
            id = "test-1",
            createdAt = 1000L,
            gwLabel = "GW6",
            home = TeamRef(name = "Chelsea", abbr = "CHE", color = "#034694", crestAsset = "logos/chelsea-apisports.png"),
            away = TeamRef(name = "Bournemouth", abbr = "BOU", color = "#DA291C", crestAsset = "logos/bournemouth-apisports.png"),
            homeFormation = "3-4-2-1",
            awayFormation = "4-2-3-1",
            homeXg = 1.10,
            awayXg = 1.38,
            gameNotes = listOf("Game note 1"),
            homeBlocks = listOf(PlayerBlock("Rogers", listOf("Scored"))),
            awayBlocks = listOf(PlayerBlock("Tavernier", listOf("No shots"))),
            takeaways = listOf("Chelsea were better")
        )

        val json = HtmlBuilder.buildPayload(match, assetBaseUrl = "https://appassets.androidplatform.net/assets/")
        assertTrue(json.contains("\"gwLabel\":\"GW6\""))
        assertTrue(json.contains("\"homeBarWidthPct\":45.6"))
        assertTrue(json.contains("\"awayBarWidthPct\":50.9"))
        assertTrue(json.contains("\"homeXgStr\":\"1.10\""))
        assertTrue(json.contains("\"awayXgStr\":\"1.38\""))
        assertTrue(json.contains("chelsea-apisports.png"))
    }
}
