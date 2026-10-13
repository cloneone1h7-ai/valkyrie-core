package com.armo.poster.renderer

import android.webkit.JavascriptInterface

class PosterBridge(
    private val onReadyCallback: () -> Unit,
    private val onErrorCallback: (String) -> Unit = {}
) {

    @JavascriptInterface
    fun onReady() {
        onReadyCallback()
    }

    @JavascriptInterface
    fun onError(message: String) {
        onErrorCallback(message)
    }

    @JavascriptInterface
    fun onFitReport(json: String) {
        // Optional report hook
    }
}
