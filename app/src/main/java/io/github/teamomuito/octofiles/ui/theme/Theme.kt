package io.github.teamomuito.octofiles.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
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

// the DS menus: a pale grid, steel-blue bars, and white windows with thin grey edges
private val Light = lightColorScheme(
    primary = Color(0xFF3D74B5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCFE2F4),
    onPrimaryContainer = Color(0xFF173B63),
    secondary = Color(0xFFD9719F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFBDDEC),
    onSecondaryContainer = Color(0xFF5E1C40),
    tertiary = Color(0xFF3C9E4E),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD8F0DA),
    onTertiaryContainer = Color(0xFF0F4220),
    background = Color(0xFFDCEAF5),
    onBackground = Color(0xFF1F3550),
    surface = Color.White,
    onSurface = Color(0xFF1F3550),
    surfaceVariant = Color(0xFFE4EEF7),
    onSurfaceVariant = Color(0xFF4D6480),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF5F9FC),
    surfaceContainer = Color(0xFFEEF4FA),
    surfaceContainerHigh = Color(0xFFE4EDF6),
    surfaceContainerHighest = Color(0xFFD8E4F0),
    outline = Color(0xFF7F93A8),
    outlineVariant = Color(0xFFBCCBDB),
)

// the same screens at night: the grid goes dark and the windows go slate
private val Dark = darkColorScheme(
    primary = Color(0xFF7FB2E8),
    onPrimary = Color(0xFF0E2740),
    primaryContainer = Color(0xFF2D5F94),
    onPrimaryContainer = Color(0xFFD6E8FA),
    secondary = Color(0xFFF0A0C8),
    onSecondary = Color(0xFF4A1A33),
    secondaryContainer = Color(0xFF6B2D52),
    onSecondaryContainer = Color(0xFFFBDDEC),
    tertiary = Color(0xFF7FD091),
    onTertiary = Color(0xFF0E3A18),
    tertiaryContainer = Color(0xFF2A5C36),
    onTertiaryContainer = Color(0xFFD8F0DA),
    background = Color(0xFF17263A),
    onBackground = Color(0xFFDCE8F5),
    surface = Color(0xFF213349),
    onSurface = Color(0xFFDCE8F5),
    surfaceVariant = Color(0xFF2B4060),
    onSurfaceVariant = Color(0xFFA9BED6),
    surfaceContainerLowest = Color(0xFF15212F),
    surfaceContainerLow = Color(0xFF1C2D42),
    surfaceContainer = Color(0xFF223751),
    surfaceContainerHigh = Color(0xFF2A4262),
    surfaceContainerHighest = Color(0xFF334E73),
    outline = Color(0xFF6C84A0),
    outlineVariant = Color(0xFF3A5070),
)

/** Colors for the little kind labels. Soft, like the calendar tiles on the DS home screen. */
object Pastel {
    val pink = Color(0xFFF6D3E8)
    val pinkInk = Color(0xFF7A2352)
    val sky = Color(0xFFCFE5F8)
    val skyInk = Color(0xFF1E4E7E)
    val mint = Color(0xFFD2F0D8)
    val mintInk = Color(0xFF1F6B35)
    val butter = Color(0xFFFBEDB0)
    val butterInk = Color(0xFF6B5300)
}

/** Pixelify Sans, for headings only. One variable file, two weights. */
@OptIn(ExperimentalTextApi::class)
val PixelFont = FontFamily(
    Font(R.font.pixelify_sans, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.pixelify_sans, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
)

private val base = Typography()

// headings get the pixel face, everything you read stays in the plain sans
private val OctoType = Typography(
    displaySmall = base.displaySmall.copy(fontFamily = PixelFont, fontWeight = FontWeight.Bold),
    headlineMedium = base.headlineMedium.copy(fontFamily = PixelFont, fontWeight = FontWeight.Bold),
    headlineSmall = base.headlineSmall.copy(fontFamily = PixelFont, fontWeight = FontWeight.Bold),
    titleLarge = base.titleLarge.copy(fontFamily = PixelFont, fontWeight = FontWeight.Bold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.Medium, fontSize = 16.sp),
    bodyLarge = base.bodyLarge.copy(fontSize = 16.sp),
    bodyMedium = base.bodyMedium.copy(fontSize = 14.sp),
    bodySmall = base.bodySmall.copy(fontSize = 12.sp),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.Medium, fontSize = 14.sp),
    labelMedium = base.labelMedium.copy(fontSize = 12.sp),
    labelSmall = base.labelSmall.copy(fontSize = 11.sp),
)

// square everywhere: the DS windows have no rounded corners
private val OctoShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(0.dp),
    medium = RoundedCornerShape(0.dp),
    large = RoundedCornerShape(0.dp),
    extraLarge = RoundedCornerShape(0.dp),
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
