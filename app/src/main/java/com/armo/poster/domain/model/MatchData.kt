package com.armo.poster.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class MatchData(
    val id: String,
    val createdAt: Long,
    val gwLabel: String,
    val home: TeamRef,
    val away: TeamRef,
    val homeFormation: String = "",
    val awayFormation: String = "",
    val homeXg: Double = 0.0,
    val awayXg: Double = 0.0,
    val gameNotes: List<String> = emptyList(),
    val homeBlocks: List<PlayerBlock> = emptyList(),
    val awayBlocks: List<PlayerBlock> = emptyList(),
    val takeaways: List<String> = emptyList()
)

@Serializable
data class TeamRef(
    val name: String,
    val abbr: String,
    val color: String,
    val crestAsset: String? = null
)

@Serializable
data class PlayerBlock(
    val name: String,
    val notes: List<String> = emptyList()
)

@Serializable
data class PairedBlock(
    val left: PlayerBlock?,
    val right: PlayerBlock?
)
