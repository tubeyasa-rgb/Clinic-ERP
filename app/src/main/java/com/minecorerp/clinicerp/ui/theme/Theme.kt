package com.minecorerp.clinicerp.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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
    surfaceDim = Color(0xFFE6E6E6),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFAFAFA),
    surfaceContainer = Color(0xFFF6F6F6),
    surfaceContainerHigh = Color(0xFFF1F1F1),
    surfaceContainerHighest = Color(0xFFECECEC),
    primaryContainer = Teal100,
    onPrimaryContainer = Teal900,
    secondaryContainer = Mint,
    onSecondaryContainer = Teal900,
    tertiaryContainer = AccentLight,
    onTertiaryContainer = Color(0xFF303030),
    errorContainer = Color(0xFFFCE9E7),
    onErrorContainer = Color(0xFF7B2525),
    outline = Border,
    outlineVariant = Border,
    surfaceTint = Primary,
    inversePrimary = Color(0xFFE7E7E7),
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
    surfaceDim = Color(0xFF121212),
    surfaceBright = Color(0xFF333333),
    surfaceContainerLowest = Color(0xFF0F0F0F),
    surfaceContainerLow = Color(0xFF171717),
    surfaceContainer = Color(0xFF191919),
    surfaceContainerHigh = Color(0xFF222222),
    surfaceContainerHighest = Color(0xFF292929),
    primaryContainer = Color(0xFF343434),
    onPrimaryContainer = Color(0xFFEFEFEF),
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = DarkInk,
    tertiaryContainer = Color(0xFF343434),
    onTertiaryContainer = Color(0xFFEFEFEF),
    errorContainer = Color(0xFF5D2929),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFFA0A0A0),
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
        shapes = Shapes(
            extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
            small = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            medium = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
            large = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
        ),
        content = content
    )
    }
}
