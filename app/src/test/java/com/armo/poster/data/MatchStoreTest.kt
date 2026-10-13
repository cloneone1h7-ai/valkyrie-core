package com.armo.poster.data

import com.armo.poster.domain.model.MatchData
import com.armo.poster.domain.model.PlayerBlock
import com.armo.poster.domain.model.TeamRef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class MatchStoreTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun saveLoadListDuplicateDelete_cycleWorksCorrectly() {
        val rootDir = tempFolder.newFolder("matches_test")
        val store = MatchStore(rootDir)

        val match = MatchData(
            id = "test-match-1",
            createdAt = 1728800000000L,
            gwLabel = "GW6",
            home = TeamRef(name = "Chelsea", abbr = "CHE", color = "#034694", crestAsset = "logos/chelsea-apisports.png"),
            away = TeamRef(name = "Bournemouth", abbr = "BOU", color = "#DA291C", crestAsset = "logos/bournemouth-apisports.png"),
            homeFormation = "3-4-2-1",
            awayFormation = "4-2-3-1",
            homeXg = 1.10,
            awayXg = 1.38,
            gameNotes = listOf("Note 1"),
            homeBlocks = listOf(PlayerBlock(name = "Rogers", notes = listOf("Played well"))),
            awayBlocks = listOf(PlayerBlock(name = "Tavernier", notes = listOf("No shots"))),
            takeaways = listOf("Chelsea were better")
        )

        // Save
        val savedId = store.save(match)
        assertEquals("test-match-1", savedId)

        // List
        val list = store.list()
        assertEquals(1, list.size)
        assertEquals("test-match-1", list[0].id)
        assertEquals("CHE vs BOU", list[0].headline)

        // Load
        val loaded = store.load("test-match-1")
        assertNotNull(loaded)
        assertEquals(match.gwLabel, loaded?.gwLabel)
        assertEquals(match.homeXg, loaded?.homeXg ?: 0.0, 0.001)

        // Duplicate
        val duplicateId = store.duplicate("test-match-1")
        assertTrue(duplicateId.isNotEmpty())
        assertEquals(2, store.list().size)

        // Delete original
        val deleted = store.delete("test-match-1")
        assertTrue(deleted)
        assertEquals(1, store.list().size)
    }
}
