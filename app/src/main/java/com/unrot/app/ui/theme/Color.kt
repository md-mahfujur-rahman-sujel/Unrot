package com.unrot.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Core palette
val UnrotPrimary = Color(0xFF0A84FF)
val UnrotPrimaryDark = Color(0xFF007AFF)
val UnrotSecondary = Color(0xFF5E5CE6)
val UnrotBackground = Color(0xFFF2F2F7)
val UnrotSurface = Color(0xFFFFFFFF)
val UnrotSurfaceVariant = Color(0xFFFFFFFF)
val UnrotOnBackground = Color(0xFF1C1C1E)
val UnrotOnSurface = Color(0xFF1C1C1E)
val UnrotError = Color(0xFFFF3B30)
val UnrotSuccess = Color(0xFF30D158)
val UnrotWarning = Color(0xFFFF9F0A)

// Soft ambient "wallpaper" blobs that drift behind the frosted glass panels — this is what
// gives the UI its depth; every card sits on top of these as a translucent layer.
val UnrotBlobBlue = Color(0xFF64D2FF)
val UnrotBlobPurple = Color(0xFF7A6CF0)
val UnrotBlobPink = Color(0xFFFF6482)
val UnrotBlobMint = Color(0xFF63E6BE)
val UnrotBlobOrange = Color(0xFFFFAE5C)
val UnrotBlobYellow = Color(0xFFFFD60A)

// Shared chart palette so every chart across the app (Stats, Home) uses consistent series colors.
val ChartPalette = listOf(
    UnrotPrimary,
    UnrotBlobPurple,
    UnrotBlobPink,
    UnrotBlobMint,
    UnrotWarning,
    UnrotBlobOrange,
    UnrotSecondary
)

/**
 * Deterministic two-tone gradient derived from a package name, used as the fallback avatar
 * whenever an app's real launcher icon can't be resolved (e.g. it was uninstalled).
 */
fun iconGradientFor(packageName: String): Brush {
    val hash = packageName.hashCode().let { if (it < 0) -it else it }
    val hue = (hash % 360).toFloat()
    val c1 = Color.hsl(hue, 0.60f, 0.56f)
    val c2 = Color.hsl((hue + 42f) % 360f, 0.60f, 0.40f)
    return Brush.linearGradient(listOf(c1, c2))
}

/**
 * Hand-picked colors for apps whose real icon is a flat mono-color glyph or a heavy multi-color
 * gradient that on-device Palette extraction tends to read poorly (it might pick a background
 * white or a minor gradient stop instead of the color a person actually associates with the
 * brand). Everything else is left out on purpose — those apps get their card color read straight
 * from their real icon, see [AppIconColorCache].
 */
private val CURATED_BRAND_COLORS: Map<String, Color> = mapOf(
    "com.instagram.android" to Color(0xFFE1306C),
    "com.instagram.barcelona" to Color(0xFF000000), // Threads
    "com.zhiliaoapp.musically" to Color(0xFFFF0050), // TikTok
    "com.ss.android.ugc.trill" to Color(0xFFFF0050), // TikTok (alt package)
    "com.google.android.youtube" to Color(0xFFFF0000),
    "com.twitter.android" to Color(0xFF000000), // X (post-rebrand black)
    "com.snapchat.android" to Color(0xFFFFFC00),
    "com.reddit.frontpage" to Color(0xFFFF4500),
    "com.facebook.katana" to Color(0xFF1877F2),
    "com.facebook.orca" to Color(0xFF00B2FF), // Messenger
    "com.pinterest" to Color(0xFFE60023),
    "com.linkedin.android" to Color(0xFF0A66C2),
    "com.discord" to Color(0xFF5865F2),
    "com.whatsapp" to Color(0xFF25D366),
    "org.telegram.messenger" to Color(0xFF26A5E4),
    "tv.twitch.android.app" to Color(0xFF9146FF),
    "com.spotify.music" to Color(0xFF1DB954)
)

/** True if [packageName] has a hand-picked entry in [CURATED_BRAND_COLORS] — if so, real-icon
 *  extraction is skipped for it and the curated color is used as-is. */
fun hasCuratedColor(packageName: String): Boolean = CURATED_BRAND_COLORS.containsKey(packageName)

/**
 * Deterministic hue derived from the package name — the same hash used by [iconGradientFor].
 * Used only as a last-resort color: when an app has no curated entry AND its icon can't be
 * loaded or yields no usable Palette swatch.
 */
fun hashColorFor(packageName: String): Color {
    val hash = packageName.hashCode().let { if (it < 0) -it else it }
    val hue = (hash % 360).toFloat()
    return Color.hsl(hue, 0.60f, 0.50f)
}

/**
 * The synchronous, "instant" color for [packageName]: the curated brand color if hand-picked,
 * otherwise the deterministic hash color. Used as the value shown immediately (before real-icon
 * extraction finishes) by [rememberAppColor], and as the fallback in non-Composable contexts.
 * For a Composable that should show the *real* icon-derived color once available, use
 * [rememberAppColor] instead of calling this directly.
 */
fun brandColorFor(packageName: String): Color =
    CURATED_BRAND_COLORS[packageName] ?: hashColorFor(packageName)
