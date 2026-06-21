package com.textlens.android.ocr

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MlKitOcrEngine : OcrEngine {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun recognizeText(bitmap: Bitmap): String {
        val result = process(bitmap).text

        val text = result.textBlocks
            .flatMap { block -> block.lines }
            .joinToString("\n") { line -> line.text.trim() }
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString("\n")

        if (text.isBlank()) {
            throw IllegalStateException("No text found in selected area.")
        }

        return text
    }

    override suspend fun recognizeTextBlocks(bitmap: Bitmap): List<OcrTextBlock> {
        val processed = process(bitmap)
        return processed.text.textBlocks.mapNotNull { block ->
            val box = block.boundingBox ?: return@mapNotNull null
            OcrTextBlock(
                text = block.text.trim(),
                boundingBox = box.mapFromScale(processed.scale),
            )
        }.filter { it.text.isNotBlank() && it.boundingBox.width() >= 8 && it.boundingBox.height() >= 8 }
    }

    override suspend fun recognizeTextLines(bitmap: Bitmap): List<OcrTextBlock> {
        val processed = process(bitmap)
        return processed.text.textBlocks
            .flatMap { block -> block.lines }
            .mapNotNull { line ->
                val box = line.boundingBox ?: return@mapNotNull null
                OcrTextBlock(
                    text = line.text.trim(),
                    boundingBox = box.mapFromScale(processed.scale),
                )
            }
            .filter { it.text.isNotBlank() && it.boundingBox.width() >= 8 && it.boundingBox.height() >= 8 }
    }

    private suspend fun process(bitmap: Bitmap): ProcessedText {
        val scaled = bitmap.downscaledForOcr()
        val image = InputImage.fromBitmap(scaled.bitmap, 0)
        val result = try {
            suspendCancellableCoroutine { continuation ->
                recognizer.process(image)
                    .addOnSuccessListener { continuation.resume(it) }
                    .addOnFailureListener { continuation.resumeWithException(it) }
            }
        } finally {
            if (scaled.bitmap !== bitmap) {
                scaled.bitmap.recycle()
            }
        }
        return ProcessedText(text = result, scale = scaled.scale)
    }

    private fun Bitmap.downscaledForOcr(): ScaledBitmap {
        val longestSide = maxOf(width, height)
        if (longestSide <= 2200) return ScaledBitmap(bitmap = this, scale = 1f)
        val scale = 2200f / longestSide.toFloat()
        val targetWidth = (width * scale).toInt().coerceAtLeast(1)
        val targetHeight = (height * scale).toInt().coerceAtLeast(1)
        return ScaledBitmap(
            bitmap = Bitmap.createScaledBitmap(this, targetWidth, targetHeight, true),
            scale = scale,
        )
    }
}

private fun Rect.mapFromScale(scale: Float): Rect =
    Rect(
        (left / scale).toInt(),
        (top / scale).toInt(),
        (right / scale).toInt(),
        (bottom / scale).toInt(),
    )

private data class ScaledBitmap(
    val bitmap: Bitmap,
    val scale: Float,
)

private data class ProcessedText(
    val text: com.google.mlkit.vision.text.Text,
    val scale: Float,
)
