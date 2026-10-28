package com.minecorerp.clinicerp.ui.theme

import androidx.compose.ui.graphics.Color
import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    secondary = Secondary,
    onSecondary = OnSecondary,
    tertiary = Tertiary,
    onTertiary = OnTertiary,
    error = Error,
    onError = OnError,
    background = Background,
    onBackground = Ink,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    surfaceDim = Color(0xFFDDE5D5),
    surfaceBright = Color(0xFFFAFCF7),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF4F7EE),
    surfaceContainer = Color(0xFFEEF3E7),
    surfaceContainerHigh = Color(0xFFE7EEDD),
    surfaceContainerHighest = Color(0xFFDFE8D4),
    primaryContainer = Teal100,
    onPrimaryContainer = Teal900,
    secondaryContainer = Mint,
    onSecondaryContainer = Teal900,
    tertiaryContainer = AccentLight,
    onTertiaryContainer = Color(0xFF604611),
    errorContainer = Color(0xFFFCE9E7),
    onErrorContainer = Color(0xFF7B2525),
    outline = Border,
    outlineVariant = Border,
    surfaceTint = Primary,
    inversePrimary = Color(0xFFB3D39E),
    inverseSurface = DarkSurface,
    inverseOnSurface = DarkInk,
    onSurfaceVariant = OnSurfaceVariant
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    error = DarkError,
    onError = DarkOnError,
    background = DarkBackground,
    onBackground = DarkInk,
    surface = DarkSurfaceColor,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    surfaceDim = Color(0xFF141D13),
    surfaceBright = Color(0xFF35422E),
    surfaceContainerLowest = Color(0xFF10180F),
    surfaceContainerLow = Color(0xFF192417),
    surfaceContainer = Color(0xFF1D291A),
    surfaceContainerHigh = Color(0xFF273422),
    surfaceContainerHighest = Color(0xFF31402A),
    primaryContainer = Color(0xFF354E2B),
    onPrimaryContainer = Color(0xFFDAEFCB),
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = DarkInk,
    tertiaryContainer = Color(0xFF4C3B17),
    onTertiaryContainer = Color(0xFFF7DFA9),
    errorContainer = Color(0xFF5D2929),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFFA0B293),
    outlineVariant = DarkBorder,
    surfaceTint = DarkPrimary,
    inversePrimary = Primary,
    inverseSurface = Background,
    inverseOnSurface = Ink,
    onSurfaceVariant = DarkOnSurfaceVariant
)

@Composable
fun ClinicErpTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
    }
}
