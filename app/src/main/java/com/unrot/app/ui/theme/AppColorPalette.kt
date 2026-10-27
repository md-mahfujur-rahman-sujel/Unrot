package com.unrot.app.ui.theme

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Reads each app's real card color straight off its actual launcher icon, instead of needing a
 * hand-picked entry for every app that exists (there are millions of them). Uses Android's
 * Palette library to pick the most vibrant swatch out of the icon's pixels, so e.g. a plain red
 * icon gives a red card, a blue icon gives a blue card, matching what the user sees on their own
 * home screen — no per-app maintenance required.
 *
 * Results are cached in memory per package name: icon decoding + Palette generation aren't free,
 * and the same package shows up repeatedly across the picker, Home, Stats, and Usage screens, so
 * this ensures the real work happens at most once per app per process lifetime.
 */
object AppIconColorCache {
    private val cache = ConcurrentHashMap<String, Color>()

    /** Returns the cached color if we've already extracted one for [packageName]. */
    fun peek(packageName: String): Color? = cache[packageName]

    /**
     * Extracts (or returns the cached) color for [packageName]'s real launcher icon. Safe to
     * call from any coroutine — the actual bitmap/Palette work is dispatched off the main thread.
     * Falls back to [hashColorFor] if the icon can't be loaded (e.g. app was uninstalled) or
     * Palette can't find a usable swatch in it.
     */
    suspend fun colorFor(context: Context, packageName: String): Color {
        cache[packageName]?.let { return it }
        val color = withContext(Dispatchers.Default) {
            runCatching {
                val bitmap = context.packageManager
                    .getApplicationIcon(packageName)
                    .toBitmap(width = 96, height = 96)
                val palette = Palette.from(bitmap).generate()
                // Prefer a punchy, saturated swatch (what a person would call "the app's color")
                // over a literal pixel average, which tends to wash out toward gray on icons that
                // mix a bold glyph with a lot of white/transparent background.
                val swatch = palette.vibrantSwatch
                    ?: palette.lightVibrantSwatch
                    ?: palette.darkVibrantSwatch
                    ?: palette.dominantSwatch
                    ?: palette.mutedSwatch
                swatch?.let { Color(it.rgb) }
            }.getOrNull()
        } ?: hashColorFor(packageName)
        cache[packageName] = color
        return color
    }
}

/**
 * The color a card/chart should use for [packageName], live: a hand-picked brand color if one
 * exists (see [hasCuratedColor]), otherwise the real color read from that app's own icon via
 * [AppIconColorCache] — extracted once, then cached for the rest of the process.
 *
 * Returns [brandColorFor]'s instant value immediately (so nothing flashes an unstyled default
 * while extraction runs), then updates in place once real-icon extraction finishes. Call this
 * from any Composable that shows a per-app tint — app picker rows, Home limit cards, Stats bars
 * and the donut chart, All-usage rows.
 */
@Composable
fun rememberAppColor(packageName: String): State<Color> {
    val context = LocalContext.current
    val initial = AppIconColorCache.peek(packageName) ?: brandColorFor(packageName)
    return produceState(initialValue = initial, key1 = packageName) {
        if (!hasCuratedColor(packageName)) {
            value = AppIconColorCache.colorFor(context, packageName)
        }
    }
}

/**
 * Batch version of [rememberAppColor] for places that need several apps' colors at once outside
 * a per-item Composable — e.g. building a donut chart's slices for a whole list of apps in one
 * go. Returns a live map from package name to color, filled in with each app's real icon color
 * as extraction completes (curated/cached ones resolve immediately).
 */
@Composable
fun rememberAppColors(packageNames: List<String>): Map<String, Color> {
    val context = LocalContext.current
    var colors by remember(packageNames) {
        mutableStateOf(packageNames.associateWith { AppIconColorCache.peek(it) ?: brandColorFor(it) })
    }
    LaunchedEffect(packageNames) {
        packageNames.forEach { pkg ->
            if (!hasCuratedColor(pkg) && AppIconColorCache.peek(pkg) == null) {
                val extracted = AppIconColorCache.colorFor(context, pkg)
                colors = colors + (pkg to extracted)
            }
        }
    }
    return colors
}
