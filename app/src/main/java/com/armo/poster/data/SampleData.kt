package com.armo.poster.data

import com.armo.poster.domain.model.MatchData
import com.armo.poster.domain.model.PlayerBlock
import com.armo.poster.domain.model.TeamRef

object SampleData {

    val cheVsBou = MatchData(
        id = "reference-che-bou-gw6",
        createdAt = 1728800000000L,
        gwLabel = "GW6",
        home = TeamRef(
            name = "Chelsea",
            abbr = "CHE",
            color = "#034694",
            crestAsset = "logos/chelsea-apisports.png"
        ),
        away = TeamRef(
            name = "Bournemouth",
            abbr = "BOU",
            color = "#DA291C",
            crestAsset = "logos/bournemouth-apisports.png"
        ),
        homeFormation = "3-4-2-1",
        awayFormation = "4-2-3-1",
        homeXg = 1.10,
        awayXg = 1.38,
        gameNotes = listOf(
            "Chelsea were much better organised, with excellent attacking moves, good pressing and clinical finishing."
        ),
        homeBlocks = listOf(
            PlayerBlock(
                name = "Rogers",
                notes = listOf(
                    "Excellent attacking runs and a strong presence in the box",
                    "Scored a good goal and maintained his form"
                )
            ),
            PlayerBlock(
                name = "Palmer",
                notes = listOf(
                    "Created 2 chances and focused on playmaking as usual",
                    "Spent much more time inside the box than in previous games"
                )
            ),
            PlayerBlock(
                name = "Henderson",
                notes = listOf(
                    "Played mostly in midfield and around the edge of the box",
                    "Had the most shots in the game (3) and took set pieces away from Palmer"
                )
            ),
            PlayerBlock(
                name = "Joao Pedro",
                notes = listOf(
                    "Not his best performance statistically, but once again showed his ability to score from difficult chances"
                )
            )
        ),
        awayBlocks = listOf(
            PlayerBlock(
                name = "Tavernier",
                notes = listOf(
                    "Had no shots in the game"
                )
            ),
            PlayerBlock(
                name = "Rayan",
                notes = listOf(
                    "Missed a one-on-one"
                )
            ),
            PlayerBlock(
                name = "Evanilson",
                notes = listOf(
                    "His goal was fortunate",
                    "A disappointing performance from all three"
                )
            )
        ),
        takeaways = listOf(
            "Love the 3 Chelsea upfront But Rogers > Palmer for me",
            "Bournemouth: only Tavernier when fixtures improve"
        )
    )
}
