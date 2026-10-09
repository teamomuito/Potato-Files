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

// sampled from the DS chat room screen: a grey field, blue-grey header and footer, off-white rows
private val Light = lightColorScheme(
    primary = Color(0xFF4D7D94),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC2D6DF),
    onPrimaryContainer = Color(0xFF00131A),
    secondary = Color(0xFF6C8793),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD0DEE4),
    onSecondaryContainer = Color(0xFF00131A),
    tertiary = Color(0xFF4F7F5E),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD3E8DA),
    onTertiaryContainer = Color(0xFF0C2A18),
    background = Color(0xFFABADAC),
    onBackground = Color(0xFF00131A),
    surface = Color(0xFFF8F8F8),
    onSurface = Color(0xFF00131A),
    surfaceVariant = Color(0xFFE2E6E8),
    onSurfaceVariant = Color(0xFF3E5560),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF2F4F5),
    surfaceContainer = Color(0xFFECEFF1),
    surfaceContainerHigh = Color(0xFFE2E6E8),
    surfaceContainerHighest = Color(0xFFD6DCE0),
    outline = Color(0xFF5B6E77),
    outlineVariant = Color(0xFF797979),
)

// the same screens at night: the grey goes charcoal and the header and footer go deep blue-grey
private val Dark = darkColorScheme(
    primary = Color(0xFF7FAFC4),
    onPrimary = Color(0xFF00131A),
    primaryContainer = Color(0xFF2B4A5B),
    onPrimaryContainer = Color(0xFFD9E8EF),
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
