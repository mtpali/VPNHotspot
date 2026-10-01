package be.mygod.vpnhotspot.ui.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val oledColors = darkColorScheme(
    primary = Color.White, onPrimary = Color.Black,
    primaryContainer = Color.Black, onPrimaryContainer = Color.White,
    inversePrimary = Color.Black,
    secondary = Color.White, onSecondary = Color.Black,
    secondaryContainer = Color.Black, onSecondaryContainer = Color.White,
    tertiary = Color.White, onTertiary = Color.Black,
    tertiaryContainer = Color.Black, onTertiaryContainer = Color.White,
    background = Color.Black, onBackground = Color.White,
    surface = Color.Black, onSurface = Color.White,
    surfaceVariant = Color.Black, onSurfaceVariant = Color.White,
    surfaceTint = Color.Black,
    inverseSurface = Color.White, inverseOnSurface = Color.Black,
    error = Color.White, onError = Color.Black,
    errorContainer = Color.Black, onErrorContainer = Color.White,
    outline = Color.White, outlineVariant = Color.White, scrim = Color.Black,
    surfaceBright = Color.Black, surfaceDim = Color.Black,
    surfaceContainer = Color.Black, surfaceContainerHigh = Color.Black,
    surfaceContainerHighest = Color.Black, surfaceContainerLow = Color.Black,
    surfaceContainerLowest = Color.Black,
    primaryFixed = Color.White, primaryFixedDim = Color.White,
    onPrimaryFixed = Color.Black, onPrimaryFixedVariant = Color.Black,
    secondaryFixed = Color.White, secondaryFixedDim = Color.White,
    onSecondaryFixed = Color.Black, onSecondaryFixedVariant = Color.Black,
    tertiaryFixed = Color.White, tertiaryFixedDim = Color.White,
    onTertiaryFixed = Color.Black, onTertiaryFixedVariant = Color.Black,
)

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun VpnHotspotTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = oledColors,
        motionScheme = MotionScheme.standard(),
        content = content,
    )
}

@Composable
fun VpnHotspotPreviewSurface(content: @Composable () -> Unit) {
    VpnHotspotTheme {
        Surface(content = content)
    }
}
