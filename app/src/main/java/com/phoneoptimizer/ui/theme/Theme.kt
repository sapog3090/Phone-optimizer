package com.phoneoptimizer.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val Good = Color(0xFF2E9E5B)
val Warn = Color(0xFFE8A317)
val Bad = Color(0xFFD9443A)

private val LightColors = lightColorScheme(
    primary = Color(0xFF0F9D58),
    secondary = Color(0xFF00897B),
    tertiary = Color(0xFF3F51B5),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF5BD893),
    secondary = Color(0xFF4DB6AC),
    tertiary = Color(0xFF9FA8DA),
)

/** Цвет индикатора по степени заполнения: зелёный → жёлтый → красный. */
fun levelColor(fraction: Float): Color = when {
    fraction < 0.7f -> Good
    fraction < 0.85f -> Warn
    else -> Bad
}

@Composable
fun PhoneOptimizerTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val colors = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (dark) DarkColors else LightColors
    }
    MaterialTheme(colorScheme = colors, content = content)
}
