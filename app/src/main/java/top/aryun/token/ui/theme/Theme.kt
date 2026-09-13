package top.aryun.token.ui.theme

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

private val CookieLight = lightColorScheme(
    primary = Color(0xFF8C5000),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDCBE),
    onPrimaryContainer = Color(0xFF2C1600),
    secondary = Color(0xFF725B3E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDCBE),
    onSecondaryContainer = Color(0xFF291806),
    tertiary = Color(0xFF55633F),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD9E8BB),
    onTertiaryContainer = Color(0xFF131F03),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFFF8F5),
    onBackground = Color(0xFF221A12),
    surface = Color(0xFFFFF8F5),
    onSurface = Color(0xFF221A12),
    surfaceVariant = Color(0xFFF1DFD1),
    onSurfaceVariant = Color(0xFF50453B),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFF1E9),
    surfaceContainer = Color(0xFFFCEBE1),
    surfaceContainerHigh = Color(0xFFF6E5DC),
    surfaceContainerHighest = Color(0xFFF1DFD6),
    surfaceDim = Color(0xFFE6D7CE),
    surfaceBright = Color(0xFFFFF8F5),
    outline = Color(0xFF82756A),
    outlineVariant = Color(0xFFD4C3B5),
    inverseSurface = Color(0xFF382E26),
    inverseOnSurface = Color(0xFFFEEEE5),
    inversePrimary = Color(0xFFFFB870),
)

private val CookieDark = darkColorScheme(
    primary = Color(0xFFFFB870),
    onPrimary = Color(0xFF4A2800),
    primaryContainer = Color(0xFF6A3C00),
    onPrimaryContainer = Color(0xFFFFDCBE),
    secondary = Color(0xFFE0C1A0),
    onSecondary = Color(0xFF402C15),
    secondaryContainer = Color(0xFF59422A),
    onSecondaryContainer = Color(0xFFFFDCBE),
    tertiary = Color(0xFFBDCFA0),
    onTertiary = Color(0xFF283500),
    tertiaryContainer = Color(0xFF3E4C26),
    onTertiaryContainer = Color(0xFFD9E8BB),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF1A120B),
    onBackground = Color(0xFFF1DFD6),
    surface = Color(0xFF1A120B),
    onSurface = Color(0xFFF1DFD6),
    surfaceVariant = Color(0xFF50453B),
    onSurfaceVariant = Color(0xFFD4C3B5),
    surfaceContainerLowest = Color(0xFF140C06),
    surfaceContainerLow = Color(0xFF221A12),
    surfaceContainer = Color(0xFF271E16),
    surfaceContainerHigh = Color(0xFF322820),
    surfaceContainerHighest = Color(0xFF3D332A),
    surfaceDim = Color(0xFF1A120B),
    surfaceBright = Color(0xFF42372E),
    outline = Color(0xFF9C8F83),
    outlineVariant = Color(0xFF50453B),
    inverseSurface = Color(0xFFF1DFD6),
    inverseOnSurface = Color(0xFF382E26),
    inversePrimary = Color(0xFF8C5000),
)

@Composable
fun GetCookieTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

        darkTheme -> CookieDark
        else -> CookieLight
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
