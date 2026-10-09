package io.github.teamomuito.octofiles.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.teamomuito.octofiles.R

// the DS palette: a sky-blue top screen, navy ink, and the bright pen colors
private val Light = lightColorScheme(
    primary = Color(0xFF2B7BD9),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCBEBFF),
    onPrimaryContainer = Color(0xFF0B3B73),
    secondary = Color(0xFFF2862B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE3C4),
    onSecondaryContainer = Color(0xFF5C2D00),
    tertiary = Color(0xFF2FAA4F),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD2F6D6),
    onTertiaryContainer = Color(0xFF0C3F1B),
    background = Color(0xFFBDE7FA),
    onBackground = Color(0xFF1C3F7A),
    surface = Color(0xFFF2FBFF),
    onSurface = Color(0xFF1C3F7A),
    surfaceVariant = Color(0xFFD7F0FC),
    onSurfaceVariant = Color(0xFF3F6A9E),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF4FCFF),
    surfaceContainer = Color(0xFFE7F7FD),
    surfaceContainerHigh = Color(0xFFDAF2FC),
    surfaceContainerHighest = Color(0xFFCBEBF9),
    outline = Color(0xFF3F6A9E),
    outlineVariant = Color(0xFF9ACDEB),
)

// the same screen at night: deep navy with the dots turned down
private val Dark = darkColorScheme(
    primary = Color(0xFF6DB8FF),
    onPrimary = Color(0xFF0A2A55),
    primaryContainer = Color(0xFF1E5CA6),
    onPrimaryContainer = Color(0xFFCBEBFF),
    secondary = Color(0xFFFFB36B),
    onSecondary = Color(0xFF4A2400),
    secondaryContainer = Color(0xFF6B3A12),
    onSecondaryContainer = Color(0xFFFFE3C4),
    tertiary = Color(0xFF6FE08F),
    onTertiary = Color(0xFF0A3A1B),
    tertiaryContainer = Color(0xFF1F5B31),
    onTertiaryContainer = Color(0xFFD2F6D6),
    background = Color(0xFF0F2A55),
    onBackground = Color(0xFFD6EEFF),
    surface = Color(0xFF163766),
    onSurface = Color(0xFFD6EEFF),
    surfaceVariant = Color(0xFF1F4A80),
    onSurfaceVariant = Color(0xFF9CCBEE),
    surfaceContainerLowest = Color(0xFF0B2147),
    surfaceContainerLow = Color(0xFF123060),
    surfaceContainer = Color(0xFF173A6E),
    surfaceContainerHigh = Color(0xFF1D4480),
    surfaceContainerHighest = Color(0xFF245292),
    outline = Color(0xFF5C8FC7),
    outlineVariant = Color(0xFF2C5A94),
)

/** Colors for the little kind labels. Bright like the DS pen colors, they sit on top of screenshots. */
object Pastel {
    val pink = Color(0xFFFFC9E6)
    val pinkInk = Color(0xFF8A1E5A)
    val sky = Color(0xFFBFE6FF)
    val skyInk = Color(0xFF0E4D8A)
    val mint = Color(0xFFC6F4C9)
    val mintInk = Color(0xFF17652B)
    val butter = Color(0xFFFFF2A0)
    val butterInk = Color(0xFF6B5300)
}

/** Pixelify Sans, the pixel face the DS menus are drawn in. One variable file, two weights. */
@OptIn(ExperimentalTextApi::class)
val PixelFont = FontFamily(
    Font(R.font.pixelify_sans, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.pixelify_sans, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
)

private val base = Typography()

// pixel type runs small, so the body sizes sit a little bigger than the stock ones
private val OctoType = Typography(
    displaySmall = base.displaySmall.copy(fontFamily = PixelFont, fontWeight = FontWeight.Bold),
    headlineMedium = base.headlineMedium.copy(fontFamily = PixelFont, fontWeight = FontWeight.Bold),
    headlineSmall = base.headlineSmall.copy(fontFamily = PixelFont, fontWeight = FontWeight.Bold),
    titleLarge = base.titleLarge.copy(fontFamily = PixelFont, fontWeight = FontWeight.Bold),
    titleMedium = base.titleMedium.copy(fontFamily = PixelFont, fontWeight = FontWeight.Normal, fontSize = 18.sp),
    bodyLarge = base.bodyLarge.copy(fontFamily = PixelFont, fontSize = 18.sp),
    bodyMedium = base.bodyMedium.copy(fontFamily = PixelFont, fontSize = 16.sp),
    bodySmall = base.bodySmall.copy(fontFamily = PixelFont, fontSize = 14.sp),
    labelLarge = base.labelLarge.copy(fontFamily = PixelFont, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    labelMedium = base.labelMedium.copy(fontFamily = PixelFont, fontWeight = FontWeight.Normal, fontSize = 13.sp),
    labelSmall = base.labelSmall.copy(fontFamily = PixelFont, fontWeight = FontWeight.Normal, fontSize = 12.sp),
)

private val OctoShapes = Shapes(
    extraSmall = pixelCorners(6.dp),
    small = pixelCorners(10.dp),
    medium = pixelCorners(16.dp),
    large = pixelCorners(22.dp),
    extraLarge = pixelCorners(28.dp),
)

@Composable
fun OctoTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val colors = if (dark) Dark else Light
    MaterialTheme(colorScheme = colors, typography = OctoType, shapes = OctoShapes) {
        // text drawn straight on the background needs a color too, not just text inside cards
        CompositionLocalProvider(
            LocalContentColor provides colors.onBackground,
            LocalPanel provides if (dark) DarkPanel else LightPanel,
            content = content,
        )
    }
}
