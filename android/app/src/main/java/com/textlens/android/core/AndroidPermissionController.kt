package com.textlens.android.core

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat

data class AndroidPermissionState(
    val overlayGranted: Boolean = false,
    val screenCaptureGranted: Boolean = false,
    val notificationsGranted: Boolean = true,
    val screenCaptureMessage: String = "Screen capture permission has not been requested yet.",
) {
    val allReady: Boolean
        get() = overlayGranted && screenCaptureGranted && notificationsGranted
}

data class ScreenCaptureGrant(
    val resultCode: Int,
    val data: Intent,
)

class AndroidPermissionController(private val activity: ComponentActivity) {
    fun currentState(
        screenCaptureGrant: ScreenCaptureGrant?,
        screenCaptureMessage: String,
    ): AndroidPermissionState =
        AndroidPermissionState(
            overlayGranted = Settings.canDrawOverlays(activity),
            screenCaptureGranted = screenCaptureGrant != null,
            notificationsGranted = hasNotificationPermission(),
            screenCaptureMessage = screenCaptureMessage,
        )

    fun overlaySettingsIntent(): Intent =
        Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${activity.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun screenCaptureIntent(): Intent {
        val manager = activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        return manager.createScreenCaptureIntent()
    }

    fun notificationPermission(): String? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.POST_NOTIFICATIONS
        } else {
            null
        }

    private fun hasNotificationPermission(): Boolean =
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            true
        } else {
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        }
}

fun screenCaptureGrantOrNull(resultCode: Int, data: Intent?): ScreenCaptureGrant? =
    if (resultCode == Activity.RESULT_OK && data != null) {
        ScreenCaptureGrant(resultCode = resultCode, data = data)
    } else {
        null
    }
