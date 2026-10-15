package com.armo.poster.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.webkit.WebSettings
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.webkit.WebViewCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue

enum class LogLevel {
    DEBUG, INFO, WARN, ERROR
}

data class LogEntry(
    val id: Long,
    val timestamp: Long,
    val level: LogLevel,
    val tag: String,
    val message: String,
    val stackTrace: String? = null
) {
    fun toFormattedString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
        val timeStr = sdf.format(Date(timestamp))
        val base = "[$timeStr] [${level.name.padEnd(5)}] [$tag] $message"
        return if (stackTrace != null) "$base\n$stackTrace" else base
    }
}

object AppLog {

    private const val MAX_MEMORY_LOGS = 1000
    private var appContext: Context? = null
    private var logFile: File? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private var entrySequence = 0L

    private val logQueue = ConcurrentLinkedQueue<LogEntry>()
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    fun init(context: Context) {
        appContext = context.applicationContext
        try {
            val logsDir = File(context.filesDir, "logs").apply { if (!exists()) mkdirs() }
            logFile = File(logsDir, "armo_app.log")
            i("AppLog", "=== ARMO Poster App Initialized ===")
            i("AppLog", "Device: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}, API ${Build.VERSION.SDK_INT})")
            i("AppLog", "ABIs: ${Build.SUPPORTED_ABIS.joinToString(", ")}")
            val metrics = context.resources.displayMetrics
            i("AppLog", "Screen: ${metrics.widthPixels}x${metrics.heightPixels} px, density=${metrics.density}, dpi=${metrics.densityDpi}")
        } catch (e: Exception) {
            Log.e("AppLog", "Failed to initialize log file", e)
        }
    }

    fun d(tag: String, message: String) = log(LogLevel.DEBUG, tag, message, null)
    fun i(tag: String, message: String) = log(LogLevel.INFO, tag, message, null)
    fun w(tag: String, message: String, throwable: Throwable? = null) = log(LogLevel.WARN, tag, message, throwable)
    fun e(tag: String, message: String, throwable: Throwable? = null) = log(LogLevel.ERROR, tag, message, throwable)

    private fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        val stackTrace = throwable?.let {
            val sw = StringWriter()
            it.printStackTrace(PrintWriter(sw))
            sw.toString().trim()
        }

        when (level) {
            LogLevel.DEBUG -> Log.d(tag, message, throwable)
            LogLevel.INFO -> Log.i(tag, message, throwable)
            LogLevel.WARN -> Log.w(tag, message, throwable)
            LogLevel.ERROR -> Log.e(tag, message, throwable)
        }

        val entry = LogEntry(
            id = ++entrySequence,
            timestamp = System.currentTimeMillis(),
            level = level,
            tag = tag,
            message = message,
            stackTrace = stackTrace
        )

        logQueue.add(entry)
        while (logQueue.size > MAX_MEMORY_LOGS) {
            logQueue.poll()
        }
        _logs.value = logQueue.toList()

