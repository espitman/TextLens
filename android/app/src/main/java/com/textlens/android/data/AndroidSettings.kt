package com.textlens.android.data

data class AndroidSettings(
    val provider: TranslationProvider = TranslationProvider.OpenRouter,
    val openRouter: ProviderSettings = ProviderSettings.openRouterDefaults(),
    val liara: ProviderSettings = ProviderSettings.liaraDefaults(),
    val targetLanguage: String = "Persian",
) {
    fun profileFor(provider: TranslationProvider): ProviderSettings =
        when (provider) {
            TranslationProvider.OpenRouter -> openRouter
            TranslationProvider.Liara -> liara
        }
}

data class ProviderSettings(
    val apiKey: String = "",
    val baseUrl: String,
    val model: String,
) {
    companion object {
        fun openRouterDefaults() = ProviderSettings(
            baseUrl = "https://openrouter.ai/api/v1",
            model = "google/gemma-4-31b-it:free",
        )

        fun liaraDefaults() = ProviderSettings(
            baseUrl = "https://ai.liara.ir/api/6a0ccd2d298429714a4b3e25/v1",
            model = "openai/gpt-5-nano",
        )
    }
}

enum class TranslationProvider {
    OpenRouter,
    Liara,
}

val TranslationProvider.displayName: String
    get() = when (this) {
        TranslationProvider.OpenRouter -> "OpenRouter"
        TranslationProvider.Liara -> "Liara"
    }

val TranslationProvider.modelOptions: List<String>
    get() = when (this) {
        TranslationProvider.OpenRouter -> listOf(
            "google/gemma-4-31b-it:free",
            "openai/gpt-4.1-mini",
            "google/gemini-2.0-flash-lite-001",
            "anthropic/claude-3.5-sonnet",
        )
        TranslationProvider.Liara -> listOf(
            "openai/gpt-5-nano",
            "openai/gpt-4.1-mini",
            "google/gemma-3-27b-it",
            "google/gemini-2.0-flash-lite-001",
            "google/gemini-2.5-flash-lite",
            "google/gemini-3.1-flash-lite",
            "google/gemini-2.5-flash",
        )
    }
