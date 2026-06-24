package com.textlens.tv

import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class TextLensTvService : Service(), TextLensWebServer.Callbacks {
    private lateinit var store: TextLensStore
    private lateinit var webServer: TextLensWebServer
    private lateinit var windowManager: WindowManager
    private lateinit var mediaSessionManager: MediaSessionManager

    private var overlayRoot: FrameLayout? = null
    private var subtitleContainer: LinearLayout? = null
    private var subtitleTextView: TextView? = null
    private var debugOverlayView: TextView? = null
    private var overlayVisible = false
    private var wasAutoShown = false
    private var cachedSubtitleVersion = Long.MIN_VALUE
    private var subtitleCues: List<SubtitleCue> = emptyList()
    private var subtitleBinding: SubtitleBinding? = null
    private var lastSettings: OverlaySettings? = null
    private val handler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            refreshOverlay()
            handler.postDelayed(this, 250)
        }
    }

    override fun onCreate() {
        super.onCreate()
        store = TextLensStore(this)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        mediaSessionManager = getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
        webServer = TextLensWebServer(this, store, this)
        webServer.start()
        YoutubeNotificationListener.requestReconnect(this)
        if (store.isOverlayEnabled()) {
            showOverlayOnMain()
        }
        handler.post(refreshRunnable)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_HIDE_OVERLAY -> hideOverlay()
            ACTION_SHOW_OVERLAY -> showOverlay()
            ACTION_STOP -> {
                hideOverlay()
                stopSelf()
                return START_NOT_STICKY
            }
            else -> webServer.start()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(refreshRunnable)
        hideOverlay()
        removeDebugOverlay()
        webServer.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun isOverlayVisible(): Boolean = overlayVisible

    override fun showOverlay() = runOnMainSync {
        store.setOverlayEnabled(true)
        showOverlayOnMain()
    }

    private fun showOverlayOnMain() {
        if (!Settings.canDrawOverlays(this)) {
            throw IllegalStateException("Overlay permission is off on the TV.")
        }

        hideOverlayOnMain()

        val subtitleView = TextView(this).apply {
            text = ""
            gravity = Gravity.CENTER
            textDirection = View.TEXT_DIRECTION_RTL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            includeFontPadding = false
            typeface = Typeface.createFromAsset(assets, "fonts/Vazirmatn.ttf")
            setShadowLayer(5f, 0f, 2f, Color.BLACK)
            setLineSpacing(0f, 1.08f)
        }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            addView(subtitleView)
            
            addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                if (visibility == View.VISIBLE && width > 0 && height > 0) {
                    val currentSettings = store.loadSettings()
                    val screenWidth = resources.displayMetrics.widthPixels
                    val targetX = (screenWidth * (currentSettings.horizontalPercent / 100f)) - (width / 2f)
                    val targetY = (resources.displayMetrics.heightPixels - height - dp(currentSettings.bottomMarginDp))
                        .coerceAtLeast(0)
                        .toFloat()
                    
                    if (x != targetX || y != targetY) {
                        x = targetX.coerceIn(0f, (screenWidth - width).coerceAtLeast(0).toFloat())
                        y = targetY
                    }
                }
            }
        }
        val root = FrameLayout(this).apply {
            addView(
                container,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL,
                ),
            )
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        windowManager.addView(root, params)
        overlayRoot = root
        subtitleContainer = container
        subtitleTextView = subtitleView
        overlayVisible = true
        wasAutoShown = false
        lastSettings = null
        refreshOverlay()
    }

    override fun hideOverlay() = runOnMainSync {
        store.setOverlayEnabled(false)
        hideOverlayOnMain()
    }

    private fun hideOverlayOnMain() {
        overlayRoot?.let { view -> runCatching { windowManager.removeView(view) } }
        overlayRoot = null
        subtitleContainer = null
        subtitleTextView = null
        overlayVisible = false
        wasAutoShown = false
    }

    override fun notifySettingsChanged() {
        runOnMainSync {
            cachedSubtitleVersion = Long.MIN_VALUE
            lastSettings = null
            refreshOverlay()
        }
    }

    override fun panelUrl(): String = TextLensWebServer.panelUrl()

    override fun currentSubtitleBinding(fileName: String, subtitleDurationMs: Long): SubtitleBinding? = runOnMainSync {
        val controller = activeYoutubeController()
        val title = controller?.mediaTitle().orEmpty()
        if (controller == null || title.isBlank()) {
            null
        } else {
            SubtitleBinding(
                fileName = fileName,
                mediaTitle = title,
                mediaDurationMs = controller.mediaDurationMs(),
                subtitleDurationMs = subtitleDurationMs,
                createdAtMs = System.currentTimeMillis(),
            )
        }
    }

    private fun <T> runOnMainSync(block: () -> T): T {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            return block()
        }

        val latch = CountDownLatch(1)
        var completed = false
        var value: Any? = null
        var failure: Throwable? = null
        handler.post {
            try {
                value = block()
            } catch (error: Throwable) {
                failure = error
            } finally {
                completed = true
                latch.countDown()
            }
        }

        if (!latch.await(5, TimeUnit.SECONDS)) {
            throw IllegalStateException("Main thread did not respond.")
        }

        failure?.let { throw it }
        if (!completed) {
            throw IllegalStateException("Main thread returned no result.")
        }

        @Suppress("UNCHECKED_CAST")
        return value as T
    }

    private fun refreshOverlay() {
        val overlayEnabled = store.isOverlayEnabled()
        if (!overlayEnabled) {
            if (overlayVisible) {
                hideOverlayOnMain()
            }
            removeDebugOverlay()
            return
        }

        val settings = store.loadSettings()
        if (settings.debugEnabled) {
            ensureDebugOverlay()
        } else {
            removeDebugOverlay()
        }

        val controller = activeYoutubeController()
        if (controller == null) {
            debugOverlayView?.text = mediaSessionStatusText()
        }

        if (controller != null) {
            val playingTitle = controller.mediaTitle()
            if (playingTitle.isNotBlank()) {
                val allBindings = store.loadAllBindings()
                val matched = allBindings
                    .filter { it.mediaTitle.looksLikeSameVideoAs(playingTitle) }
                    .sortedWith(
                        compareByDescending<SubtitleBinding> { 
                            val nameLower = it.fileName.lowercase(Locale.US)
                            nameLower.contains("fa") || nameLower.contains("persian") || nameLower.contains("farsi")
                        }.thenBy { 
                            val nameLower = it.fileName.lowercase(Locale.US)
                            if (nameLower.contains("english") || nameLower.contains("auto-generated")) 1 else 0
                        }
                    )
                    .firstOrNull()
                if (matched != null) {
                    val currentActiveName = store.loadSubtitleName()
                    val currentBinding = store.loadSubtitleBinding()
                    val playingDuration = controller.mediaDurationMs()
                    
                    val titleChanged = matched.mediaTitle != playingTitle
                    val durationChanged = matched.mediaDurationMs == 0L && playingDuration > 0L
                    
                    val bindingToUse = if (titleChanged || durationChanged) {
                        val updated = SubtitleBinding(
                            fileName = matched.fileName,
                            mediaTitle = playingTitle,
                            mediaDurationMs = if (playingDuration > 0L) playingDuration else matched.mediaDurationMs,
                            subtitleDurationMs = matched.subtitleDurationMs,
                            createdAtMs = matched.createdAtMs
                        )
                        store.addOrUpdateBinding(updated)
                        updated
                    } else {
                        matched
                    }

                    if (currentActiveName != bindingToUse.fileName || currentBinding == null || currentBinding.mediaTitle != bindingToUse.mediaTitle || currentBinding.mediaDurationMs != bindingToUse.mediaDurationMs) {
                        store.setActiveSubtitle(bindingToUse.fileName)
                        cachedSubtitleVersion = Long.MIN_VALUE
                    }

                    if (!overlayVisible) {
                        showOverlayOnMain()
                        wasAutoShown = true
                    }
                } else {
                    if (overlayVisible && wasAutoShown) {
                        hideOverlayOnMain()
                        wasAutoShown = false
                    }
                }
            }
        } else {
            if (overlayVisible && wasAutoShown) {
                hideOverlayOnMain()
                wasAutoShown = false
            }
        }

        if (!overlayVisible || overlayRoot == null || subtitleTextView == null) {
            return
        }

        refreshSubtitleCacheIfNeeded()
        applySettings(settings)

        if (controller == null) {
            subtitleTextView?.text = ""
            subtitleContainer?.visibility = View.GONE
            return
        }

        val positionMs = controller.currentPositionMs()
        val suppressionReason = controller.subtitleSuppressionReason(positionMs, subtitleCues, subtitleBinding)
        if (suppressionReason != null) {
            subtitleTextView?.text = ""
            subtitleContainer?.visibility = View.GONE
            debugOverlayView?.text = controller.describe(positionMs, "subtitle: hidden ($suppressionReason)")
            return
        }

        val cue = subtitleCues.activeCueAt(positionMs)
        subtitleTextView?.text = cue?.text ?: ""
        subtitleContainer?.visibility = if (cue == null) View.GONE else View.VISIBLE
        debugOverlayView?.text = controller.describe(positionMs)
    }

    private fun refreshSubtitleCacheIfNeeded() {
        val version = store.subtitleVersion()
        if (version == cachedSubtitleVersion) return
        subtitleCues = store.loadSubtitleCues()
        subtitleBinding = store.loadSubtitleBinding()
        cachedSubtitleVersion = version
    }

    private fun applySettings(settings: OverlaySettings) {
        val root = overlayRoot ?: return
        val container = subtitleContainer ?: return
        val textView = subtitleTextView ?: return
        if (settings == lastSettings) return

        textView.textSize = settings.fontSizeSp
        textView.setTextColor(settings.textColor)
        textView.maxLines = settings.maxLines
        textView.setShadowLayer(5f, 0f, 2f, Color.BLACK)

        val alpha = (settings.backgroundOpacity.coerceIn(0, 100) * 255 / 100).coerceIn(0, 255)
        val background = Color.argb(
            alpha,
            Color.red(settings.backgroundColor),
            Color.green(settings.backgroundColor),
            Color.blue(settings.backgroundColor),
        )
        container.setBackgroundColor(background)
        container.setPadding(dp(18), dp(7), dp(18), dp(9))

        container.requestLayout()
        lastSettings = settings
    }

    private fun activeYoutubeController(): MediaController? {
        val component = ComponentName(this, YoutubeNotificationListener::class.java)
        val controllers = runCatching { mediaSessionManager.getActiveSessions(component) }
            .getOrElse { return null }

        return controllers
            .sortedByDescending { it.sessionScore() }
            .firstOrNull { it.packageName.contains("youtube", ignoreCase = true) }
            ?: controllers.maxByOrNull { it.sessionScore() }
    }

    private fun mediaSessionStatusText(): String {
        if (!isNotificationListenerEnabled()) {
            return "MediaSession: notification access is off"
        }

        val component = ComponentName(this, YoutubeNotificationListener::class.java)
        val result = runCatching { mediaSessionManager.getActiveSessions(component) }
        val controllers = result.getOrElse { error ->
            return "MediaSession: ${error.message ?: error.javaClass.simpleName}"
        }
        if (controllers.isEmpty()) {
            return "MediaSession: no active sessions"
        }

        val chosen = activeYoutubeController()
        val packages = controllers.sortedByDescending { it.sessionScore() }.joinToString("\n") { controller ->
            val state = controller.playbackState?.stateName() ?: "unknown"
            val duration = controller.metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION)
                ?.takeIf { it > 0 }
                .asClock()
            val marker = if (controller == chosen) "*" else "-"
            "$marker ${controller.packageName}: $state ${controller.currentPositionMs().asClock()} / $duration"
        }
        return "MediaSession: no YouTube session\n$packages"
    }

    private fun ensureDebugOverlay() {
        if (debugOverlayView != null) return

        val debugView = TextView(this).apply {
            text = "MediaSession: waiting..."
            textSize = 13f
            setTextColor(Color.WHITE)
            gravity = Gravity.START
            setPadding(dp(14), dp(9), dp(14), dp(9))
            setBackgroundColor(Color.argb(185, 0, 0, 0))
            setLineSpacing(0f, 1.08f)
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = dp(24)
            y = dp(36)
        }

        windowManager.addView(debugView, params)
        debugOverlayView = debugView
    }

    private fun removeDebugOverlay() {
        debugOverlayView?.let { view -> runCatching { windowManager.removeView(view) } }
        debugOverlayView = null
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun isNotificationListenerEnabled(): Boolean {
        val enabledListeners = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners",
        ) ?: return false
        val expected = "$packageName/${YoutubeNotificationListener::class.java.name}"
        return TextUtils.SimpleStringSplitter(':').also { splitter ->
            splitter.setString(enabledListeners)
            for (listener in splitter) {
                if (listener.equals(expected, ignoreCase = true)) return true
            }
        }.let { false }
    }

    companion object {
        const val ACTION_SHOW_OVERLAY = "com.textlens.tv.action.SHOW_OVERLAY"
        const val ACTION_HIDE_OVERLAY = "com.textlens.tv.action.HIDE_OVERLAY"
        const val ACTION_STOP = "com.textlens.tv.action.STOP"
    }
}

