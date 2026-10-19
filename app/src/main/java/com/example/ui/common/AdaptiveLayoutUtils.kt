package com.example.ui.common

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Window Size Classes for adaptive mobile, foldable, and tablet POS layouts.
 */
enum class AppWindowSizeClass(
    val label: String
) {
    COMPACT("Compact (Smartphone)"),
    MEDIUM("Medium (Tablet Portrait / Foldable)"),
    EXPANDED("Expanded (Tablet Landscape / POS Terminal)");

    val isTabletOrWide: Boolean
        get() = this == MEDIUM || this == EXPANDED

    val isExpanded: Boolean
        get() = this == EXPANDED
}

fun calculateAppWindowSizeClass(maxWidth: Dp): AppWindowSizeClass {
    return when {
        maxWidth < 600.dp -> AppWindowSizeClass.COMPACT
        maxWidth < 840.dp -> AppWindowSizeClass.MEDIUM
        else -> AppWindowSizeClass.EXPANDED
    }
}

val LocalAppWindowSizeClass = staticCompositionLocalOf { AppWindowSizeClass.COMPACT }
