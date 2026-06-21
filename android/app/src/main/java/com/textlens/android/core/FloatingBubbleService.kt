package com.textlens.android.core

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.textlens.android.MainActivity
import com.textlens.android.R
import com.textlens.android.data.SettingsStore
import com.textlens.android.data.modelOptions
import com.textlens.android.ocr.MlKitOcrEngine
import com.textlens.android.translation.OpenAiCompatibleTranslationClient
import com.textlens.android.translation.TranslationOutput
import com.textlens.android.ui.SelectionOverlayView
import com.textlens.android.ui.TranslationPopupView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FloatingBubbleService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var windowManager: WindowManager
    private var bubbleView: View? = null
    private var selectionView: SelectionOverlayView? = null
    private var popupView: TranslationPopupView? = null
    private var activeJob: Job? = null
    private val settingsStore by lazy { SettingsStore(applicationContext) }
    private val captureService by lazy { ScreenCaptureService(applicationContext) }
    private val ocrEngine by lazy { MlKitOcrEngine() }
    private val translationClient by lazy { OpenAiCompatibleTranslationClient() }
    private var lastArea: ScreenArea? = null
    private var lastTranslation: TranslationOutput? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        startBubbleForeground()
        showBubble()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (bubbleView == null) {
            showBubble()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        activeJob?.cancel()
        removeBubble()
        removeSelection()
        removePopup()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun showBubble() {
        if (!Settings.canDrawOverlays(this) || bubbleView != null) return

        val bubble = TextView(this).apply {
            text = "T"
            textSize = 21f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(245, 200, 74))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setBackgroundResource(R.drawable.bubble_background)
        }
        val params = overlayParams(width = dp(46), height = dp(46)).apply {
            gravity = Gravity.TOP or Gravity.START
            x = preferences().getInt("bubble_x", dp(22))
            y = preferences().getInt("bubble_y", dp(160))
        }
        installBubbleTouch(bubble, params)
        windowManager.addView(bubble, params)
        bubbleView = bubble
    }

    private fun installBubbleTouch(view: View, params: WindowManager.LayoutParams) {
        var startX = 0
        var startY = 0
        var downRawX = 0f
        var downRawY = 0f
        var downTime = 0L

        view.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x
                    startY = params.y
                    downRawX = event.rawX
                    downRawY = event.rawY
                    downTime = System.currentTimeMillis()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = startX + (event.rawX - downRawX).toInt()
                    params.y = startY + (event.rawY - downRawY).toInt()
                    windowManager.updateViewLayout(view, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val moved = kotlin.math.abs(event.rawX - downRawX) > 12 || kotlin.math.abs(event.rawY - downRawY) > 12
                    val longPress = System.currentTimeMillis() - downTime > 520
                    if (longPress) {
                        openSettings()
                    } else if (!moved) {
                        handleBubbleTap()
                    } else {
                        snapBubble(view, params)
                    }
                    true
                }
                else -> true
            }
        }
    }

    private fun handleBubbleTap() {
        if (popupView != null) {
            activeJob?.cancel()
            activeJob = null
            removePopup()
            return
        }

        startSelection()
    }

    private fun snapBubble(view: View, params: WindowManager.LayoutParams) {
        val width = resources.displayMetrics.widthPixels
        params.x = if (params.x < width / 2) dp(12) else width - dp(58)
        preferences().edit()
            .putInt("bubble_x", params.x)
            .putInt("bubble_y", params.y)
            .apply()
        windowManager.updateViewLayout(view, params)
    }

    private fun startSelection() {
        if (!captureService.canCapture) {
            showErrorPopup("Accessibility permission is required. Long press the bubble, open TextLens, then enable Accessibility in Permission Flow.")
            return
        }
        activeJob?.cancel()
        activeJob = null
        removePopup()
        bubbleView?.visibility = View.GONE
        removeSelection()
        val overlay = SelectionOverlayView(this) { area ->
            removeSelection()
            bubbleView?.visibility = View.VISIBLE
            if (area == null) {
                return@SelectionOverlayView
            } else {
                lastArea = area
                runTranslation(area)
            }
        }
        windowManager.addView(
            overlay,
            overlayParams(width = WindowManager.LayoutParams.MATCH_PARENT, height = WindowManager.LayoutParams.MATCH_PARENT).apply {
                gravity = Gravity.TOP or Gravity.START
            },
        )
        selectionView = overlay
    }

    private fun runTranslation(area: ScreenArea) {
        activeJob?.cancel()
        activeJob = scope.launch {
            var popup: TranslationPopupView? = null
            try {
                val bitmap = captureService.capture(area)
                popup = showLoadingPopup()
                val recognizedText = try {
                    ocrEngine.recognizeText(bitmap)
                } finally {
                    bitmap.recycle()
                }
                val settings = settingsStore.settings.first()
                val output = translationClient.translate(recognizedText, settings)
                lastTranslation = output
                popup?.state = TranslationPopupView.State.Result(
                    text = output.text,
                    model = output.model,
                    costToman = output.costToman,
                )
            } catch (error: Throwable) {
                val visiblePopup = popup ?: showLoadingPopup()
                visiblePopup.state = TranslationPopupView.State.Error(error.message ?: "TextLens could not translate the selected area.")
            }
        }
    }

    private fun showLoadingPopup(): TranslationPopupView {
        removePopup()
        val popup = TranslationPopupView(this).apply {
            state = TranslationPopupView.State.Loading
            onClose = {
                activeJob?.cancel()
                activeJob = null
                removePopup()
            }
            onRetry = {
                lastArea?.let(::runTranslation)
            }
            onSwitchModel = {
                scope.launch {
                    switchToNextModel()
                }
            }
            onCopy = {
                lastTranslation?.let { copyText(it.text) }
            }
        }
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
        val popupWidth = minOf(dp(440), screenWidth - dp(40))
        val popupHeight = minOf(dp(320), screenHeight - dp(96))
        val params = overlayParams(width = popupWidth, height = popupHeight).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (screenWidth - popupWidth - dp(20)).coerceIn(dp(12), (screenWidth - popupWidth - dp(12)).coerceAtLeast(dp(12)))
            y = dp(96).coerceIn(dp(12), (screenHeight - popupHeight - dp(12)).coerceAtLeast(dp(12)))
        }
        popup.onDragBy = { dx, dy ->
            params.x = (params.x + dx).coerceIn(dp(8), (screenWidth - popupWidth - dp(8)).coerceAtLeast(dp(8)))
            params.y = (params.y + dy).coerceIn(dp(8), (screenHeight - popupHeight - dp(8)).coerceAtLeast(dp(8)))
            windowManager.updateViewLayout(popup, params)
        }
        windowManager.addView(popup, params)
        popupView = popup
        return popup
    }

    private fun showErrorPopup(message: String) {
        val popup = showLoadingPopup()
        popup.state = TranslationPopupView.State.Error(message)
    }

    private fun copyText(text: String) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("TextLens translation", text))
    }

    private suspend fun switchToNextModel() {
        val settings = settingsStore.settings.first()
        val provider = settings.provider
        val options = provider.modelOptions
        if (options.isEmpty()) return
        val current = settings.profileFor(provider).model
        val currentIndex = options.indexOf(current).takeIf { it >= 0 } ?: -1
        val next = options[(currentIndex + 1).floorMod(options.size)]
        val updated = when (provider) {
            com.textlens.android.data.TranslationProvider.OpenRouter -> settings.copy(
                openRouter = settings.openRouter.copy(model = next),
            )
            com.textlens.android.data.TranslationProvider.Liara -> settings.copy(
                liara = settings.liara.copy(model = next),
            )
        }
        settingsStore.save(updated)
        popupView?.state = TranslationPopupView.State.Error("Model switched to $next. Press Retry.")
    }

    private fun openSettings() {
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
    }

    private fun removeBubble() {
        bubbleView?.let { runCatching { windowManager.removeView(it) } }
        bubbleView = null
    }

    private fun removeSelection() {
        selectionView?.let { runCatching { windowManager.removeView(it) } }
        selectionView = null
    }

    private fun removePopup() {
        popupView?.let { runCatching { windowManager.removeView(it) } }
        popupView = null
    }

    private fun overlayParams(width: Int, height: Int): WindowManager.LayoutParams =
        WindowManager.LayoutParams(
            width,
            height,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        )

    private fun startBubbleForeground() {
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification("Floating bubble is active"),
            foregroundType(ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE),
        )
    }

    private fun foregroundType(type: Int): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) type else 0

    private fun notification(content: String): android.app.Notification {
        val channelId = "textlens_bubble"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(channelId, "TextLens Bubble", NotificationManager.IMPORTANCE_LOW),
            )
        }
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, FloatingBubbleService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_textlens)
            .setContentTitle("TextLens")
            .setContentText(content)
            .setContentIntent(openIntent)
            .addAction(0, "Stop", stopIntent)
            .setOngoing(true)
            .build()
    }

    private fun preferences() =
        getSharedPreferences("textlens_bubble", MODE_PRIVATE)

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun Int.floorMod(other: Int): Int =
        ((this % other) + other) % other

    private companion object {
        const val NOTIFICATION_ID = 3108
        const val ACTION_STOP = "com.textlens.android.STOP_BUBBLE"
    }
}
