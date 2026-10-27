package com.unrot.app.util

/**
 * Internal system components that inflate "app usage" totals without representing anything the
 * user meaningfully "used" — system UI chrome, theme engines, and input methods that have no
 * launcher icon of their own. The device's actual home/launcher package is excluded separately
 * by the caller (via [android.content.pm.PackageManager] resolution) since it varies by OEM and
 * can't be captured in a static list.
 *
 * Genuine preloaded apps the user can actually open (Settings, a carrier security center,
 * Chrome, etc.) are intentionally NOT filtered here, even though many of them are also
 * technically `FLAG_SYSTEM` — only non-interactive background/shell components are excluded, so
 * real usage the user chose to do isn't hidden from their own stats.
 */
object SystemPackages {
    private val HIDDEN_PREFIXES = listOf(
        "com.android.systemui",
        "com.android.theme",
        "com.android.wallpaper",
        "com.android.inputmethod",
        "com.google.android.inputmethod",
        "com.android.permissioncontroller",
        "com.android.settings.intelligence"
    )

    fun isHiddenFromUsage(packageName: String): Boolean =
        HIDDEN_PREFIXES.any { packageName.startsWith(it) }
}
