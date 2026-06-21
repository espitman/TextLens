package com.textlens.android.translation

import com.textlens.android.data.AndroidSettings
import com.textlens.android.data.displayName
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Dns
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.InetAddress
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.roundToInt

class OpenAiCompatibleTranslationClient : TranslationClient {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val client = OkHttpClient.Builder()
        .dns(TextLensDns)
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
                        content = "You are TextLens, a precise OCR translation engine. Translate the entire user text into natural ${settings.targetLanguage}. Do not summarize. Do not omit sentences. Preserve names, numbers, paragraphs, ordering, and tone. If the OCR text has broken lines, reconstruct the meaning naturally. Return only the full translation.",
                    ),
                    ChatMessage(role = "user", content = "Translate this complete OCR text:\n\n$text"),
                ),
                temperature = 0.0,
                maxTokens = max(2048, text.length * 4),
            )
            val url = providerSettings.baseUrl.trimEnd('/') + "/chat/completions"
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer ${providerSettings.apiKey}")
                .header("Content-Type", "application/json")
                .header("HTTP-Referer", "https://github.com/espitman/TextLens")
                .header("X-Title", "TextLens")
                .post(json.encodeToString(ChatRequest.serializer(), requestBody).toRequestBody(JSON_MEDIA_TYPE))
                .build()

            Log.i(
                TAG,
                "Sending request provider=${settings.provider.displayName}, baseUrl=${providerSettings.baseUrl}, model=${providerSettings.model}, keyLength=${providerSettings.apiKey.length}, textLength=${text.length}",
            )
            val response = executeWithRetry(request, url)

            response.use {
                val body = it.body?.string().orEmpty()
                if (!it.isSuccessful) {
                    Log.e(TAG, "Translation API returned ${it.code}: $body")
                    val message = when (it.code) {
                        401 -> "کلید API ${settings.provider.displayName} رد شد. کلید را Sync یا دوباره وارد کن."
                        429 -> "محدودیت ${settings.provider.displayName} پر شده. بعداً تست کن یا مدل را عوض کن."
                        else -> "API ترجمه خطای ${it.code} داد: ${body.ifBlank { it.message }}"
                    }
                    throw IOException(message)
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

    private fun executeWithRetry(request: Request, url: String): okhttp3.Response {
        var lastError: IOException? = null
        repeat(3) { attempt ->
            try {
                return client.newCall(request).execute()
            } catch (error: IOException) {
                lastError = error
                Log.e(TAG, "Translation API request failed attempt=${attempt + 1}: $url", error)
                if (attempt < 2) {
                    Thread.sleep(450L * (attempt + 1))
                }
            }
        }
        val error = lastError ?: IOException("Unknown network failure")
        val detail = error.localizedMessage ?: error.javaClass.simpleName
        throw IOException("ارتباط با API ترجمه برقرار نشد: $detail", error)
    }

    private companion object {
        const val TAG = "TextLensTranslation"
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}

private object TextLensDns : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        if (hostname.equals("openrouter.ai", ignoreCase = true)) {
            val pinned = listOf("104.18.2.115", "104.18.3.115").map(InetAddress::getByName)
            val system = runCatching { Dns.SYSTEM.lookup(hostname) }.getOrDefault(emptyList())
            return (pinned + system).distinctBy { it.hostAddress }
        }

        return Dns.SYSTEM.lookup(hostname)
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
