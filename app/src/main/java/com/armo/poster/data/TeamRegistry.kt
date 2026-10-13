package com.armo.poster.data

import android.content.Context
import com.armo.poster.domain.model.TeamRef
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.InputStreamReader

@Serializable
private data class TeamsRoot(
    val premier_league: Map<String, TeamEntry> = emptyMap()
)

@Serializable
private data class TeamEntry(
    val id: Int,
    val abbr: String,
    val color: String,
    val crest: String
)

class TeamRegistry {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val teams = mutableMapOf<String, TeamRef>()

    fun loadFromAssets(context: Context) {
        try {
            context.assets.open("teams.json").use { inputStream ->
                val content = InputStreamReader(inputStream).readText()
                loadFromJsonString(content)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadFromJsonString(jsonString: String) {
        try {
            val root = json.decodeFromString<TeamsRoot>(jsonString)
            teams.clear()
            for ((name, entry) in root.premier_league) {
                teams[name.lowercase()] = TeamRef(
                    name = name,
                    abbr = entry.abbr,
                    color = entry.color,
                    crestAsset = entry.crest
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getTeam(name: String): TeamRef? {
        return teams[name.lowercase()]
    }

    fun allTeams(): List<TeamRef> {
        return teams.values.toList().sortedBy { it.name }
    }

    fun findTeamByAbbr(abbr: String): TeamRef? {
        return teams.values.firstOrNull { it.abbr.equals(abbr, ignoreCase = true) }
    }
}
