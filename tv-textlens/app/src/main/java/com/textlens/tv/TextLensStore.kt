package com.textlens.tv

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class TextLensStore(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("textlens_tv", Context.MODE_PRIVATE)

    fun loadSettings(): OverlaySettings =
        OverlaySettings(
            fontSizeSp = prefs.getFloat(KEY_FONT_SIZE, 28f),
            textColor = prefs.getString(KEY_TEXT_COLOR, "#FFD61E").orEmpty().toColorIntOr(Color.rgb(255, 214, 30)),
            backgroundColor = prefs.getString(KEY_BACKGROUND_COLOR, "#000000").orEmpty().toColorIntOr(Color.BLACK),
            backgroundOpacity = prefs.getInt(KEY_BACKGROUND_OPACITY, 62).coerceIn(0, 100),
            horizontalPercent = prefs.getFloat(KEY_HORIZONTAL_PERCENT, 50f).coerceIn(0f, 100f),
            bottomMarginDp = prefs.getInt(KEY_BOTTOM_MARGIN, 36).coerceIn(0, 360),
            maxLines = prefs.getInt(KEY_MAX_LINES, 3).coerceIn(1, 6),
            debugEnabled = prefs.getBoolean(KEY_DEBUG_ENABLED, false),
        )

    fun saveSettings(settings: OverlaySettings) {
        prefs.edit()
            .putFloat(KEY_FONT_SIZE, settings.fontSizeSp.coerceIn(12f, 72f))
            .putString(KEY_TEXT_COLOR, settings.textColor.toHexColor())
            .putString(KEY_BACKGROUND_COLOR, settings.backgroundColor.toHexColor())
            .putInt(KEY_BACKGROUND_OPACITY, settings.backgroundOpacity.coerceIn(0, 100))
            .putFloat(KEY_HORIZONTAL_PERCENT, settings.horizontalPercent.coerceIn(0f, 100f))
            .putInt(KEY_BOTTOM_MARGIN, settings.bottomMarginDp.coerceIn(0, 360))
            .putInt(KEY_MAX_LINES, settings.maxLines.coerceIn(1, 6))
            .putBoolean(KEY_DEBUG_ENABLED, settings.debugEnabled)
            .apply()
    }

    fun loadSubtitleName(): String =
        prefs.getString(KEY_SUBTITLE_NAME, "").orEmpty()

    fun loadSubtitleContent(): String =
        prefs.getString(KEY_SUBTITLE_CONTENT, "").orEmpty()

    fun loadSubtitleContent(fileName: String): String {
        return try {
            val dir = File(context.filesDir, "subtitles")
            val file = File(dir, fileName)
            if (file.exists()) {
                file.readText()
            } else {
                if (fileName == loadSubtitleName()) {
                    loadSubtitleContent()
                } else {
                    ""
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun saveSubtitleFile(name: String, content: String) {
        try {
            val dir = File(context.filesDir, "subtitles")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            File(dir, name).writeText(content)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun deleteSubtitleFile(name: String) {
        try {
            val dir = File(context.filesDir, "subtitles")
            val file = File(dir, name)
            if (file.exists()) {
                file.delete()
            }
            val bindings = loadAllBindings().filter { it.fileName != name }
            saveAllBindings(bindings)

            if (name == loadSubtitleName()) {
                clearSubtitle()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadAllBindings(): List<SubtitleBinding> {
        val jsonStr = prefs.getString(KEY_ALL_BINDINGS, "[]") ?: "[]"
        val list = mutableListOf<SubtitleBinding>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(SubtitleBinding(
                    fileName = obj.getString("fileName"),
                    videoId = obj.optString("videoId", "").ifBlank {
                        extractYoutubeVideoId(obj.optString("fileName", "")).orEmpty()
                    },
                    mediaTitle = obj.getString("mediaTitle"),
                    mediaDurationMs = obj.optLong("mediaDurationMs", 0L),
                    subtitleDurationMs = obj.optLong("subtitleDurationMs", 0L),
                    createdAtMs = obj.optLong("createdAtMs", 0L)
                ))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun saveAllBindings(bindings: List<SubtitleBinding>) {
        val array = JSONArray()
        for (binding in bindings) {
            array.put(binding.toJson())
        }
        prefs.edit().putString(KEY_ALL_BINDINGS, array.toString()).apply()
    }

    fun addOrUpdateBinding(binding: SubtitleBinding) {
        val bindings = loadAllBindings().toMutableList()
        bindings.removeAll { it.fileName == binding.fileName }
        bindings.add(binding)
        saveAllBindings(bindings)
    }

    fun setActiveSubtitle(fileName: String) {
        val content = loadSubtitleContent(fileName)
        val binding = loadAllBindings().firstOrNull { it.fileName == fileName }
        saveSubtitle(fileName, content, binding)
    }

    fun saveSubtitle(name: String, content: String, binding: SubtitleBinding?) {
        prefs.edit()
            .putString(KEY_SUBTITLE_NAME, name)
            .putString(KEY_SUBTITLE_CONTENT, content)
            .putString(KEY_SUBTITLE_BINDING_VIDEO_ID, binding?.videoId.orEmpty())
            .putString(KEY_SUBTITLE_BINDING_TITLE, binding?.mediaTitle.orEmpty())
            .putLong(KEY_SUBTITLE_BINDING_MEDIA_DURATION, binding?.mediaDurationMs ?: 0L)
            .putLong(KEY_SUBTITLE_BINDING_SUBTITLE_DURATION, binding?.subtitleDurationMs ?: 0L)
            .putLong(KEY_SUBTITLE_BINDING_CREATED_AT, binding?.createdAtMs ?: 0L)
            .putLong(KEY_SUBTITLE_UPDATED_AT, System.currentTimeMillis())
            .apply()
    }

    fun clearSubtitle() {
        prefs.edit()
            .remove(KEY_SUBTITLE_NAME)
            .remove(KEY_SUBTITLE_CONTENT)
            .remove(KEY_SUBTITLE_BINDING_VIDEO_ID)
            .remove(KEY_SUBTITLE_BINDING_TITLE)
            .remove(KEY_SUBTITLE_BINDING_MEDIA_DURATION)
            .remove(KEY_SUBTITLE_BINDING_SUBTITLE_DURATION)
            .remove(KEY_SUBTITLE_BINDING_CREATED_AT)
            .putLong(KEY_SUBTITLE_UPDATED_AT, System.currentTimeMillis())
            .apply()
    }

    fun subtitleVersion(): Long =
        prefs.getLong(KEY_SUBTITLE_UPDATED_AT, 0L)

    fun loadSubtitleCues(): List<SubtitleCue> =
        parseSrt(loadSubtitleContent())

    fun loadSubtitleBinding(): SubtitleBinding? {
        val fileName = loadSubtitleName()
        val storedBinding = loadAllBindings().firstOrNull { it.fileName == fileName }
        val videoId = prefs.getString(KEY_SUBTITLE_BINDING_VIDEO_ID, "").orEmpty()
            .ifBlank { storedBinding?.videoId.orEmpty() }
            .ifBlank { extractYoutubeVideoId(fileName).orEmpty() }
        val title = prefs.getString(KEY_SUBTITLE_BINDING_TITLE, "").orEmpty()
        val mediaDurationMs = prefs.getLong(KEY_SUBTITLE_BINDING_MEDIA_DURATION, 0L)
        val subtitleDurationMs = prefs.getLong(KEY_SUBTITLE_BINDING_SUBTITLE_DURATION, 0L)
        val createdAtMs = prefs.getLong(KEY_SUBTITLE_BINDING_CREATED_AT, 0L)
        if (fileName.isBlank() || title.isBlank() || createdAtMs <= 0L) return null
        return SubtitleBinding(
            fileName = fileName,
            videoId = videoId,
            mediaTitle = title,
            mediaDurationMs = mediaDurationMs,
            subtitleDurationMs = subtitleDurationMs,
            createdAtMs = createdAtMs,
        )
    }

    fun isOverlayEnabled(): Boolean =
        prefs.getBoolean(KEY_OVERLAY_ENABLED, true)

    fun setOverlayEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_OVERLAY_ENABLED, enabled).apply()
    }

    fun stateJson(isOverlayVisible: Boolean, panelUrl: String): JSONObject {
        val settings = loadSettings()
        val subtitleContent = loadSubtitleContent()
        val cueCount = parseSrt(subtitleContent).size
        val bindingsArray = JSONArray()
        for (binding in loadAllBindings()) {
            bindingsArray.put(binding.toJson())
        }
        return JSONObject()
            .put("overlayVisible", isOverlayVisible)
            .put("overlayEnabled", isOverlayEnabled())
            .put("panelUrl", panelUrl)
            .put("subtitleName", loadSubtitleName())
            .put("cueCount", cueCount)
            .put("hasSubtitle", subtitleContent.isNotBlank() && cueCount > 0)
            .put("subtitleBinding", loadSubtitleBinding()?.toJson())
            .put("settings", settings.toJson())
            .put("storedBindings", bindingsArray)
    }

    companion object {
        private const val KEY_FONT_SIZE = "font_size_sp"
        private const val KEY_TEXT_COLOR = "text_color"
        private const val KEY_BACKGROUND_COLOR = "background_color"
        private const val KEY_BACKGROUND_OPACITY = "background_opacity"
        private const val KEY_HORIZONTAL_PERCENT = "horizontal_percent"
        private const val KEY_BOTTOM_MARGIN = "bottom_margin_dp"
        private const val KEY_MAX_LINES = "max_lines"
        private const val KEY_DEBUG_ENABLED = "debug_enabled"
        private const val KEY_SUBTITLE_NAME = "subtitle_name"
        private const val KEY_SUBTITLE_CONTENT = "subtitle_content"
        private const val KEY_SUBTITLE_UPDATED_AT = "subtitle_updated_at"
        private const val KEY_SUBTITLE_BINDING_VIDEO_ID = "subtitle_binding_video_id"
        private const val KEY_SUBTITLE_BINDING_TITLE = "subtitle_binding_title"
        private const val KEY_SUBTITLE_BINDING_MEDIA_DURATION = "subtitle_binding_media_duration"
        private const val KEY_SUBTITLE_BINDING_SUBTITLE_DURATION = "subtitle_binding_subtitle_duration"
        private const val KEY_SUBTITLE_BINDING_CREATED_AT = "subtitle_binding_created_at"
        private const val KEY_ALL_BINDINGS = "all_bindings"
        private const val KEY_OVERLAY_ENABLED = "overlay_enabled"
    }
}

fun SubtitleBinding.toJson(): JSONObject =
    JSONObject()
        .put("fileName", fileName)
        .put("videoId", videoId)
        .put("mediaTitle", mediaTitle)
        .put("mediaDurationMs", mediaDurationMs)
        .put("subtitleDurationMs", subtitleDurationMs)
        .put("createdAtMs", createdAtMs)

fun OverlaySettings.toJson(): JSONObject =
    JSONObject()
        .put("fontSizeSp", fontSizeSp)
        .put("textColor", textColor.toHexColor())
        .put("backgroundColor", backgroundColor.toHexColor())
        .put("backgroundOpacity", backgroundOpacity)
        .put("horizontalPercent", horizontalPercent)
        .put("bottomMarginDp", bottomMarginDp)
        .put("maxLines", maxLines)
        .put("debugEnabled", debugEnabled)

fun JSONObject.toOverlaySettings(current: OverlaySettings): OverlaySettings =
    OverlaySettings(
        fontSizeSp = optDouble("fontSizeSp", current.fontSizeSp.toDouble()).toFloat().coerceIn(12f, 72f),
        textColor = optString("textColor", current.textColor.toHexColor()).toColorIntOr(current.textColor),
        backgroundColor = optString("backgroundColor", current.backgroundColor.toHexColor()).toColorIntOr(current.backgroundColor),
        backgroundOpacity = optInt("backgroundOpacity", current.backgroundOpacity).coerceIn(0, 100),
        horizontalPercent = optDouble("horizontalPercent", current.horizontalPercent.toDouble()).toFloat().coerceIn(0f, 100f),
        bottomMarginDp = optInt("bottomMarginDp", current.bottomMarginDp).coerceIn(0, 360),
        maxLines = optInt("maxLines", current.maxLines).coerceIn(1, 6),
        debugEnabled = optBoolean("debugEnabled", current.debugEnabled),
    )
