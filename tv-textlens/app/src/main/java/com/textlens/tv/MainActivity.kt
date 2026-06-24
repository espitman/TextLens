package com.textlens.tv

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var urlText: TextView
    private var waitingForOverlayPermission = false
    private var waitingForNotificationPermission = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startService(Intent(this, TextLensTvService::class.java))
        setContentView(makeContentView())
    }

    override fun onResume() {
        super.onResume()
        urlText.text = TextLensWebServer.panelUrl()
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
    }

    private fun makeContentView(): LinearLayout {
        val title = TextView(this).apply {
            text = "TextLens TV"
            textSize = 34f
            setTextColor(Color.rgb(255, 208, 0))
            gravity = Gravity.CENTER
        }

        val hint = TextView(this).apply {
            text = "Open this address on your phone or laptop, upload a translated SRT, and style the TV overlay."
            textSize = 18f
            setTextColor(Color.rgb(210, 210, 210))
            gravity = Gravity.CENTER
        }

        urlText = TextView(this).apply {
            text = TextLensWebServer.panelUrl()
            textSize = 26f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(24, 18, 24, 18)
            setBackgroundColor(Color.rgb(35, 35, 35))
        }

        val showButton = Button(this).apply {
            text = "Show Overlay"
            textSize = 20f
            setOnClickListener { requestPermissionsThenShowOverlay() }
        }

        val hideButton = Button(this).apply {
            text = "Hide Overlay"
            textSize = 20f
            setOnClickListener {
                startService(
                    Intent(this@MainActivity, TextLensTvService::class.java)
                        .setAction(TextLensTvService.ACTION_HIDE_OVERLAY),
                )
            }
        }

        val notificationButton = Button(this).apply {
            text = "Media Access"
            textSize = 20f
            setOnClickListener { openNotificationListenerSettings() }
        }

        val buttonRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            addView(showButton)
            addView(
                hideButton,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply { leftMargin = 18 },
            )
            addView(
                notificationButton,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply { leftMargin = 18 },
            )
        }

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.rgb(10, 10, 10))
            setPadding(56, 56, 56, 56)
            addView(
                title,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ),
            )
            addView(
                hint,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply {
                    topMargin = 24
                    bottomMargin = 26
                },
            )
            addView(
                urlText,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply { bottomMargin = 32 },
            )
            addView(buttonRow)
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
