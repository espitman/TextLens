package com.textlens.tv

import android.content.Context
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.cio.CIO
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.NetworkInterface
import java.net.URL

class TextLensWebServer(
    private val context: Context,
    private val store: TextLensStore,
    private val callbacks: Callbacks,
) {
    interface Callbacks {
        fun isOverlayVisible(): Boolean
        fun showOverlay()
        fun hideOverlay()
        fun notifySettingsChanged()
        fun panelUrl(): String
        fun currentSubtitleBinding(fileName: String, subtitleDurationMs: Long): SubtitleBinding?
    }

    private fun fetchYoutubeTitle(videoId: String): String? {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v=$videoId&format=json")
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.setRequestProperty("User-Agent", "Mozilla/5.0")

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()
                val json = JSONObject(response.toString())
                return json.optString("title")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            connection?.disconnect()
        }
        return null
    }

    private var engine: ApplicationEngine? = null

    fun start() {
        if (engine != null) return

        engine = embeddedServer(CIO, host = "0.0.0.0", port = PORT) {
            routing {
                get("/") {
                    call.respondText(readPanelHtml(), ContentType.Text.Html)
                }

                get("/api/state") {
                    call.respondSafely {
                        store.stateJson(callbacks.isOverlayVisible(), callbacks.panelUrl())
                    }
                }

                post("/api/subtitle") {
                    call.respondSafely {
                        val payload = JSONObject(call.receiveText())
                        val name = payload.optString("name", "subtitle.srt").ifBlank { "subtitle.srt" }
                        val content = payload.optString("content", "")
                        val clientYoutubeTitle = payload.optString("youtubeTitle", "")
                        val cues = parseSrt(content)
                        val cueCount = cues.size
                        if (cueCount == 0) {
                            throw BadRequestException("No valid SRT cues found.")
                        }

                        val subtitleDurationMs = cues.lastOrNull()?.endMs ?: 0L
                        val videoId = payload.optString("videoId", "")
                            .ifBlank { extractYoutubeVideoId(name).orEmpty() }
                        var binding: SubtitleBinding? = null

                        if (clientYoutubeTitle.isNotBlank()) {
                            binding = SubtitleBinding(
                                fileName = name,
                                videoId = videoId,
                                mediaTitle = clientYoutubeTitle,
                                mediaDurationMs = 0L,
                                subtitleDurationMs = subtitleDurationMs,
                                createdAtMs = System.currentTimeMillis()
                            )
                        } else if (videoId.isNotBlank()) {
                            val title = withContext(Dispatchers.IO) {
                                fetchYoutubeTitle(videoId)
                            }
                            val titleToUse = title ?: name
                                .substringBeforeLast(".")
                                .replace(Regex("[_-]fa$|[_-]fa[_-]", RegexOption.IGNORE_CASE), "")
                                .replace(Regex("[_-]"), " ")
                                .trim()

                            binding = SubtitleBinding(
                                fileName = name,
                                videoId = videoId,
                                mediaTitle = titleToUse,
                                mediaDurationMs = 0L,
                                subtitleDurationMs = subtitleDurationMs,
                                createdAtMs = System.currentTimeMillis()
                            )
                        } else {
                            binding = callbacks.currentSubtitleBinding(name, subtitleDurationMs)
                            if (binding == null) {
                                val cleanTitle = name
                                    .substringBeforeLast(".")
                                    .replace(Regex("[_-]fa$|[_-]fa[_-]", RegexOption.IGNORE_CASE), "")
                                    .replace(Regex("[_-]"), " ")
                                    .replace(Regex("\\[[^\\]]*]"), "")
                                    .replace(Regex("\\([^)]*\\)"), "")
                                    .trim()
                                    .replace(Regex("\\s+"), " ")

                                val titleToUse = if (cleanTitle.isBlank()) "Uploaded Subtitle" else cleanTitle
                                binding = SubtitleBinding(
                                    fileName = name,
                                    videoId = extractYoutubeVideoId(name).orEmpty(),
                                    mediaTitle = titleToUse,
                                    mediaDurationMs = 0L,
                                    subtitleDurationMs = subtitleDurationMs,
                                    createdAtMs = System.currentTimeMillis()
                                )
                            }
                        }

                        store.saveSubtitleFile(name, content)
                        store.addOrUpdateBinding(binding!!)
                        store.saveSubtitle(name, content, binding)
                        callbacks.notifySettingsChanged()

                        JSONObject()
                            .put("ok", true)
                            .put("name", name)
                            .put("cueCount", cueCount)
                            .put("binding", binding.toJson())
                    }
                }

                post("/api/subtitle/delete") {
                    call.respondSafely {
                        val payload = JSONObject(call.receiveText())
                        val name = payload.getString("name")
                        store.deleteSubtitleFile(name)
                        callbacks.notifySettingsChanged()
                        JSONObject().put("ok", true)
                    }
                }

                post("/api/subtitle/clear") {
                    call.respondSafely {
                        store.clearSubtitle()
                        callbacks.notifySettingsChanged()
                        JSONObject().put("ok", true)
                    }
                }

                post("/api/settings") {
                    call.respondSafely {
                        val payload = JSONObject(call.receiveText())
                        val next = payload.toOverlaySettings(store.loadSettings())
                        store.saveSettings(next)
                        callbacks.notifySettingsChanged()
                        JSONObject().put("ok", true).put("settings", next.toJson())
                    }
                }

                post("/api/overlay/show") {
                    call.respondSafely {
                        callbacks.showOverlay()
                        JSONObject().put("ok", true)
                    }
                }

                post("/api/overlay/hide") {
                    call.respondSafely {
                        callbacks.hideOverlay()
                        JSONObject().put("ok", true)
                    }
                }
            }
        }.start(wait = false)
    }

    fun stop() {
        engine?.stop(gracePeriodMillis = 300, timeoutMillis = 1_000)
        engine = null
    }

    private fun readPanelHtml(): String =
        context.assets.open("web/index.html").bufferedReader(Charsets.UTF_8).use { it.readText() }

    companion object {
        const val PORT = 5012

        fun localIpAddress(): String =
            NetworkInterface.getNetworkInterfaces().asSequence()
                .flatMap { it.inetAddresses.asSequence() }
                .firstOrNull { address ->
                    !address.isLoopbackAddress &&
                        address.hostAddress?.contains(":") == false &&
                        !address.hostAddress.orEmpty().startsWith("169.254.")
                }
                ?.hostAddress
                ?: "127.0.0.1"

        fun panelUrl(): String = "http://${localIpAddress()}:$PORT/"
    }
}

private class BadRequestException(message: String) : IllegalArgumentException(message)

private suspend fun io.ktor.server.application.ApplicationCall.respondJson(
    payload: JSONObject,
    status: HttpStatusCode = HttpStatusCode.OK,
) {
    respondText(payload.toString(), ContentType.Application.Json, status)
}

private suspend fun io.ktor.server.application.ApplicationCall.respondSafely(
    block: suspend () -> JSONObject,
) {
    runCatching { block() }
        .onSuccess { payload -> respondJson(payload) }
        .onFailure { error ->
            val status = if (error is BadRequestException) {
                HttpStatusCode.BadRequest
            } else {
                HttpStatusCode.InternalServerError
            }
            respondJson(
                JSONObject()
                    .put("ok", false)
                    .put("error", error.message ?: error.javaClass.simpleName),
                status,
            )
        }
}
