package com.armo.poster.renderer

import android.graphics.Bitmap
import kotlinx.serialization.Serializable

@Serializable
data class FitMetrics(
    val innerH: Int = 0,
    val contentH: Int = 0,
    val unscaledH: Int = 0,
    val scale: Double = 1.0
) {
    val overflowPx: Int
        get() = (contentH - innerH).coerceAtLeast(0)

    val fits: Boolean
        get() = contentH <= innerH
}

sealed class RenderResult {
    data class Success(
        val bitmap: Bitmap,
        val fitLevel: Int,
        val metrics: FitMetrics,
        val fontTimeout: Boolean = false
    ) : RenderResult()

    data class TooLong(
        val bitmap: Bitmap? = null,
        val overflowPx: Int,
        val fitLevel: Int
    ) : RenderResult()

    data class Failed(
        val reason: String
    ) : RenderResult()
}
