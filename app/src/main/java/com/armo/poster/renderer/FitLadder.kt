package com.armo.poster.renderer

object FitLadder {
    const val MAX_LEVEL = 10

    fun isFit(metrics: FitMetrics): Boolean {
        return metrics.contentH <= metrics.innerH
    }
}
