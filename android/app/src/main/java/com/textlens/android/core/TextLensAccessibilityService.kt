package com.textlens.android.core

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.graphics.RectF
import android.os.Build
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RequiresApi
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class TextLensAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() {
        activeService = this
        super.onServiceConnected()
    }

    override fun onDestroy() {
        if (activeService === this) {
            activeService = null
        }
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    @RequiresApi(Build.VERSION_CODES.R)
    private suspend fun capture(area: ScreenArea): Bitmap =
        suspendCancellableCoroutine { continuation ->
            takeScreenshot(
                Display.DEFAULT_DISPLAY,
                mainExecutor,
                object : TakeScreenshotCallback {
                    override fun onSuccess(result: ScreenshotResult) {
                        try {
                            val hardwareBuffer = result.hardwareBuffer
                            val hardwareBitmap = Bitmap.wrapHardwareBuffer(hardwareBuffer, result.colorSpace)
                                ?: throw IllegalStateException("Accessibility screenshot returned an empty frame.")
                            val fullBitmap = hardwareBitmap.copy(Bitmap.Config.ARGB_8888, false)
                            hardwareBuffer.close()

                            val captureRect = RectF(
                                area.left.toFloat(),
                                area.top.toFloat(),
                                (area.left + area.width).toFloat(),
                                (area.top + area.height).toFloat(),
                            )
                            val left = captureRect.left.toInt().coerceIn(0, fullBitmap.width - 1)
                            val top = captureRect.top.toInt().coerceIn(0, fullBitmap.height - 1)
                            val width = area.width.coerceAtMost(fullBitmap.width - left)
                            val height = area.height.coerceAtMost(fullBitmap.height - top)
                            if (width <= 1 || height <= 1) {
                                throw IllegalArgumentException("Selected area is outside the accessibility screenshot.")
                            }
                            continuation.resume(Bitmap.createBitmap(fullBitmap, left, top, width, height))
                            fullBitmap.recycle()
                        } catch (error: Throwable) {
                            continuation.resumeWithException(error)
                        }
                    }

                    override fun onFailure(errorCode: Int) {
                        continuation.resumeWithException(
                            IllegalStateException("Accessibility screenshot failed with code $errorCode."),
                        )
                    }
                },
            )
        }

    companion object {
        @Volatile
        private var activeService: TextLensAccessibilityService? = null

        val isReady: Boolean
            get() = activeService != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

        suspend fun captureArea(area: ScreenArea): Bitmap {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                throw IllegalStateException("Accessibility screenshots require Android 11 or newer.")
            }
            val service = activeService ?: throw IllegalStateException("TextLens Accessibility permission is required.")
            return service.capture(area)
        }
    }
}
