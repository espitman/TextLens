package com.textlens.android.core

data class ScreenArea(
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int,
) {
    val isMeaningful: Boolean
        get() = width >= 24 && height >= 24
}
