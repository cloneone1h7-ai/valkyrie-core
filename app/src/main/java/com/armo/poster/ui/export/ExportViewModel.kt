package com.armo.poster.ui.export

import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.armo.poster.domain.model.DerivedValues
import com.armo.poster.domain.model.MatchData
import com.armo.poster.export.ExportHelper
import com.armo.poster.export.ShareHelper
import com.armo.poster.renderer.PosterRenderer
import com.armo.poster.renderer.RenderResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ExportViewModel(
    private val posterRenderer: PosterRenderer
) : ViewModel() {

    private val _isRendering = MutableStateFlow(false)
    val isRendering: StateFlow<Boolean> = _isRendering.asStateFlow()

    private val _renderResult = MutableStateFlow<RenderResult?>(null)
    val renderResult: StateFlow<RenderResult?> = _renderResult.asStateFlow()

    private val _scale = MutableStateFlow(2) // 2x for 2400x2400, 1x for 1200x1200
    val scale: StateFlow<Int> = _scale.asStateFlow()

    fun setScale(newScale: Int) {
        _scale.value = newScale
    }

    fun renderPoster(data: MatchData) {
        viewModelScope.launch {
            _isRendering.value = true
            val result = posterRenderer.render(data, _scale.value)
            _renderResult.value = result
            _isRendering.value = false
        }
    }

    fun saveToGallery(context: Context, data: MatchData) {
        val currentBitmap = (renderResult.value as? RenderResult.Success)?.bitmap ?: return
        viewModelScope.launch {
            val headline = DerivedValues.formatHeadline(data.home.abbr, data.away.abbr).replace(" ", "-")
            val fileName = "ARMO-NOTES-$headline-${data.gwLabel}-${System.currentTimeMillis()}"
            val uri = ExportHelper.saveToGallery(context, currentBitmap, fileName)
            if (uri != null) {
                Toast.makeText(context, "در گالری ذخیره شد (Pictures/ARMO NOTES)", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "خطا در ذخیره تصویر", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun shareToX(context: Context, data: MatchData) {
        val currentBitmap = (renderResult.value as? RenderResult.Success)?.bitmap ?: return
        viewModelScope.launch {
            val headline = DerivedValues.formatHeadline(data.home.abbr, data.away.abbr)
            val fileName = "ARMO-NOTES-${data.gwLabel}"
            ShareHelper.sharePoster(
                context = context,
                bitmap = currentBitmap,
                fileName = fileName,
                fallbackText = "ARMO NOTES · ${data.gwLabel} · $headline #FPL #ARMO"
            )
        }
    }
}
