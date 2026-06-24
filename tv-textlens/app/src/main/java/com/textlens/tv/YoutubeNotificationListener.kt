package com.textlens.tv

import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.service.notification.NotificationListenerService

class YoutubeNotificationListener : NotificationListenerService() {
    companion object {
        fun requestReconnect(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                requestRebind(ComponentName(context, YoutubeNotificationListener::class.java))
            }
        }
    }
}
