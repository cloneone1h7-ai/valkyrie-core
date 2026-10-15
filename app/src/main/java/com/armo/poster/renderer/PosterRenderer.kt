package com.armo.poster.renderer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader
import com.armo.poster.domain.model.MatchData
import com.armo.poster.util.AppLog
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
        layoutParams = FrameLayout.LayoutParams(2400, 2400)
        translationY = -5000f
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        settings.javaScriptEnabled = true
        settings.allowFileAccess = true
        settings.domStorageEnabled = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = false // Prevent overview zoom, scaling handled in CSS/JS

        measure(
            View.MeasureSpec.makeMeasureSpec(2400, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(2400, View.MeasureSpec.EXACTLY)
        )
        layout(0, 0, 2400, 2400)
    }

    private val assetLoader = WebViewAssetLoader.Builder()
        .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
        .build()

    private var onReadyContinuation: (() -> Unit)? = null
    private var onErrorContinuation: ((String) -> Unit)? = null

    init {
        hostViewGroup.addView(offscreenWebView)

        offscreenWebView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(cm: ConsoleMessage?): Boolean {
                cm?.let {
                    AppLog.d("WebViewJS", "${it.message()} [${it.sourceId()}:${it.lineNumber()}]")
                }
                return true
            }
        }

        offscreenWebView.addJavascriptInterface(
            PosterBridge(
                onReadyCallback = {
                    AppLog.i("PosterBridge", "onReady signal received from JS")
                    onReadyContinuation?.invoke()
                    onReadyContinuation = null
                },
                onErrorCallback = { error ->
                    AppLog.e("PosterBridge", "onError signal received from JS: $error")
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
        val startTime = System.currentTimeMillis()
        try {
            val targetSize = 1200 * scale // 2400 (if scale=2) or 1200 (if scale=1)
            val density = context.resources.displayMetrics.density
            val targetCssSize = targetSize / density

            AppLog.i(
                "PosterRenderer",
                "render() start -> scale=$scale, targetSize=${targetSize}px, density=$density, targetCssSize=${targetCssSize}px"
            )

            // Step 0: Dynamically measure & layout offscreen host and WebView to target size
            hostViewGroup.layoutParams = FrameLayout.LayoutParams(targetSize, targetSize)
            hostViewGroup.measure(
                View.MeasureSpec.makeMeasureSpec(targetSize, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(targetSize, View.MeasureSpec.EXACTLY)
            )
            hostViewGroup.layout(0, 0, targetSize, targetSize)

            offscreenWebView.layoutParams = FrameLayout.LayoutParams(targetSize, targetSize)
            offscreenWebView.measure(
                View.MeasureSpec.makeMeasureSpec(targetSize, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(targetSize, View.MeasureSpec.EXACTLY)
            )
            offscreenWebView.layout(0, 0, targetSize, targetSize)

            val payload = HtmlBuilder.buildPayload(data)
            var fontTimeoutOccurred = false

            // Step 1: Wait for page load
            val pageLoadStart = System.currentTimeMillis()
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
                AppLog.e("PosterRenderer", "Failed to load poster HTML template")
                return@withContext RenderResult.Failed("Failed to load poster HTML template")
            }
            AppLog.i("PosterRenderer", "Page loaded in ${System.currentTimeMillis() - pageLoadStart}ms")

            // Step 2: Inject payload and await JS ready signal
            val escapedPayload = payload.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", "\\n")
                .replace("\r", "")

            val jsReadyReceived = withTimeoutOrNull(3500L) {
                suspendCancellableCoroutine<Boolean> { cont ->
                    val resumed = AtomicBoolean(false)
                    onReadyContinuation = {
                        if (resumed.compareAndSet(false, true) && cont.isActive) {
                            cont.resume(true)
                        }
                    }
                    onErrorContinuation = { err ->
                        AppLog.w("PosterRenderer", "JS signaled error: $err")
                        if (resumed.compareAndSet(false, true) && cont.isActive) {
                            cont.resume(true)
                        }
                    }

                    offscreenWebView.evaluateJavascript("window.__armo.build('$escapedPayload')", null)
                }
            }

            if (jsReadyReceived == null) {
                fontTimeoutOccurred = true
                AppLog.w("PosterRenderer", "Font/Asset ready wait timed out (3.5s) — fallback applied")
            }

            // Step 3: Explicitly align viewport scale to targetCssSize
            evaluateJs("window.__armo.applyViewportScale($targetCssSize, $targetCssSize)")

            // Step 4: Run fit ladder (Levels 1 to 10)
            var n = 0
            var metrics = queryMetrics()
            AppLog.i("PosterRenderer", "Initial metrics (fit 0): innerH=${metrics.innerH}, contentH=${metrics.contentH}, fits=${metrics.fits}")

            while (n < FitLadder.MAX_LEVEL && !metrics.fits) {
                n++
                evaluateJs("window.__armo.fit($n)")
                delay(30) // Wait for reflow
                metrics = queryMetrics()
                AppLog.i("PosterRenderer", "Fit step $n: innerH=${metrics.innerH}, contentH=${metrics.contentH}, fits=${metrics.fits}")
            }

            // If ladder did not fully fit, apply adaptive transform scaling
            if (!metrics.fits) {
                AppLog.i("PosterRenderer", "Content overflow at fit level $n (overflowPx=${metrics.overflowPx}). Applying autoScaleToFit()...")
                evaluateJs("window.__armo.autoScaleToFit()")
                delay(30)
                metrics = queryMetrics()
                AppLog.i("PosterRenderer", "After autoScaleToFit: innerH=${metrics.innerH}, contentH=${metrics.contentH}, scale=${metrics.scale}, fits=${metrics.fits}")
            }

            // Step 5: Draw directly to Bitmap (offscreenWebView is already measured & scaled to targetSize)
            val bitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            offscreenWebView.draw(canvas)

            // Step 6: Blank bitmap detection
            if (isBlankBitmap(bitmap)) {
                AppLog.w("PosterRenderer", "First draw produced blank bitmap; invalidating and retrying...")
                offscreenWebView.invalidate()
                delay(60)
                offscreenWebView.draw(canvas)
                if (isBlankBitmap(bitmap)) {
                    AppLog.e("PosterRenderer", "Generated bitmap is still blank after invalidate retry!")
                    return@withContext RenderResult.Failed("Generated bitmap is blank or unrendered")
                }
            }

            val totalTime = System.currentTimeMillis() - startTime
            AppLog.i(
                "PosterRenderer",
                "Render completed in ${totalTime}ms: ${bitmap.width}x${bitmap.height} px, fitLevel=$n, fits=${metrics.fits}, scale=${metrics.scale}"
            )

            if (!metrics.fits) {
                AppLog.w("PosterRenderer", "Content residual overflow: overflowPx=${metrics.overflowPx}")
                return@withContext RenderResult.TooLong(
                    bitmap = bitmap,
                    overflowPx = metrics.overflowPx,
                    fitLevel = n
                )
            }

            return@withContext RenderResult.Success(
                bitmap = bitmap,
                fitLevel = n,
                metrics = metrics,
                fontTimeout = fontTimeoutOccurred
            )


        } catch (e: Exception) {
            AppLog.e("PosterRenderer", "Unexpected error during render: ${e.message}", e)
            return@withContext RenderResult.Failed(e.message ?: "Unknown render failure")
        }
    }

    private suspend fun queryMetrics(): FitMetrics {
        val resultStr = evaluateJs("JSON.stringify(window.__armo.measure())")
        val cleanJson = resultStr?.trim()?.removeSurrounding("\"")?.replace("\\\"", "\"") ?: "{}"
        return try {
            json.decodeFromString<FitMetrics>(cleanJson)
        } catch (e: Exception) {
            AppLog.w("PosterRenderer", "Failed to parse metrics JSON: $cleanJson (${e.message})")
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
