package com.armo.poster.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.armo.poster.data.MatchStore
import com.armo.poster.data.TeamRegistry
import com.armo.poster.domain.model.MatchData
import com.armo.poster.domain.model.PlayerBlock
import com.armo.poster.domain.model.TeamRef
import com.armo.poster.renderer.HtmlBuilder
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.UUID

@OptIn(FlowPreview::class)
class EditorViewModel(
    private val teamRegistry: TeamRegistry,
    private val matchStore: MatchStore
) : ViewModel() {

    private val defaultMatch = MatchData(
        id = UUID.randomUUID().toString(),
        createdAt = System.currentTimeMillis(),
        gwLabel = "GW6",
        home = teamRegistry.getTeam("Chelsea") ?: TeamRef("Chelsea", "CHE", "#034694", "logos/chelsea-apisports.png"),
        away = teamRegistry.getTeam("Bournemouth") ?: TeamRef("Bournemouth", "BOU", "#DA291C", "logos/bournemouth-apisports.png"),
        homeFormation = "3-4-2-1",
        awayFormation = "4-2-3-1",
        homeXg = 1.10,
        awayXg = 1.38,
        gameNotes = listOf("Chelsea were much better organised, with excellent attacking moves, good pressing and clinical finishing."),
        homeBlocks = listOf(
            PlayerBlock("Rogers", listOf("Excellent attacking runs and a strong presence in the box", "Scored a good goal and maintained his form")),
            PlayerBlock("Palmer", listOf("Created 2 chances and focused on playmaking as usual", "Spent much more time inside the box than in previous games")),
            PlayerBlock("Henderson", listOf("Played mostly in midfield and around the edge of the box", "Had the most shots in the game (3) and took set pieces away from Palmer")),
            PlayerBlock("Joao Pedro", listOf("Not his best performance statistically, but once again showed his ability to score from difficult chances"))
        ),
        awayBlocks = listOf(
            PlayerBlock("Tavernier", listOf("Had no shots in the game")),
            PlayerBlock("Rayan", listOf("Missed a one-on-one")),
            PlayerBlock("Evanilson", listOf("His goal was fortunate", "A disappointing performance from all three"))
        ),
        takeaways = listOf(
            "Love the 3 Chelsea upfront But Rogers > Palmer for me",
            "Bournemouth: only Tavernier when fixtures improve"
        )
    )

    private val _matchData = MutableStateFlow(defaultMatch)
    val matchData: StateFlow<MatchData> = _matchData.asStateFlow()

    val availableTeams: List<TeamRef> = teamRegistry.allTeams()

    val debouncedPayload: StateFlow<String> = _matchData
        .debounce(250L)
        .map { data -> HtmlBuilder.buildPayload(data) }
        .stateIn(viewModelScope, SharingStarted.Lazily, HtmlBuilder.buildPayload(_matchData.value))

    fun loadMatch(data: MatchData) {
        _matchData.value = data
    }

    fun saveCurrentMatch(): String {
        return matchStore.save(_matchData.value)
    }

    fun updateGwLabel(label: String) {
        _matchData.value = _matchData.value.copy(gwLabel = label)
    }

    fun updateHomeTeam(team: TeamRef) {
        _matchData.value = _matchData.value.copy(home = team)
    }

    fun updateAwayTeam(team: TeamRef) {
        _matchData.value = _matchData.value.copy(away = team)
    }

    fun updateHomeFormation(formation: String) {
        _matchData.value = _matchData.value.copy(homeFormation = formation)
    }

    fun updateAwayFormation(formation: String) {
        _matchData.value = _matchData.value.copy(awayFormation = formation)
    }

    fun updateHomeXg(xg: Double) {
        _matchData.value = _matchData.value.copy(homeXg = xg)
    }

    fun updateAwayXg(xg: Double) {
        _matchData.value = _matchData.value.copy(awayXg = xg)
    }

    fun addGameNote(note: String) {
        if (note.isNotBlank()) {
            _matchData.value = _matchData.value.copy(
                gameNotes = _matchData.value.gameNotes + note.trim()
            )
        }
    }

    fun removeGameNote(index: Int) {
        val notes = _matchData.value.gameNotes.toMutableList()
        if (index in notes.indices) {
            notes.removeAt(index)
            _matchData.value = _matchData.value.copy(gameNotes = notes)
        }
    }

    fun addHomePlayer(name: String, notes: List<String>) {
        if (name.isNotBlank()) {
            _matchData.value = _matchData.value.copy(
                homeBlocks = _matchData.value.homeBlocks + PlayerBlock(name.trim(), notes)
            )
        }
    }

    fun removeHomePlayer(index: Int) {
        val list = _matchData.value.homeBlocks.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            _matchData.value = _matchData.value.copy(homeBlocks = list)
        }
    }

    fun addAwayPlayer(name: String, notes: List<String>) {
        if (name.isNotBlank()) {
            _matchData.value = _matchData.value.copy(
                awayBlocks = _matchData.value.awayBlocks + PlayerBlock(name.trim(), notes)
            )
        }
    }

    fun removeAwayPlayer(index: Int) {
        val list = _matchData.value.awayBlocks.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            _matchData.value = _matchData.value.copy(awayBlocks = list)
        }
    }

    fun addTakeaway(text: String) {
        if (text.isNotBlank()) {
            _matchData.value = _matchData.value.copy(
                takeaways = _matchData.value.takeaways + text.trim()
            )
        }
    }

    fun removeTakeaway(index: Int) {
        val list = _matchData.value.takeaways.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            _matchData.value = _matchData.value.copy(takeaways = list)
        }
    }
}
