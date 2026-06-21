package com.textlens.android.core

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat

data class AndroidPermissionState(
    val overlayGranted: Boolean = false,
    val accessibilityGranted: Boolean = false,
    val notificationsGranted: Boolean = true,
) {
    val allReady: Boolean
        get() = overlayGranted && accessibilityGranted && notificationsGranted
}

class AndroidPermissionController(private val activity: ComponentActivity) {
    fun currentState(): AndroidPermissionState =
        AndroidPermissionState(
            overlayGranted = Settings.canDrawOverlays(activity),
            accessibilityGranted = hasAccessibilityPermission(),
            notificationsGranted = hasNotificationPermission(),
        )

    fun overlaySettingsIntent(): Intent =
        Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${activity.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun accessibilitySettingsIntent(): Intent =
        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

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

    private fun hasAccessibilityPermission(): Boolean {
        if (TextLensAccessibilityService.isReady) return true
        val enabledServices = Settings.Secure.getString(
            activity.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ).orEmpty()
        val serviceName = "${activity.packageName}/${TextLensAccessibilityService::class.java.name}"
        val shortServiceName = "${activity.packageName}/.core.TextLensAccessibilityService"
        return enabledServices.split(':').any {
            it.equals(serviceName, ignoreCase = true) || it.equals(shortServiceName, ignoreCase = true)
        }
    }
}