private fun MediaController.currentPositionMs(): Long {
    val state = playbackState ?: return 0L
    val basePosition = state.position.coerceAtLeast(0L)
    if (state.state != PlaybackState.STATE_PLAYING) return basePosition
    val elapsedSinceUpdate = (SystemClock.elapsedRealtime() - state.lastPositionUpdateTime).coerceAtLeast(0L)
    return basePosition + (elapsedSinceUpdate * state.playbackSpeed).toLong()
}

private fun MediaController.mediaTitle(): String =
    metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
        ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
        ?: ""

private fun MediaController.mediaDurationMs(): Long =
    metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION)?.takeIf { it > 0 } ?: 0L

private fun MediaController.sessionScore(): Int {
    val state = playbackState
    val metadata = metadata
    val duration = mediaDurationMs()
    val position = state?.position?.coerceAtLeast(0L) ?: 0L
    val title = mediaTitle()
    val stateScore = when (state?.state) {
        PlaybackState.STATE_PLAYING -> 800
        PlaybackState.STATE_BUFFERING,
        PlaybackState.STATE_CONNECTING -> 600
        PlaybackState.STATE_PAUSED -> 420
        PlaybackState.STATE_FAST_FORWARDING,
        PlaybackState.STATE_REWINDING -> 360
        else -> 0
    }
    val packageScore = if (packageName.contains("youtube", ignoreCase = true)) 1_000 else 0
    val durationScore = when {
        duration >= 10 * 60_000 -> 350
        duration >= 60_000 -> 220
        duration > 0 -> 25
        else -> 0
    }
    val positionScore = (position / 1_000).coerceIn(0, 240).toInt()
    val titleScore = if (title.isNotBlank()) 35 else 0
    return packageScore + stateScore + durationScore + positionScore + titleScore
}

