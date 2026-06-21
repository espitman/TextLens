package com.textlens.android.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MlKitOcrEngine : OcrEngine {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun recognizeText(bitmap: Bitmap): String {
        val ocrBitmap = bitmap.downscaledForOcr()
        val image = InputImage.fromBitmap(ocrBitmap, 0)
        val result = try {
            suspendCancellableCoroutine { continuation ->
                recognizer.process(image)
                    .addOnSuccessListener { continuation.resume(it) }
                    .addOnFailureListener { continuation.resumeWithException(it) }
            }
        } finally {
            if (ocrBitmap !== bitmap) {
                ocrBitmap.recycle()
            }
        }

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

    private fun Bitmap.downscaledForOcr(): Bitmap {
        val longestSide = maxOf(width, height)
        if (longestSide <= 2200) return this
        val scale = 2200f / longestSide.toFloat()
        val targetWidth = (width * scale).toInt().coerceAtLeast(1)
        val targetHeight = (height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(this, targetWidth, targetHeight, true)
    }
}
