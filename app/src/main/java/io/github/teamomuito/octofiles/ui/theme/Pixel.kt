package io.github.teamomuito.octofiles.ui.theme

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** One pixel of the pixel art. Every rounded corner is built from steps this size. */
private val PIXEL = 3.dp

/** Corners as little staircases, like pixel art. A bigger [radius] takes more steps, up to three. */
fun pixelCorners(radius: Dp): CornerBasedShape = PixelShape(((radius / PIXEL) / 2f).roundToInt().coerceIn(1, 3))

// a CornerBasedShape, because the Material shape theme only accepts those; the corner sizes are unused
private class PixelShape(private val steps: Int) : CornerBasedShape(
    CornerSize(0.dp),
    CornerSize(0.dp),
    CornerSize(0.dp),
    CornerSize(0.dp),
) {
    override fun copy(
        topStart: CornerSize,
        topEnd: CornerSize,
        bottomEnd: CornerSize,
        bottomStart: CornerSize,
    ): CornerBasedShape = PixelShape(steps)

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val n = steps
        val w = size.width
        val h = size.height
        // the steps never eat more than half of a side
        val c = minOf(with(density) { PIXEL.toPx() }, w / (2 * n), h / (2 * n))
        val path = Path().apply {
            // walk the outline clockwise from the left edge: top-left, top-right, bottom-right, bottom-left
            moveTo(0f, n * c)
            for (j in 0 until n) {
                lineTo(j * c, (n - j) * c)
                lineTo((j + 1) * c, (n - j) * c)
                lineTo((j + 1) * c, (n - j - 1) * c)
            }
            for (j in 0 until n) {
                lineTo(w - (n - j) * c, j * c)
                lineTo(w - (n - j) * c, (j + 1) * c)
                lineTo(w - (n - j - 1) * c, (j + 1) * c)
            }
            for (j in n - 1 downTo 0) {
                lineTo(w - (n - j - 1) * c, h - (j + 1) * c)
                lineTo(w - (n - j) * c, h - (j + 1) * c)
                lineTo(w - (n - j) * c, h - j * c)
            }
            for (j in n - 1 downTo 0) {
                lineTo((j + 1) * c, h - (n - j - 1) * c)
                lineTo((j + 1) * c, h - (n - j) * c)
                lineTo(j * c, h - (n - j) * c)
            }
            close()
        }
        return Outline.Generic(path)
    }
}

/**
 * A panel, like the windows and keys on the DS: a solid fill with a navy rim.
 * Nothing is see-through, so the dot grid never shows through a card.
 */
@Immutable
data class Panel(
    val fill: Color,
    val rim: Color,
    val dark: Boolean,
)

val LightPanel = Panel(fill = Color.White, rim = Color(0xFF1C3F7A), dark = false)

val DarkPanel = Panel(fill = Color(0xFF17386B), rim = Color(0xFF5C8FC7), dark = true)

val LocalPanel = staticCompositionLocalOf { LightPanel }

/** How much room the floating tab bar takes at the bottom, so lists can scroll clear of it. */
val LocalBarSpace = compositionLocalOf { 0.dp }

@Composable
fun bottomSpace(): Dp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + LocalBarSpace.current

/** A panel's fill and rim. [tint] goes over the fill, so the panel stays solid underneath. */
fun Modifier.panel(shape: Shape, panel: Panel, tint: Color? = null): Modifier =
    clip(shape)
        .background(panel.fill)
        .then(if (tint != null) Modifier.background(tint) else Modifier)
        .border(2.dp, panel.rim, shape)

/** Clickable that squishes a little while pressed, like a key on the DS. */
@Composable
private fun Modifier.applySquish(enabled: Boolean, onClick: () -> Unit): Modifier {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, spring(dampingRatio = 0.55f, stiffness = 600f), label = "squish")
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }.clickable(interactionSource = source, indication = null, enabled = enabled, onClick = onClick)
}

@Composable
fun Squishy(modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(modifier.applySquish(enabled, onClick), contentAlignment = Alignment.Center) { content() }
}

/** A panel. Pass [onClick] and it squishes when pressed. */
@Composable
fun PanelCard(
    modifier: Modifier = Modifier,
    shape: Shape = pixelCorners(26.dp),
    tint: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val panel = LocalPanel.current
    val base = if (onClick != null) modifier.applySquish(true, onClick) else modifier
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
        Column(base.panel(shape, panel, tint), content = content)
    }
}

/** The DS top screen: a sky-blue field with a grid of pixel dots. Static, so nothing redraws while scrolling. */
@Composable
fun PixelBackground(modifier: Modifier = Modifier) {
    val panel = LocalPanel.current
    val base = MaterialTheme.colorScheme.background
    val dot = if (panel.dark) Color(0xFF3D6FB0) else Color.White
    Box(
        modifier
            .fillMaxSize()
            .drawWithCache {
                val step = 8.dp.toPx()
                // read the size out here: inside buildList, `size` would mean the list's own size
                val width = size.width
                val height = size.height
                val points = buildList {
                    var y = step / 2
                    while (y < height) {
                        var x = step / 2
                        while (x < width) {
                            add(Offset(x, y))
                            x += step
                        }
                        y += step
                    }
                }
                onDrawBehind {
                    drawRect(base)
                    drawPoints(points, PointMode.Points, dot, strokeWidth = 2.dp.toPx(), cap = StrokeCap.Square)
                }
            },
    )
}

data class TabItem(val label: String, val icon: ImageVector)

/**
 * The bottom screen's key row: one panel with a blue key that slides to the selected tab.
 */
@Composable
fun PixelTabBar(tabs: List<TabItem>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val panel = LocalPanel.current
    BoxWithConstraints(
        modifier
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .fillMaxWidth()
            .height(BAR_HEIGHT)
            .panel(pixelCorners(16.dp), panel),
    ) {
        val slot = maxWidth / tabs.size
        val x by animateDpAsState(slot * selected, spring(dampingRatio = 0.7f, stiffness = 420f), label = "tab")
        Box(
            Modifier
                .offset(x = x)
                .width(slot)
                .fillMaxHeight()
                .padding(5.dp)
                .clip(pixelCorners(10.dp))
                .background(MaterialTheme.colorScheme.primary),
        )
        Row(Modifier.fillMaxSize()) {
            tabs.forEachIndexed { i, tab ->
                val on = i == selected
                val tint = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                Squishy(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    onClick = { onSelect(i) },
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(tab.icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
                        Text(tab.label, style = MaterialTheme.typography.labelMedium, color = tint)
                    }
                }
            }
        }
    }
}

/** Height of the bar plus its margins, for [LocalBarSpace]. */
val BAR_HEIGHT = 64.dp
val BAR_SPACE = BAR_HEIGHT + 24.dp

/** A round-ish panel button, for the swipe card controls. */
@Composable
fun PanelCircle(
    icon: ImageVector,
    description: String,
    tint: Color,
    ink: Color,
    size: Dp,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val panel = LocalPanel.current
    Squishy(
        modifier = Modifier
            .size(size)
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f }
            .panel(pixelCorners(size / 2), panel, tint.copy(alpha = if (panel.dark) 0.35f else 0.75f)),
        enabled = enabled,
        onClick = onClick,
    ) {
        Icon(icon, contentDescription = description, tint = ink, modifier = Modifier.size(size * 0.42f))
    }
}
