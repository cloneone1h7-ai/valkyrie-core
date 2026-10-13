package com.armo.poster.export

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object ShareHelper {

    suspend fun sharePoster(
        context: Context,
        bitmap: Bitmap,
        fileName: String,
        fallbackText: String = "ARMO NOTES Match Poster"
    ) = withContext(Dispatchers.IO) {
        try {
            val cacheDir = File(context.cacheDir, "posters").apply {
                if (!exists()) mkdirs()
            }
            val imageFile = File(cacheDir, "$fileName.png")

            FileOutputStream(imageFile).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                imageFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, fallbackText)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "اشتراک پوستر ARMO").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            withContext(Dispatchers.Main) {
                try {
                    context.startActivity(chooser)
                } catch (e: Exception) {
                    Toast.makeText(context, "برنامه‌ای برای اشتراک‌گذاری پیدا نشد", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "خطا در آماده‌سازی تصویر برای اشتراک", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