        // Append to persistent log file asynchronously
        scope.launch {
            try {
                logFile?.let { file ->
                    FileWriter(file, true).use { writer ->
                        writer.append(entry.toFormattedString()).append("\n")
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun clear() {
        logQueue.clear()
        _logs.value = emptyList()
        scope.launch {
            try {
                logFile?.writeText("")
            } catch (_: Exception) {}
        }
        i("AppLog", "Logs cleared by user")
    }

    fun getFormattedFullLog(context: Context): String = buildString {
        appendLine("================================================================")
        appendLine("           ARMO NOTES POSTER - SYSTEM DIAGNOSTIC LOG            ")
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss (z)", Locale.US)
        appendLine("Exported: ${sdf.format(Date())}")
        appendLine("================================================================")
        appendLine()

        appendLine("--- DEVICE TELEMETRY ---")
        appendLine("Manufacturer: ${Build.MANUFACTURER}")
        appendLine("Model:        ${Build.MODEL}")
        appendLine("Device:       ${Build.DEVICE}")
        appendLine("Product:      ${Build.PRODUCT}")
        appendLine("Hardware:     ${Build.HARDWARE}")
        appendLine("Android OS:   ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        appendLine("Build ID:     ${Build.DISPLAY}")
        appendLine("Supported ABI:${Build.SUPPORTED_ABIS.joinToString(", ")}")
        appendLine()

        appendLine("--- DISPLAY METRICS ---")
        val dm = context.resources.displayMetrics
        val widthDp = dm.widthPixels / dm.density
        val heightDp = dm.heightPixels / dm.density
        appendLine("Resolution:   ${dm.widthPixels} x ${dm.heightPixels} px")
        appendLine("Density:      ${dm.density} (factor)")
        appendLine("DPI:          ${dm.densityDpi} dpi")
        appendLine("ScaledDensity:${dm.scaledDensity}")
        appendLine("Screen DP:    ${widthDp.toInt()} x ${heightDp.toInt()} dp")
        appendLine()

        appendLine("--- SYSTEM WEBVIEW ---")
        try {
            val webViewPackage = WebViewCompat.getCurrentWebViewPackage(context)
            if (webViewPackage != null) {
                appendLine("Package:      ${webViewPackage.packageName}")
                appendLine("Version:      ${webViewPackage.versionName}")
            } else {
                appendLine("User-Agent:   ${WebSettings.getDefaultUserAgent(context)}")
            }
        } catch (e: Exception) {
            appendLine("WebView info: ${e.message}")
        }
        appendLine()

        appendLine("--- MEMORY STATUS ---")
        val rt = Runtime.getRuntime()
        val mb = 1024 * 1024
        val maxMem = rt.maxMemory() / mb
        val totalMem = rt.totalMemory() / mb
        val freeMem = rt.freeMemory() / mb
        val usedMem = totalMem - freeMem
        appendLine("Max JVM Heap: ${maxMem} MB")
        appendLine("Total Heap:   ${totalMem} MB")
        appendLine("Used Heap:    ${usedMem} MB")
        appendLine("Free Heap:    ${freeMem} MB")
        appendLine()

        val entries = _logs.value
        appendLine("--- EVENT LOGS (${entries.size} entries) ---")
        for (entry in entries) {
            appendLine(entry.toFormattedString())
        }
        appendLine()
        appendLine("====================== END OF DIAGNOSTIC LOG =====================")
    }

    fun copyToClipboard(context: Context) {
        try {
            val fullLog = getFormattedFullLog(context)
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("ARMO Poster Diagnostic Log", fullLog)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "تمام لاگ‌ها در کلیپ‌بورد کپی شدند", Toast.LENGTH_SHORT).show()
            i("AppLog", "Diagnostic logs copied to clipboard (${fullLog.length} chars)")
        } catch (e: Exception) {
            Toast.makeText(context, "خطا در کپی لاگ‌ها: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun exportLogFile(context: Context): File? {
        return try {
            val cacheDir = File(context.cacheDir, "logs").apply { if (!exists()) mkdirs() }
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(cacheDir, "armo_poster_log_$timestamp.txt")
            file.writeText(getFormattedFullLog(context))
            file
        } catch (e: Exception) {
            this.e("AppLog", "Failed to export log file", e)
            null
        }
    }

    fun shareLogFile(context: Context) {
        scope.launch(Dispatchers.IO) {
            val file = exportLogFile(context)
            if (file == null) {
                launch(Dispatchers.Main) {
                    Toast.makeText(context, "خطا در ایجاد فایل لاگ", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            try {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "ARMO Poster Diagnostic Log (${file.name})")
                    putExtra(Intent.EXTRA_TEXT, "ARMO NOTES App Diagnostic Log (${file.name})")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val chooser = Intent.createChooser(intent, "ارسال فایل لاگ ARMO").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                launch(Dispatchers.Main) {
                    try {
                        context.startActivity(chooser)
                        i("AppLog", "Log share sheet presented for ${file.name}")
                    } catch (e: Exception) {
                        Toast.makeText(context, "برنامه‌ای برای ارسال فایل پیدا نشد", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    Toast.makeText(context, "خطا در اشتراک‌گذاری: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
