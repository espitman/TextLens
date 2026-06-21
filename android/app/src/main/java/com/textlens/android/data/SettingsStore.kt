package com.textlens.android.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.textLensDataStore by preferencesDataStore(name = "textlens_settings")

class SettingsStore(private val context: Context) {
    private val encryptedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "textlens_secure_settings",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    val settings: Flow<AndroidSettings> = context.textLensDataStore.data.map { prefs ->
        AndroidSettings(
            provider = TranslationProvider.valueOf(
                prefs[Keys.provider] ?: TranslationProvider.OpenRouter.name,
            ),
            openRouter = ProviderSettings(
                apiKey = apiKeyFor(TranslationProvider.OpenRouter),
                baseUrl = prefs[Keys.openRouterBaseUrl] ?: ProviderSettings.openRouterDefaults().baseUrl,
                model = prefs[Keys.openRouterModel] ?: ProviderSettings.openRouterDefaults().model,
            ),
            liara = ProviderSettings(
                apiKey = apiKeyFor(TranslationProvider.Liara),
                baseUrl = prefs[Keys.liaraBaseUrl] ?: ProviderSettings.liaraDefaults().baseUrl,
                model = prefs[Keys.liaraModel] ?: ProviderSettings.liaraDefaults().model,
            ),
            targetLanguage = prefs[Keys.targetLanguage] ?: "Persian",
        )
    }

    suspend fun save(settings: AndroidSettings) {
        encryptedPreferences.edit()
            .putString(Keys.secureApiKey(TranslationProvider.OpenRouter), settings.openRouter.apiKey)
            .putString(Keys.secureApiKey(TranslationProvider.Liara), settings.liara.apiKey)
            .apply()

        context.textLensDataStore.edit { prefs ->
            prefs[Keys.provider] = settings.provider.name
            prefs[Keys.openRouterBaseUrl] = settings.openRouter.baseUrl
            prefs[Keys.openRouterModel] = settings.openRouter.model
            prefs[Keys.liaraBaseUrl] = settings.liara.baseUrl
            prefs[Keys.liaraModel] = settings.liara.model
            prefs[Keys.targetLanguage] = settings.targetLanguage
        }
    }

    private fun apiKeyFor(provider: TranslationProvider): String =
        encryptedPreferences.getString(Keys.secureApiKey(provider), "").orEmpty()

    private object Keys {
        val provider = stringPreferencesKey("provider")
        val openRouterBaseUrl = stringPreferencesKey("openrouter_base_url")
        val openRouterModel = stringPreferencesKey("openrouter_model")
        val liaraBaseUrl = stringPreferencesKey("liara_base_url")
        val liaraModel = stringPreferencesKey("liara_model")
        val targetLanguage = stringPreferencesKey("target_language")

        fun secureApiKey(provider: TranslationProvider): String =
            "${provider.name.lowercase()}_api_key"
    }
}
