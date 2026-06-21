package com.textlens.android.translation

import com.textlens.android.data.AndroidSettings
import com.textlens.android.data.displayName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.roundToInt

class OpenAiCompatibleTranslationClient : TranslationClient {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    override suspend fun translate(text: String, settings: AndroidSettings): TranslationOutput =
        withContext(Dispatchers.IO) {
            val providerSettings = settings.profileFor(settings.provider)
            if (providerSettings.apiKey.isBlank()) {
                throw IllegalStateException("${settings.provider.displayName} API key is missing.")
            }

            val requestBody = ChatRequest(
                model = providerSettings.model,
                messages = listOf(
                    ChatMessage(
                        role = "system",
                        content = "You are a precise translation engine. Translate user text into natural ${settings.targetLanguage}. Preserve meaning, names, numbers, paragraphs, and tone. Return only the translation.",
                    ),
                    ChatMessage(role = "user", content = text),
                ),
                temperature = 0.2,
                maxTokens = max(1200, text.length * 2),
            )
            val url = providerSettings.baseUrl.trimEnd('/') + "/chat/completions"
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer ${providerSettings.apiKey}")
                .header("Content-Type", "application/json")
                .post(json.encodeToString(ChatRequest.serializer(), requestBody).toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = try {
                client.newCall(request).execute()
            } catch (error: IOException) {
                throw IOException("TextLens could not reach the translation API.", error)
            }

            response.use {
                val body = it.body?.string().orEmpty()
                if (!it.isSuccessful) {
                    throw IOException("Translation API returned ${it.code}: ${body.ifBlank { it.message }}")
                }

                val decoded = json.decodeFromString(ChatResponse.serializer(), body)
                val translatedText = decoded.choices.firstOrNull()?.message?.content?.trim().orEmpty()
                if (translatedText.isBlank()) {
                    throw IOException("Translation API returned an empty response.")
                }

                TranslationOutput(
                    text = translatedText,
                    costToman = decoded.usage?.let { usage ->
                        max(1, ((usage.totalTokens ?: 0) * 0.35).roundToInt())
                    },
                    model = providerSettings.model,
                )
            }
        }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}

@Serializable
private data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double,
    @SerialName("max_tokens") val maxTokens: Int,
)

@Serializable
private data class ChatMessage(
    val role: String,
    val content: String,
)

@Serializable
private data class ChatResponse(
    val choices: List<Choice> = emptyList(),
    val usage: Usage? = null,
)

@Serializable
private data class Choice(
    val message: ResponseMessage? = null,
)

@Serializable
private data class ResponseMessage(
    val content: String? = null,
)

@Serializable
private data class Usage(
    @SerialName("total_tokens") val totalTokens: Int? = null,
)
