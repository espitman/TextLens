package com.textlens.tv

import android.graphics.Color

data class SubtitleCue(
    val startMs: Long,
    val endMs: Long,
    val text: String,
)

data class SubtitleBinding(
    val fileName: String,
    val videoId: String = "",
    val mediaTitle: String,
    val mediaDurationMs: Long,
    val subtitleDurationMs: Long,
    val createdAtMs: Long,
)

data class OverlaySettings(
    val fontSizeSp: Float = 28f,
    val textColor: Int = Color.rgb(255, 214, 30),
    val backgroundColor: Int = Color.BLACK,
    val backgroundOpacity: Int = 62,
    val horizontalPercent: Float = 50f,
    val bottomMarginDp: Int = 36,
    val maxLines: Int = 3,
    val debugEnabled: Boolean = false,
)

fun parseSrt(content: String): List<SubtitleCue> =
    content
        .removePrefix("\uFEFF")
        .replace("\r\n", "\n")
        .replace("\r", "\n")
        .trim()
        .split(Regex("\\n\\s*\\n"))
        .mapNotNull { block -> block.toSubtitleCue() }
        .sortedBy { it.startMs }

private fun String.toSubtitleCue(): SubtitleCue? {
    val lines = trim().lines().map { it.trim() }.filter { it.isNotBlank() }
    if (lines.size < 3) return null

    val timeLineIndex = lines.indexOfFirst { "-->" in it }
    if (timeLineIndex < 0 || timeLineIndex + 1 >= lines.size) return null

    val parts = lines[timeLineIndex].split("-->")
    if (parts.size != 2) return null

    val start = parts[0].trim().toSrtTimeMs() ?: return null
    val end = parts[1].trim().substringBefore(" ").toSrtTimeMs() ?: return null
    val text = lines.drop(timeLineIndex + 1).joinToString("\n")
    if (text.isBlank()) return null

    return SubtitleCue(startMs = start, endMs = end, text = text)
}

private fun String.toSrtTimeMs(): Long? {
    val match = Regex("(\\d{2}):(\\d{2}):(\\d{2})[,.](\\d{3})").find(this) ?: return null
    val hours = match.groupValues[1].toLong()
    val minutes = match.groupValues[2].toLong()
    val seconds = match.groupValues[3].toLong()
    val millis = match.groupValues[4].toLong()
    return hours * 3_600_000 + minutes * 60_000 + seconds * 1_000 + millis
}

fun List<SubtitleCue>.activeCueAt(positionMs: Long): SubtitleCue? =
    firstOrNull { positionMs in it.startMs..it.endMs }

fun Int.toHexColor(): String = "#%06X".format(0xFFFFFF and this)

fun String.toColorIntOr(defaultColor: Int): Int =
    runCatching { Color.parseColor(this) }.getOrDefault(defaultColor)

fun extractYoutubeVideoId(value: String): String? {
    val regex = Regex("(?<![A-Za-z0-9_-])([A-Za-z0-9_-]{11})(?![A-Za-z0-9_-])")
    return regex.find(value)?.groupValues?.get(1)
}
