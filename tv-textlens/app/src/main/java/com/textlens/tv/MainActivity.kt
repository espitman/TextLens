package com.textlens.tv

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.BitmapFactory
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import android.util.StateSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.net.URL

class MainActivity : Activity() {
    private lateinit var urlText: TextView
    private var waitingForOverlayPermission = false
    private var waitingForNotificationPermission = false
    private lateinit var store: TextLensStore

    private var overlayBadge: View? = null
    private var overlayBadgeDot: View? = null
    private var overlayBadgeText: TextView? = null

    private var mediaBadge: View? = null
    private var mediaBadgeDot: View? = null
    private var mediaBadgeText: TextView? = null

    private var listContainer: LinearLayout? = null

    private var cachedOverlayEnabled: Boolean? = null
    private var cachedOverlayPermission: Boolean? = null
    private var cachedMediaPermission: Boolean? = null
    private var cachedBindingsList: List<SubtitleBinding>? = null

    private val uiRefreshHandler = Handler(Looper.getMainLooper())
    private val uiRefreshRunnable = object : Runnable {
        override fun run() {
            updateStatusBadges()
            updateStoredSubtitlesList()
            uiRefreshHandler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = TextLensStore(this)
        startService(Intent(this, TextLensTvService::class.java))
    }

    override fun onResume() {
        super.onResume()
        cachedOverlayEnabled = null
        cachedOverlayPermission = null
        cachedMediaPermission = null
        cachedBindingsList = null

        setContentView(makeContentView())
        updateStatusBadges()
        updateStoredSubtitlesList()

        if (isNotificationListenerEnabled()) {
            YoutubeNotificationListener.requestReconnect(this)
        }
        if (waitingForOverlayPermission && Settings.canDrawOverlays(this)) {
            waitingForOverlayPermission = false
            showOverlay()
        }
        if (waitingForNotificationPermission && isNotificationListenerEnabled()) {
            waitingForNotificationPermission = false
            showOverlay()
        }
        uiRefreshHandler.post(uiRefreshRunnable)
    }

    override fun onPause() {
        super.onPause()
        uiRefreshHandler.removeCallbacks(uiRefreshRunnable)
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun makeCardDrawable(bgColor: String, strokeColor: String, radiusDp: Float): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.parseColor(bgColor))
            cornerRadius = dp(radiusDp.toInt()).toFloat()
            setStroke(dp(1), Color.parseColor(strokeColor))
        }
    }

    private fun makeFocusableButtonDrawable(): StateListDrawable {
        val focused = GradientDrawable().apply {
            setColor(Color.parseColor("#f5c84a"))
            cornerRadius = dp(12).toFloat()
            setStroke(dp(2), Color.parseColor("#ffffff"))
        }
        val normal = GradientDrawable().apply {
            setColor(Color.parseColor("#1c1a14"))
            cornerRadius = dp(12).toFloat()
            setStroke(dp(1), Color.parseColor("#3a351b"))
        }
        return StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_focused), focused)
            addState(StateSet.WILD_CARD, normal)
        }
    }

    private fun loadQrCode(urlStr: String, imageView: ImageView) {
        Thread {
            try {
                val encodedUrl = Uri.encode(urlStr)
                val qrUrl = URL("https://api.qrserver.com/v1/create-qr-code/?size=220x220&color=f5c84a&bgcolor=12110c&data=$encodedUrl")
                val connection = qrUrl.openConnection()
                connection.connectTimeout = 3000
                connection.readTimeout = 3000
                val bitmap = BitmapFactory.decodeStream(connection.getInputStream())
                if (bitmap != null) {
                    runOnUiThread {
                        imageView.setImageBitmap(bitmap)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun makeContentView(): View {
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.TOP
            val bg = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.parseColor("#050505"), Color.parseColor("#15130b"))
            )
            background = bg
            setPadding(dp(32), dp(20), dp(32), dp(20))
        }

        // --- HEADER ROW ---
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        
        val logoText = TextView(this).apply {
            text = "TextLens TV"
            textSize = 28f
            setTextColor(Color.parseColor("#f5c84a"))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        headerRow.addView(logoText)

        val headerSpacer = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, 1).apply { weight = 1f }
        }
        headerRow.addView(headerSpacer)

        val badgesContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val overlayBadgeView = createStatusBadgeView()
        overlayBadge = overlayBadgeView
        overlayBadgeDot = (overlayBadgeView as LinearLayout).getChildAt(0)
        overlayBadgeText = overlayBadgeView.getChildAt(1) as TextView
        badgesContainer.addView(overlayBadgeView)

        val mediaBadgeView = createStatusBadgeView()
        mediaBadge = mediaBadgeView
        mediaBadgeDot = (mediaBadgeView as LinearLayout).getChildAt(0)
        mediaBadgeText = mediaBadgeView.getChildAt(1) as TextView
        val mediaBadgeParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            leftMargin = dp(12)
        }
        badgesContainer.addView(mediaBadgeView, mediaBadgeParams)

        headerRow.addView(badgesContainer)
        rootLayout.addView(headerRow, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        val headerContentSpacer = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(1, dp(20))
        }
        rootLayout.addView(headerContentSpacer)

        // --- TWO COLUMNS LAYOUT ---
        val columnsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.TOP
        }

        // --- LEFT COLUMN: Connection Info ---
        val leftColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.TOP
            background = makeCardDrawable("#12110c", "#2a2615", 20f)
            setPadding(dp(20), dp(16), dp(20), dp(16))
        }

        val leftTitle = TextView(this).apply {
            text = "Scan & Translate"
            textSize = 20f
            setTextColor(Color.parseColor("#f7f0dc"))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER_HORIZONTAL
        }
        leftColumn.addView(leftTitle)

        val qrContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(140)).apply {
                topMargin = dp(12)
                bottomMargin = dp(12)
            }
            layoutParams = params
        }

        val qrImageView = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(120), dp(120))
            background = makeCardDrawable("#1c1b14", "#3a351b", 12f)
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }
        qrContainer.addView(qrImageView)
        leftColumn.addView(qrContainer)

        val panelUrl = TextLensWebServer.panelUrl()
        loadQrCode(panelUrl, qrImageView)

        urlText = TextView(this).apply {
            text = panelUrl
            textSize = 17f
            setTextColor(Color.parseColor("#f5c84a"))
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = makeCardDrawable("#1c1b14", "#ffd21a", 12f)
        }
        leftColumn.addView(urlText, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dp(12)
        })

        val leftHint = TextView(this).apply {
            text = "Connect via your mobile device to translate in real-time."
            textSize = 13f
            setTextColor(Color.parseColor("#8c8775"))
            gravity = Gravity.CENTER
        }
        leftColumn.addView(leftHint)

        val actionButtonsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(12)
            }
            layoutParams = params
        }

        val row1 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        val showButton = TextView(this).apply {
            text = "Show Overlay"
            textSize = 13f
            gravity = Gravity.CENTER
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            background = makeFocusableButtonDrawable()
            setTextColor(Color.parseColor("#f7f0dc"))
            setPadding(dp(8), dp(10), dp(8), dp(10))
            isFocusable = true
            isClickable = true
            setOnFocusChangeListener { _, hasFocus ->
                setTextColor(if (hasFocus) Color.parseColor("#050505") else Color.parseColor("#f7f0dc"))
            }
            setOnClickListener { requestPermissionsThenShowOverlay() }
        }
        row1.addView(showButton, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            weight = 1f
        })

        val hideButton = TextView(this).apply {
            text = "Hide Overlay"
            textSize = 13f
            gravity = Gravity.CENTER
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            background = makeFocusableButtonDrawable()
            setTextColor(Color.parseColor("#f7f0dc"))
            setPadding(dp(8), dp(10), dp(8), dp(10))
            isFocusable = true
            isClickable = true
            setOnFocusChangeListener { _, hasFocus ->
                setTextColor(if (hasFocus) Color.parseColor("#050505") else Color.parseColor("#f7f0dc"))
            }
            setOnClickListener {
                startService(
                    Intent(this@MainActivity, TextLensTvService::class.java)
                        .setAction(TextLensTvService.ACTION_HIDE_OVERLAY),
                )
            }
        }
        row1.addView(hideButton, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            weight = 1f
            leftMargin = dp(8)
        })

        actionButtonsLayout.addView(row1, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        val mediaButton = TextView(this).apply {
            text = "Media Access Settings"
            textSize = 13f
            gravity = Gravity.CENTER
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            background = makeFocusableButtonDrawable()
            setTextColor(Color.parseColor("#f7f0dc"))
            setPadding(dp(12), dp(10), dp(12), dp(10))
            isFocusable = true
            isClickable = true
            setOnFocusChangeListener { _, hasFocus ->
                setTextColor(if (hasFocus) Color.parseColor("#050505") else Color.parseColor("#f7f0dc"))
            }
            setOnClickListener { openNotificationListenerSettings() }
        }
        actionButtonsLayout.addView(mediaButton, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(8)
        })

        leftColumn.addView(actionButtonsLayout)

        columnsLayout.addView(leftColumn, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT).apply {
            weight = 1f
        })

        val columnsSpacer = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(28), 1)
        }
        columnsLayout.addView(columnsSpacer)

        // --- RIGHT COLUMN: Stored Subtitles Directory ---
        val rightColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.TOP
            background = makeCardDrawable("#12110c", "#2a2615", 20f)
            setPadding(dp(24), dp(24), dp(24), dp(24))
        }

        val rightTitle = TextView(this).apply {
            text = "Stored Subtitles on TV"
            textSize = 20f
            setTextColor(Color.parseColor("#f7f0dc"))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER_HORIZONTAL
        }
        rightColumn.addView(rightTitle)

        val scrollView = ScrollView(this).apply {
            val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0).apply {
                weight = 1f
                topMargin = dp(16)
            }
            layoutParams = params
        }

        val listContainerView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.TOP
        }
        listContainer = listContainerView

        scrollView.addView(listContainerView)
        rightColumn.addView(scrollView)

        columnsLayout.addView(rightColumn, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT).apply {
            weight = 1f
        })

        rootLayout.addView(columnsLayout, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0).apply {
            weight = 1f
        })

        return rootLayout
    }

    private fun extractVideoId(name: String): String? {
        val regex = Regex("([a-zA-Z0-9_-]{11})")
        val match = regex.find(name)
        return match?.groupValues?.get(1)
    }

    private fun createStatusBadgeView(): View {
        val badge = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(5), dp(10), dp(5))
        }

        val dot = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(8), dp(8))
        }
        badge.addView(dot)

        val badgeText = TextView(this).apply {
            textSize = 11f
            setPadding(dp(6), 0, 0, 0)
        }
        badge.addView(badgeText)

        return badge
    }

    private fun updateStatusBadges() {
        val overlayGranted = Settings.canDrawOverlays(this)
        val overlayEnabled = store.isOverlayEnabled() && overlayGranted
        val mediaGranted = isNotificationListenerEnabled()

        if (overlayEnabled != cachedOverlayEnabled || overlayGranted != cachedOverlayPermission) {
            cachedOverlayEnabled = overlayEnabled
            cachedOverlayPermission = overlayGranted
            
            val text = if (!overlayGranted) "Overlay: Required" else if (overlayEnabled) "Overlay: Enabled" else "Overlay: Disabled"
            val isActive = overlayEnabled
            
            overlayBadge?.background = makeCardDrawable(
                if (isActive) "#0a2615" else "#2c0e0e",
                if (isActive) "#116035" else "#651717",
                8f
            )
            val dotColor = if (isActive) "#31be60" else "#ef4444"
            overlayBadgeDot?.background = GradientDrawable().apply {
                setColor(Color.parseColor(dotColor))
                cornerRadius = dp(4).toFloat()
            }
            overlayBadgeText?.text = text
            overlayBadgeText?.setTextColor(if (isActive) Color.parseColor("#a7f3d0") else Color.parseColor("#fca5a5"))
        }

        if (mediaGranted != cachedMediaPermission) {
            cachedMediaPermission = mediaGranted
            
            val text = if (mediaGranted) "Media Access: Active" else "Media: Required"
            val isActive = mediaGranted
            
            mediaBadge?.background = makeCardDrawable(
                if (isActive) "#0a2615" else "#2c0e0e",
                if (isActive) "#116035" else "#651717",
                8f
            )
            val dotColor = if (isActive) "#31be60" else "#ef4444"
            mediaBadgeDot?.background = GradientDrawable().apply {
                setColor(Color.parseColor(dotColor))
                cornerRadius = dp(4).toFloat()
            }
            mediaBadgeText?.text = text
            mediaBadgeText?.setTextColor(if (isActive) Color.parseColor("#a7f3d0") else Color.parseColor("#fca5a5"))
        }
    }

    private fun updateStoredSubtitlesList() {
        val allBindings = store.loadAllBindings()
        val hasChanged = cachedBindingsList == null ||
                allBindings.size != cachedBindingsList?.size ||
                allBindings.zip(cachedBindingsList!!).any { (a, b) ->
                    a.fileName != b.fileName || a.mediaTitle != b.mediaTitle
                }

        if (!hasChanged) return
        cachedBindingsList = allBindings

        val container = listContainer ?: return
        container.removeAllViews()

        if (allBindings.isEmpty()) {
            val emptyText = TextView(this).apply {
                text = "No subtitles stored on the TV yet.\nUpload srt files through the web panel."
                textSize = 14f
                setTextColor(Color.parseColor("#8c8775"))
                gravity = Gravity.CENTER
                setPadding(dp(24), dp(48), dp(24), dp(48))
            }
            container.addView(emptyText)
        } else {
            for (binding in allBindings) {
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(14), dp(12), dp(14), dp(12))
                    background = makeCardDrawable("#1c1a14", "#26241c", 14f)
                }

                val textLayout = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.LEFT
                }

                val titleText = TextView(this).apply {
                    text = binding.mediaTitle.ifBlank { "Unbound Subtitle" }
                    textSize = 15f
                    setTextColor(Color.parseColor("#f7f0dc"))
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    maxLines = 1
                    ellipsize = TextUtils.TruncateAt.END
                }
                textLayout.addView(titleText)

                val fileText = TextView(this).apply {
                    text = binding.fileName
                    textSize = 11f
                    setTextColor(Color.parseColor("#8c8775"))
                    maxLines = 1
                    ellipsize = TextUtils.TruncateAt.END
                    setPadding(0, dp(4), 0, 0)
                }
                textLayout.addView(fileText)

                row.addView(textLayout, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    weight = 1f
                })

                val playButton = TextView(this).apply {
                    text = "Play"
                    textSize = 13f
                    gravity = Gravity.CENTER
                    background = makeFocusableButtonDrawable()
                    setTextColor(Color.parseColor("#f7f0dc"))
                    setPadding(dp(16), dp(8), dp(16), dp(8))
                    isFocusable = true
                    isClickable = true
                    setOnFocusChangeListener { _, hasFocus ->
                        setTextColor(if (hasFocus) Color.parseColor("#050505") else Color.parseColor("#f7f0dc"))
                    }
                    setOnClickListener {
                        val videoId = extractVideoId(binding.fileName)
                        if (videoId != null) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$videoId"))
                            startActivity(intent)
                        } else {
                            android.widget.Toast.makeText(this@MainActivity, "Could not extract YouTube ID from file name.", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                row.addView(playButton, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    leftMargin = dp(12)
                })

                container.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = dp(8)
                })
            }
        }
    }

    private fun requestPermissionsThenShowOverlay() {
        when {
            !Settings.canDrawOverlays(this) -> {
                waitingForOverlayPermission = true
                openOverlaySettings()
            }
            !isNotificationListenerEnabled() -> {
                waitingForNotificationPermission = true
                openNotificationListenerSettings()
            }
            else -> showOverlay()
        }
    }

    private fun showOverlay() {
        startService(
            Intent(this, TextLensTvService::class.java)
                .setAction(TextLensTvService.ACTION_SHOW_OVERLAY),
        )
    }

    private fun openOverlaySettings() {
        startActivity(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName"),
            ),
        )
    }

    private fun openNotificationListenerSettings() {
        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

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
}
