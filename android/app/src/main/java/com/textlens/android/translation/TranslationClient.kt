package com.textlens.android.translation

import com.textlens.android.data.AndroidSettings

interface TranslationClient {
    suspend fun translate(text: String, settings: AndroidSettings): TranslationOutput
}

data class TranslationOutput(
    val text: String,
    val costToman: Int?,
    val model: String,
)
