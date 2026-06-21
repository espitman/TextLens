package com.textlens.android.core

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

class ScreenCaptureService(private val context: Context) {
    val canCapture: Boolean
        get() = TextLensAccessibilityService.isReady

    suspend fun capture(area: ScreenArea): Bitmap = withContext(Dispatchers.IO) {
        if (!area.isMeaningful) {
            throw IllegalArgumentException("Selected area is too small.")
        }
        if (!TextLensAccessibilityService.isReady) {
            throw IllegalStateException("TextLens Accessibility permission is required.")
        }
        TextLensAccessibilityService.captureArea(area)
    }

    suspend fun captureScreen(): CapturedScreen = withContext(Dispatchers.IO) {
        if (!TextLensAccessibilityService.isReady) {
            throw IllegalStateException("TextLens Accessibility permission is required.")
        }
        val metrics = displayMetrics()
        val area = ScreenArea(
            left = 0,
            top = 0,
            width = metrics.widthPixels,
            height = metrics.heightPixels,
        )
        CapturedScreen(
            bitmap = TextLensAccessibilityService.captureArea(area),
            area = area,
        )
    }

    private fun displayMetrics(): DisplayMetrics {
        val metrics = DisplayMetrics()
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            metrics.widthPixels = max(1, bounds.width())
            metrics.heightPixels = max(1, bounds.height())
            metrics.densityDpi = context.resources.displayMetrics.densityDpi
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(metrics)
        }
        return metrics
    }
}

data class CapturedScreen(
    val bitmap: Bitmap,
    val area: ScreenArea,
)