private fun MediaController.subtitleSuppressionReason(
    positionMs: Long,
    cues: List<SubtitleCue>,
    binding: SubtitleBinding?,
): String? {
    val subtitleDurationMs = cues.lastOrNull()?.endMs ?: return null
    if (subtitleDurationMs < 5 * 60_000L) return null

    if (playbackState?.state != PlaybackState.STATE_PLAYING) {
        return "not playing"
    }

    if (binding == null) {
        return "subtitle not bound"
    }

    if (!matchesBinding(binding, positionMs)) {
        return "different video"
    }

    val title = mediaTitle()
    val normalizedTitle = title.lowercase(Locale.US)
    if ("advert" in normalizedTitle || "sponsored" in normalizedTitle) {
        return "ad metadata"
    }

    val durationMs = mediaDurationMs().takeIf { it > 0 } ?: return null
    val stillInsideReportedMedia = positionMs <= durationMs + 3_000L
    if (!stillInsideReportedMedia) return null

    if (durationMs <= 120_000L) {
        return "short media ${durationMs.asClock()}"
    }

    if (durationMs < (subtitleDurationMs * 0.35f).toLong()) {
        return "media shorter than subtitle"
    }

    return null
}

private fun MediaController.matchesBinding(binding: SubtitleBinding, positionMs: Long): Boolean {
    val currentTitle = mediaTitle()
    val currentDurationMs = mediaDurationMs()
    if (currentTitle.isBlank()) return false

    if (!currentTitle.looksLikeSameVideoAs(binding.mediaTitle)) return false

    if (binding.mediaDurationMs > 0 && currentDurationMs > 0) {
        val toleranceMs = maxOf(15_000L, (binding.mediaDurationMs * 0.06f).toLong())
        if (kotlin.math.abs(currentDurationMs - binding.mediaDurationMs) <= toleranceMs) {
            return true
        }

        val stillInsideReportedMedia = positionMs <= currentDurationMs + 3_000L
        if (currentDurationMs <= 120_000L && stillInsideReportedMedia) return false
    }

    if (binding.subtitleDurationMs > 0 && currentDurationMs > 0) {
        val toleranceMs = maxOf(20_000L, (binding.subtitleDurationMs * 0.08f).toLong())
        val closeToSubtitleDuration = kotlin.math.abs(currentDurationMs - binding.subtitleDurationMs) <= toleranceMs
        val clearlyShortMedia = currentDurationMs <= 120_000L && positionMs <= currentDurationMs + 3_000L
        return closeToSubtitleDuration || !clearlyShortMedia
    }

    return true
}

