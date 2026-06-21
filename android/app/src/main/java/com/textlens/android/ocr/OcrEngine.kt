package com.textlens.android.ocr

import android.graphics.Bitmap
import android.graphics.Rect

interface OcrEngine {
    suspend fun recognizeText(bitmap: Bitmap): String
    suspend fun recognizeTextBlocks(bitmap: Bitmap): List<OcrTextBlock>
    suspend fun recognizeTextLines(bitmap: Bitmap): List<OcrTextBlock>
}

data class OcrTextBlock(
    val text: String,
    val boundingBox: Rect,
)
