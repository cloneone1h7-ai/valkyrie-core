package com.armo.poster.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.armo.poster.data.MatchStore
import com.armo.poster.data.MatchSummary
import com.armo.poster.domain.model.MatchData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val matchStore: MatchStore
) : ViewModel() {

    private val _matches = MutableStateFlow<List<MatchSummary>>(emptyList())
    val matches: StateFlow<List<MatchSummary>> = _matches.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _matches.value = matchStore.list()
        }
    }

    fun duplicateMatch(id: String, onDuplicated: (MatchData) -> Unit) {
        viewModelScope.launch {
            try {
                val newId = matchStore.duplicate(id)
                refresh()
                matchStore.load(newId)?.let(onDuplicated)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteMatch(id: String) {
        viewModelScope.launch {
            matchStore.delete(id)
            refresh()
        }
    }

    fun loadMatch(id: String): MatchData? {
        return matchStore.load(id)
    }
}
