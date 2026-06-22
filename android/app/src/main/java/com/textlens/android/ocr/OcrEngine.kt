package com.textlens.android.ocr

import android.graphics.Bitmap

interface OcrEngine {
    suspend fun recognizeText(bitmap: Bitmap): String
}
