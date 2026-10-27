package com.unrot.app.ui.theme

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap

/**
 * Shows the real, live launcher icon for [packageName] — the same icon the user sees on their
 * own home screen — fetched straight from [android.content.pm.PackageManager]. This is what
 * makes other apps' logos appear inside Unrot (app picker, limits, stats, usage list).
 *
 * Falls back to a deterministic colored initial if the icon can't be resolved, e.g. the app was
 * uninstalled after being tracked.
 */
@Composable
fun AppIcon(
    packageName: String,
    appName: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    cornerRadius: Dp = size / 3.4f
) {
    val context = LocalContext.current
    val bitmapState = produceState<ImageBitmap?>(initialValue = null, key1 = packageName) {
        value = runCatching {
            context.packageManager
                .getApplicationIcon(packageName)
                .toBitmap(width = 128, height = 128)
                .asImageBitmap()
        }.getOrNull()
    }

    val bitmap = bitmapState.value
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .then(
                if (bitmap == null) Modifier.background(iconGradientFor(packageName)) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = appName,
                modifier = Modifier.size(size),
                contentScale = ContentScale.Fit
            )
        } else {
            Text(
                text = appName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}
