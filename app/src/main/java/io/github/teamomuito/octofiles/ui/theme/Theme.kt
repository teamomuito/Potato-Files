package io.github.teamomuito.octofiles.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.teamomuito.octofiles.R

// the DS menu palette, sampled from pictoclip: paper grid, slate header, pale footer, blue cursor
private val Light = lightColorScheme(
    primary = Color(0xFF3E6DB0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD5E2EE),
    onPrimaryContainer = Color(0xFF1B3A63),
    secondary = Color(0xFF4A6573),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCE6EB),
    onSecondaryContainer = Color(0xFF2A3D47),
    tertiary = Color(0xFF4F7F5E),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD3E8DA),
    onTertiaryContainer = Color(0xFF0C2A18),
    background = Color(0xFFF4F5F6),
    onBackground = Color(0xFF3A3F45),
    surface = Color.White,
    onSurface = Color(0xFF3A3F45),
    surfaceVariant = Color(0xFFE3E6E9),
    onSurfaceVariant = Color(0xFF5E656C),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF8F9FA),
    surfaceContainer = Color(0xFFEEF0F2),
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color(0xFFD9DDE1),
    outline = Color(0xFF4A4F55),
    outlineVariant = Color(0xFFC3C8CD),
)

// the same screens at night: the paper goes slate, the header and footer go deeper
private val Dark = darkColorScheme(
    primary = Color(0xFF7FA6DA),
    onPrimary = Color(0xFF0E1A2B),
    primaryContainer = Color(0xFF2E4A6E),
    onPrimaryContainer = Color(0xFFD5E2EE),
    secondary = Color(0xFF9DB3BE),
    onSecondary = Color(0xFF00131A),
    secondaryContainer = Color(0xFF354852),
    onSecondaryContainer = Color(0xFFD9E8EF),
    tertiary = Color(0xFF8CC79B),
    onTertiary = Color(0xFF0C2A18),
    tertiaryContainer = Color(0xFF2E5139),
    onTertiaryContainer = Color(0xFFD3E8DA),
    background = Color(0xFF2B3136),
    onBackground = Color(0xFFE6EEF2),
    surface = Color(0xFF3A4449),
    onSurface = Color(0xFFE6EEF2),
    surfaceVariant = Color(0xFF454F55),
    onSurfaceVariant = Color(0xFFB4C4CC),
    surfaceContainerLowest = Color(0xFF252B2F),
    surfaceContainerLow = Color(0xFF30373C),
    surfaceContainer = Color(0xFF384146),
    surfaceContainerHigh = Color(0xFF424C52),
    surfaceContainerHighest = Color(0xFF4C575E),
    outline = Color(0xFF7C8E97),
    outlineVariant = Color(0xFF55636B),
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

/** DotGothic16, the pixel face the DS menus use for everything. It has one weight. */
val DotFont = FontFamily(Font(R.font.dotgothic16))

// every style gets the pixel face at regular weight, with no tracking, so text reads like the DS menus
private fun pixel(style: TextStyle, size: TextUnit) = style.copy(
    fontFamily = DotFont,
    fontWeight = FontWeight.Normal,
    fontSize = size,
    letterSpacing = 0.sp,
)

private val base = Typography()

private val OctoType = Typography(
    displayLarge = pixel(base.displayLarge, 48.sp),
    displayMedium = pixel(base.displayMedium, 40.sp),
    displaySmall = pixel(base.displaySmall, 32.sp),
    headlineLarge = pixel(base.headlineLarge, 28.sp),
    headlineMedium = pixel(base.headlineMedium, 24.sp),
    headlineSmall = pixel(base.headlineSmall, 22.sp),
    titleLarge = pixel(base.titleLarge, 20.sp),
    titleMedium = pixel(base.titleMedium, 16.sp),
    titleSmall = pixel(base.titleSmall, 14.sp),
    bodyLarge = pixel(base.bodyLarge, 16.sp),
    bodyMedium = pixel(base.bodyMedium, 14.sp),
    bodySmall = pixel(base.bodySmall, 12.sp),
    labelLarge = pixel(base.labelLarge, 14.sp),
    labelMedium = pixel(base.labelMedium, 12.sp),
    labelSmall = pixel(base.labelSmall, 11.sp),
)

/**
 * The DS panels are cut at the corners, not rounded: a 2dp chamfer that matches the rim width.
 * Every Material shape uses it too, so cards and dialogs get the same notch.
 */
val PanelShape = CutCornerShape(2.dp)

private val OctoShapes = Shapes(
    extraSmall = PanelShape,
    small = PanelShape,
    medium = PanelShape,
    large = PanelShape,
    extraLarge = PanelShape,
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
