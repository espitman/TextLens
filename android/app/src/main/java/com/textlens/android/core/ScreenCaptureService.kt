package com.textlens.android.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

class ScreenCaptureService(private val context: Context) {
    private var projection: MediaProjection? = null
    private var projectionCallback: MediaProjection.Callback? = null
    private var reader: ImageReader? = null
    private var display: VirtualDisplay? = null
    private var displayMetrics: DisplayMetrics? = null

    val hasActiveProjection: Boolean
        get() = projection != null

    suspend fun capture(area: ScreenArea): Bitmap = withContext(Dispatchers.IO) {
        val grant = MediaProjectionSession.grant ?: throw IllegalStateException("Screen capture permission is required.")
        if (!area.isMeaningful) {
            throw IllegalArgumentException("Selected area is too small.")
        }

        ensureProjection(grant)
        val metrics = displayMetrics ?: throw IllegalStateException("Screen capture is not ready.")
        val activeReader = reader ?: throw IllegalStateException("Screen capture is not ready.")

        delay(220)
        val image = acquireLatestImage(activeReader) ?: throw IllegalStateException("Could not read a screen frame.")
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
    }

    fun close() {
        display?.release()
        display = null
        reader?.close()
        reader = null
        projectionCallback?.let { callback ->
            projection?.unregisterCallback(callback)
        }
        projectionCallback = null
        projection?.stop()
        projection = null
        displayMetrics = null
    }

    private fun ensureProjection(grant: ScreenCaptureGrant) {
        if (projection != null && reader != null && display != null && displayMetrics != null) return

        val metrics = displayMetrics()
        val activeReader = ImageReader.newInstance(
            metrics.widthPixels,
            metrics.heightPixels,
            PixelFormat.RGBA_8888,
            2,
        )
        val activeProjection = (context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager)
            .getMediaProjection(grant.resultCode, grant.data)
        val callback = object : MediaProjection.Callback() {
            override fun onStop() {
                display?.release()
                display = null
                reader?.close()
                reader = null
                projection = null
                projectionCallback = null
                displayMetrics = null
                MediaProjectionSession.clear()
            }
        }
        activeProjection.registerCallback(callback, Handler(Looper.getMainLooper()))
        val activeDisplay = activeProjection.createVirtualDisplay(
            "TextLensCapture",
            metrics.widthPixels,
            metrics.heightPixels,
            metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            activeReader.surface,
            null,
            null,
        )

        projection = activeProjection
        projectionCallback = callback
        reader = activeReader
        display = activeDisplay
        displayMetrics = metrics
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
