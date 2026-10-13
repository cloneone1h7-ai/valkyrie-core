package com.armo.poster.renderer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader
import com.armo.poster.domain.model.MatchData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

class PosterRenderer(
    private val context: Context,
    private val hostViewGroup: ViewGroup
) {

    private val json = Json { ignoreUnknownKeys = true }
    private val offscreenWebView: WebView = WebView(context).apply {
        layoutParams = ViewGroup.LayoutParams(1200, 1200)
        translationY = -5000f
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        settings.javaScriptEnabled = true
        settings.allowFileAccess = true
        settings.domStorageEnabled = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true

        measure(
            View.MeasureSpec.makeMeasureSpec(1200, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1200, View.MeasureSpec.EXACTLY)
        )
        layout(0, 0, 1200, 1200)
    }

    private val assetLoader = WebViewAssetLoader.Builder()
        .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
        .build()

    private var onReadyContinuation: (() -> Unit)? = null
    private var onErrorContinuation: ((String) -> Unit)? = null

    init {
        hostViewGroup.addView(offscreenWebView)

        offscreenWebView.addJavascriptInterface(
            PosterBridge(
                onReadyCallback = {
                    onReadyContinuation?.invoke()
                    onReadyContinuation = null
                },
                onErrorCallback = { error ->
                    onErrorContinuation?.invoke(error)
                    onErrorContinuation = null
                }
            ),
            "PosterBridge"
        )

        offscreenWebView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? {
                return assetLoader.shouldInterceptRequest(request.url)
            }
        }
    }

    suspend fun render(data: MatchData, scale: Int = 2): RenderResult = withContext(Dispatchers.Main) {
        try {
            val payload = HtmlBuilder.buildPayload(data)
            var fontTimeoutOccurred = false

            // Step 1: Wait for page load
            val pageLoaded = suspendCancellableCoroutine<Boolean> { cont ->
                offscreenWebView.webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView,
                        request: WebResourceRequest
                    ): WebResourceResponse? {
                        return assetLoader.shouldInterceptRequest(request.url)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        if (cont.isActive) cont.resume(true)
                    }
                }
                offscreenWebView.loadUrl("https://appassets.androidplatform.net/assets/poster/poster.html")
            }

            if (!pageLoaded) {
                return@withContext RenderResult.Failed("Failed to load poster HTML template")
            }

            // Step 2: Inject payload and await JS ready signal
            val escapedPayload = payload.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", "\\n")
                .replace("\r", "")

            val jsReadyReceived = withTimeoutOrNull(3000L) {
                suspendCancellableCoroutine<Boolean> { cont ->
                    val resumed = AtomicBoolean(false)
                    onReadyContinuation = {
                        if (resumed.compareAndSet(false, true) && cont.isActive) {
                            cont.resume(true)
                        }
                    }
                    onErrorContinuation = { _ ->
                        if (resumed.compareAndSet(false, true) && cont.isActive) {
                            cont.resume(true)
                        }
                    }

                    offscreenWebView.evaluateJavascript("window.__armo.build('$escapedPayload')", null)
                }
            }

            if (jsReadyReceived == null) {
                fontTimeoutOccurred = true
            }

            // Step 3: Run fit ladder
            var n = 0
            var metrics = queryMetrics()

            while (n < FitLadder.MAX_LEVEL && !metrics.fits) {
                n++
                evaluateJs("window.__armo.fit($n)")
                delay(40) // Wait for reflow
                metrics = queryMetrics()
            }

            if (!metrics.fits) {
                return@withContext RenderResult.TooLong(
                    overflowPx = metrics.overflowPx,
                    fitLevel = n
                )
            }

            // Step 4: Draw to Bitmap
            val targetSize = 1200 * scale
            val bitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.scale(scale.toFloat(), scale.toFloat())

            offscreenWebView.draw(canvas)

            // Step 5: Blank bitmap detection
            if (isBlankBitmap(bitmap)) {
                offscreenWebView.invalidate()
                delay(50)
                offscreenWebView.draw(canvas)
                if (isBlankBitmap(bitmap)) {
                    return@withContext RenderResult.Failed("Generated bitmap is blank or unrendered")
                }
            }

            return@withContext RenderResult.Success(
                bitmap = bitmap,
                fitLevel = n,
                metrics = metrics,
                fontTimeout = fontTimeoutOccurred
            )

        } catch (e: Exception) {
            return@withContext RenderResult.Failed(e.message ?: "Unknown render failure")
        }
    }

    private suspend fun queryMetrics(): FitMetrics {
        val resultStr = evaluateJs("JSON.stringify(window.__armo.measure())")
        val cleanJson = resultStr?.trim()?.removeSurrounding("\"")?.replace("\\\"", "\"") ?: "{}"
        return try {
            json.decodeFromString<FitMetrics>(cleanJson)
        } catch (_: Exception) {
            FitMetrics(innerH = 1112, contentH = 1112)
        }
    }

    private suspend fun evaluateJs(script: String): String? = suspendCancellableCoroutine { cont ->
        offscreenWebView.evaluateJavascript(script) { result ->
            if (cont.isActive) cont.resume(result)
        }
    }

    private fun isBlankBitmap(bitmap: Bitmap): Boolean {
        val firstPixel = bitmap.getPixel(0, 0)
        val samplePoints = listOf(
            Pair(bitmap.width / 4, bitmap.height / 4),
            Pair(bitmap.width / 2, bitmap.height / 2),
            Pair(3 * bitmap.width / 4, 3 * bitmap.height / 4),
            Pair(bitmap.width / 2, bitmap.height / 4)
        )
        return samplePoints.all { (x, y) -> bitmap.getPixel(x, y) == firstPixel } &&
                Color.alpha(firstPixel) == 0
    }
}
