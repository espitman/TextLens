package com.textlens.android.core

import android.content.Intent

object MediaProjectionSession {
    var grant: ScreenCaptureGrant? = null
        private set

    fun update(resultCode: Int, data: Intent?) {
        grant = screenCaptureGrantOrNull(resultCode, data)
    }

    fun clear() {
        grant = null
    }
}
