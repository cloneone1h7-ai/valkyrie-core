package com.armo.poster.renderer

import com.armo.poster.domain.model.DerivedValues
import com.armo.poster.domain.model.MatchData
import com.armo.poster.domain.model.PairedBlock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Locale

@Serializable
data class PosterPayload(
    val gwLabel: String,
    val homeName: String,
    val homeAbbr: String,
    val homeColor: String,
    val homeCrestUrl: String,
    val awayName: String,
    val awayAbbr: String,
    val awayColor: String,
    val awayCrestUrl: String,
    val homeFormation: String,
    val awayFormation: String,
    val homeXgStr: String,
    val awayXgStr: String,
    val homeBarWidthPct: Double,
    val awayBarWidthPct: Double,
    val gameNotes: List<String>,
    val pairedRows: List<PairedBlock>,
    val takeaways: List<String>,
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

object HtmlBuilder {

    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun buildPayload(data: MatchData, assetBaseUrl: String = "https://appassets.androidplatform.net/assets/"): String {
        val (homePct, awayPct) = DerivedValues.calculateBarWidths(data.homeXg, data.awayXg, data.xgLayout)
        val paired = DerivedValues.pairPlayerBlocks(data.homeBlocks, data.awayBlocks)

        val homeCrestUrl = resolveCrestUrl(data.home.crestAsset, assetBaseUrl)
        val awayCrestUrl = resolveCrestUrl(data.away.crestAsset, assetBaseUrl)

        val payload = PosterPayload(
            gwLabel = data.gwLabel,
            homeName = data.home.name,
            homeAbbr = data.home.abbr.take(3).uppercase(),
            homeColor = data.home.color,
            homeCrestUrl = homeCrestUrl,
            awayName = data.away.name,
            awayAbbr = data.away.abbr.take(3).uppercase(),
            awayColor = data.away.color,
            awayCrestUrl = awayCrestUrl,
            homeFormation = data.homeFormation,
            awayFormation = data.awayFormation,
            homeXgStr = String.format(Locale.US, "%.2f", data.homeXg),
            awayXgStr = String.format(Locale.US, "%.2f", data.awayXg),
            homeBarWidthPct = homePct,
            awayBarWidthPct = awayPct,
            gameNotes = data.gameNotes,
            pairedRows = paired,
            takeaways = data.takeaways,
            xgLayout = data.xgLayout,
            fontSizeScale = data.fontSizeScale,
            canvasSpace = data.canvasSpace,
            highContrast = data.highContrast,
            bodyFontSize = data.bodyFontSize,
            nameFontSize = data.nameFontSize,
            fontWeight = data.fontWeight,
            lineHeight = data.lineHeight,
            fontFamily = data.fontFamily,
            bgColor = data.bgColor,
            panelColor = data.panelColor,
            accentColor = data.accentColor,
            bodyTextColor = data.bodyTextColor
        )

        return json.encodeToString(payload)
    }

    private fun resolveCrestUrl(crestAsset: String?, assetBaseUrl: String): String {
        if (crestAsset.isNullOrBlank()) {
            return ""
        }
        return if (crestAsset.startsWith("http://") || crestAsset.startsWith("https://") || crestAsset.startsWith("file://")) {
            crestAsset
        } else {
            // e.g. logos/chelsea-apisports.png
            assetBaseUrl.trimEnd('/') + "/" + crestAsset.trimStart('/')
        }
    }
}
