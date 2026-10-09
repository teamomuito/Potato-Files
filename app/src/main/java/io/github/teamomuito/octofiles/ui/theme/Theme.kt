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
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.teamomuito.octofiles.R

private val Light = lightColorScheme(
    primary = Color(0xFFA86F00),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE9A6),
    onPrimaryContainer = Color(0xFF4A3500),
    secondary = Color(0xFF8A6A12),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFF0C2),
    onSecondaryContainer = Color(0xFF3D2C05),
    tertiary = Color(0xFF6B7A1F),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEEF5C2),
    onTertiaryContainer = Color(0xFF2A3305),
    background = Color(0xFFFFFBEA),
    onBackground = Color(0xFF3B2E10),
    surface = Color(0xFFFFFBEA),
    onSurface = Color(0xFF3B2E10),
    surfaceVariant = Color(0xFFFFF1C4),
    onSurfaceVariant = Color(0xFF7A6336),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFFF6D6),
    surfaceContainer = Color(0xFFFFF1C4),
    surfaceContainerHigh = Color(0xFFFFEBB0),
    surfaceContainerHighest = Color(0xFFFFE49A),
    outline = Color(0xFFD8B563),
    outlineVariant = Color(0xFFF3E2A8),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFFFD54A),
    onPrimary = Color(0xFF3B2A00),
    primaryContainer = Color(0xFF5C4300),
    onPrimaryContainer = Color(0xFFFFE9A6),
    secondary = Color(0xFFE8D08A),
    onSecondary = Color(0xFF3B2C05),
    secondaryContainer = Color(0xFF4A3A0C),
    onSecondaryContainer = Color(0xFFFFF0C2),
    tertiary = Color(0xFFC6D878),
    onTertiary = Color(0xFF0D3B30),
    tertiaryContainer = Color(0xFF3A4212),
    onTertiaryContainer = Color(0xFFEEF5C2),
    background = Color(0xFF1F1A10),
    onBackground = Color(0xFFF8EDD0),
    surface = Color(0xFF1F1A10),
    onSurface = Color(0xFFF8EDD0),
    surfaceVariant = Color(0xFF3A3220),
    onSurfaceVariant = Color(0xFFD9C98F),
    surfaceContainerLowest = Color(0xFF171208),
    surfaceContainerLow = Color(0xFF27200F),
    surfaceContainer = Color(0xFF302A15),
    surfaceContainerHigh = Color(0xFF3A3319),
    surfaceContainerHighest = Color(0xFF443D22),
    outline = Color(0xFF8E7F50),
    outlineVariant = Color(0xFF4E4424),
)

/** Colors for the little kind labels. Pastel on purpose, they sit on top of screenshots. */
object Pastel {
    val lavender = Color(0xFFFFF0C2)
    val lavenderInk = Color(0xFF6E5200)
    val sky = Color(0xFFFFF6D6)
    val skyInk = Color(0xFF7A5B00)
    val mint = Color(0xFFFFE9A6)
    val mintInk = Color(0xFF6E4A00)
    val butter = Color(0xFFFFE28A)
    val butterInk = Color(0xFF4A3500)
}

val Sniglet = FontFamily(
    Font(R.font.sniglet_regular, FontWeight.Normal),
    Font(R.font.sniglet_extrabold, FontWeight.ExtraBold),
)

private val base = Typography()

private val OctoType = Typography(
    displaySmall = base.displaySmall.copy(fontFamily = Sniglet, fontWeight = FontWeight.ExtraBold),
    headlineMedium = base.headlineMedium.copy(fontFamily = Sniglet, fontWeight = FontWeight.ExtraBold),
    headlineSmall = base.headlineSmall.copy(fontFamily = Sniglet, fontWeight = FontWeight.ExtraBold),
    titleLarge = base.titleLarge.copy(fontFamily = Sniglet, fontWeight = FontWeight.ExtraBold),
    titleMedium = base.titleMedium.copy(fontFamily = Sniglet, fontWeight = FontWeight.Normal, fontSize = 17.sp),
    labelLarge = base.labelLarge.copy(fontFamily = Sniglet, fontWeight = FontWeight.Normal, fontSize = 15.sp),
    labelMedium = base.labelMedium.copy(fontFamily = Sniglet, fontWeight = FontWeight.Normal),
    labelSmall = base.labelSmall.copy(fontFamily = Sniglet, fontWeight = FontWeight.Normal, fontSize = 11.sp),
)

private val OctoShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

@Composable
fun OctoTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val colors = if (dark) Dark else Light
    MaterialTheme(colorScheme = colors, typography = OctoType, shapes = OctoShapes) {
        // text drawn straight on the background needs a color too, not just text inside cards
        CompositionLocalProvider(
            LocalContentColor provides colors.onBackground,
            LocalGlass provides if (dark) DarkGlass else LightGlass,
            content = content,
        )
    }
}