private fun MediaController.describe(positionMs: Long, extraLine: String? = null): String {
    val state = playbackState
    val metadata = metadata
    val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
        ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
        ?: "-"
    val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION)
        ?.takeIf { it > 0 }
    val speed = state?.playbackSpeed ?: 0f
    return listOfNotNull(
        "pkg: $packageName",
        "score: ${sessionScore()}",
        "state: ${state?.stateName() ?: "unknown"} speed: ${String.format(Locale.US, "%.2f", speed)}",
        "pos: ${positionMs.asClock()} / ${duration.asClock()}",
        "title: $title",
        extraLine,
    ).joinToString("\n")
}

private fun PlaybackState.stateName(): String =
    when (state) {
        PlaybackState.STATE_NONE -> "none"
        PlaybackState.STATE_STOPPED -> "stopped"
        PlaybackState.STATE_PAUSED -> "paused"
        PlaybackState.STATE_PLAYING -> "playing"
        PlaybackState.STATE_FAST_FORWARDING -> "fast_forwarding"
        PlaybackState.STATE_REWINDING -> "rewinding"
        PlaybackState.STATE_BUFFERING -> "buffering"
        PlaybackState.STATE_ERROR -> "error"
        PlaybackState.STATE_CONNECTING -> "connecting"
        PlaybackState.STATE_SKIPPING_TO_PREVIOUS -> "skip_previous"
        PlaybackState.STATE_SKIPPING_TO_NEXT -> "skip_next"
        PlaybackState.STATE_SKIPPING_TO_QUEUE_ITEM -> "skip_queue_item"
        else -> "unknown($state)"
    }

private fun String.looksLikeSameVideoAs(other: String): Boolean {
    val left = normalizedVideoTitle()
    val right = other.normalizedVideoTitle()
    if (left.isBlank() || right.isBlank()) return false
    if (left == right) return true
    if (left.length >= 12 && right.contains(left)) return true
    if (right.length >= 12 && left.contains(right)) return true

    val leftTokens = left.split(" ").filter { it.length >= 3 }.toSet()
    val rightTokens = right.split(" ").filter { it.length >= 3 }.toSet()
    if (leftTokens.isEmpty() || rightTokens.isEmpty()) return false

    val shared = leftTokens.intersect(rightTokens).size
    val required = (minOf(leftTokens.size, rightTokens.size) * 0.55f).toInt().coerceAtLeast(2)
    return shared >= required
}

private fun String.normalizedVideoTitle(): String =
    lowercase(Locale.US)
        .replace(Regex("\\[[^\\]]*]"), " ")
        .replace(Regex("\\([^)]*\\)"), " ")
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .trim()
        .replace(Regex("\\s+"), " ")

private fun Long?.asClock(): String {
    val value = this ?: return "-"
    val totalSeconds = value / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
