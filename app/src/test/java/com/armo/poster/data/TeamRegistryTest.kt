package com.armo.poster.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TeamRegistryTest {

    private val sampleJson = """
    {
      "premier_league": {
        "Arsenal": { "id": 42, "abbr": "ARS", "color": "#EF0107", "crest": "logos/arsenal-apisports.png" },
        "Chelsea": { "id": 49, "abbr": "CHE", "color": "#034694", "crest": "logos/chelsea-apisports.png" },
        "Bournemouth": { "id": 35, "abbr": "BOU", "color": "#DA291C", "crest": "logos/bournemouth-apisports.png" }
      }
    }
    """.trimIndent()

    @Test
    fun parseJson_loadsTeamsCorrectly() {
        val registry = TeamRegistry()
        registry.loadFromJsonString(sampleJson)

        assertEquals(3, registry.allTeams().size)

        val chelsea = registry.getTeam("Chelsea")
        assertNotNull(chelsea)
        assertEquals("CHE", chelsea?.abbr)
        assertEquals("#034694", chelsea?.color)
        assertEquals("logos/chelsea-apisports.png", chelsea?.crestAsset)
    }

    @Test
    fun getTeam_caseInsensitiveLookup() {
        val registry = TeamRegistry()
        registry.loadFromJsonString(sampleJson)

        val bou = registry.getTeam("bournemouth")
        assertNotNull(bou)
        assertEquals("BOU", bou?.abbr)
    }
}
