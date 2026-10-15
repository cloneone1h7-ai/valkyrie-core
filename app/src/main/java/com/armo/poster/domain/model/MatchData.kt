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
    val takeaways: List<String> = emptyList(),
    val xgLayout: String = "center_out",
    val fontSizeScale: String = "standard",
    val canvasSpace: String = "expanded",
    val highContrast: Boolean = false,
    val bodyFontSize: Int = 19,
    val nameFontSize: Int = 29,
    val fontWeight: Int = 400,
    val lineHeight: Double = 1.35,
    val fontFamily: String = "Inter",
    val bgColor: String = "#0B1A14",
    val panelColor: String = "rgba(255,255,255,0.035)",
    val accentColor: String = "#D4FF00",
    val bodyTextColor: String = "#E2E8F0"
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
