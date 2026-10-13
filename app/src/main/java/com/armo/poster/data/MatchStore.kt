package com.armo.poster.data

import com.armo.poster.domain.model.DerivedValues
import com.armo.poster.domain.model.MatchData
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

@Serializable
data class MatchSummary(
    val id: String,
    val createdAt: Long,
    val gwLabel: String,
    val headline: String,
    val homeName: String,
    val awayName: String
)

class MatchStore(private val storageDir: File) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    init {
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }
    }

    fun list(): List<MatchSummary> {
        val files = storageDir.listFiles { file -> file.isFile && file.extension == "json" } ?: return emptyList()
        val summaries = mutableListOf<MatchSummary>()

        for (file in files) {
            try {
                val content = file.readText()
                val match = json.decodeFromString<MatchData>(content)
                summaries.add(
                    MatchSummary(
                        id = match.id,
                        createdAt = match.createdAt,
                        gwLabel = match.gwLabel,
                        headline = DerivedValues.formatHeadline(match.home.abbr, match.away.abbr),
                        homeName = match.home.name,
                        awayName = match.away.name
                    )
                )
            } catch (e: Exception) {
                System.err.println("Skipping corrupt match file: ${file.name} - ${e.message}")
            }
        }
        return summaries.sortedByDescending { it.createdAt }
    }

    fun load(id: String): MatchData? {
        val file = File(storageDir, "$id.json")
        if (!file.exists()) return null
        return try {
            val content = file.readText()
            json.decodeFromString<MatchData>(content)
        } catch (e: Exception) {
            System.err.println("Failed to load match $id: ${e.message}")
            null
        }
    }

    fun save(data: MatchData): String {
        val matchId = if (data.id.isBlank()) UUID.randomUUID().toString() else data.id
        val toSave = if (data.id != matchId) data.copy(id = matchId) else data
        val file = File(storageDir, "$matchId.json")
        file.writeText(json.encodeToString(toSave))
        return matchId
    }

    fun duplicate(id: String): String {
        val original = load(id) ?: throw IllegalArgumentException("Match not found: $id")
        val newId = UUID.randomUUID().toString()
        val duplicated = original.copy(
            id = newId,
            createdAt = System.currentTimeMillis()
        )
        save(duplicated)
        return newId
    }

    fun delete(id: String): Boolean {
        val file = File(storageDir, "$id.json")
        return if (file.exists()) {
            file.delete()
        } else {
            false
        }
    }
}
