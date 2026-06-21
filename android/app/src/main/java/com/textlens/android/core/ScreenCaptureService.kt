package com.textlens.android.core

import android.content.Context
import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ScreenCaptureService(@Suppress("UNUSED_PARAMETER") private val context: Context) {
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
}
