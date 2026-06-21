package com.textlens.android.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

class ScreenCaptureService(private val context: Context) {
    suspend fun capture(area: ScreenArea): Bitmap = withContext(Dispatchers.IO) {
        val grant = MediaProjectionSession.grant ?: throw IllegalStateException("Screen capture permission is required.")
        if (!area.isMeaningful) {
            throw IllegalArgumentException("Selected area is too small.")
        }

        val metrics = displayMetrics()
        val reader = ImageReader.newInstance(
            metrics.widthPixels,
            metrics.heightPixels,
            PixelFormat.RGBA_8888,
            2,
        )
        val projection = (context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager)
            .getMediaProjection(grant.resultCode, grant.data)
        val display = projection.createVirtualDisplay(
            "TextLensCapture",
            metrics.widthPixels,
            metrics.heightPixels,
            metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.surface,
            null,
            null,
        )

        try {
            delay(220)
            val image = acquireLatestImage(reader) ?: throw IllegalStateException("Could not read a screen frame.")
            image.use {
                val fullBitmap = it.toBitmap(metrics.widthPixels, metrics.heightPixels)
                try {
                    val left = area.left.coerceIn(0, fullBitmap.width - 1)
                    val top = area.top.coerceIn(0, fullBitmap.height - 1)
                    val width = min(area.width, fullBitmap.width - left)
                    val height = min(area.height, fullBitmap.height - top)
                    if (width <= 1 || height <= 1) {
                        throw IllegalArgumentException("Selected area is outside the captured screen.")
                    }
                    Bitmap.createBitmap(fullBitmap, left, top, width, height)
                } finally {
                    fullBitmap.recycle()
                }
            }
        } finally {
            display.release()
            reader.close()
            projection.stop()
            MediaProjectionSession.clear()
        }
    }

    private suspend fun acquireLatestImage(reader: ImageReader): Image? {
        repeat(12) {
            reader.acquireLatestImage()?.let { return it }
            delay(80)
        }
        return null
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

private fun Image.toBitmap(width: Int, height: Int): Bitmap {
    val plane = planes.first()
    val buffer = plane.buffer
    val pixelStride = plane.pixelStride
    val rowStride = plane.rowStride
    val rowPadding = rowStride - pixelStride * width
    val paddedWidth = width + rowPadding / pixelStride
    val paddedBitmap = Bitmap.createBitmap(paddedWidth, height, Bitmap.Config.ARGB_8888)
    paddedBitmap.copyPixelsFromBuffer(buffer)
    return if (paddedWidth == width) {
        paddedBitmap
    } else {
        val cropped = Bitmap.createBitmap(paddedBitmap, 0, 0, width, height)
        paddedBitmap.recycle()
        cropped
    }
}
