package io.github.teamomuito.octofiles.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** A window: a solid fill with a thin grey edge. Nothing is see-through. */
@Immutable
data class Panel(
    val fill: Color,
    val rim: Color,
    val dark: Boolean,
)

val LightPanel = Panel(fill = Color(0xFFF8F8F8), rim = Color(0xFF0C0E0D), dark = false)

val DarkPanel = Panel(fill = Color(0xFF3A4449), rim = Color(0xFF0E1416), dark = true)

val LocalPanel = staticCompositionLocalOf { LightPanel }

/** How much room the bottom bar takes, so lists can scroll clear of it. */
val LocalBarSpace = compositionLocalOf { 0.dp }

@Composable
fun bottomSpace(): Dp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + LocalBarSpace.current

/** A panel's fill and edge. [tint] goes over the fill, so the panel stays solid underneath. */
fun Modifier.panel(shape: Shape, panel: Panel, tint: Color? = null): Modifier =
    clip(shape)
        .background(panel.fill)
        .then(if (tint != null) Modifier.background(tint) else Modifier)
        .border(1.dp, panel.rim, shape)

/** Clickable that dips a little while pressed. */
@Composable
private fun Modifier.applySquish(enabled: Boolean, onClick: () -> Unit): Modifier {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, spring(dampingRatio = 0.6f, stiffness = 600f), label = "squish")
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }.clickable(interactionSource = source, indication = null, enabled = enabled, onClick = onClick)
}

@Composable
fun Squishy(modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(modifier.applySquish(enabled, onClick), contentAlignment = Alignment.Center) { content() }
}

/** A panel. Pass [onClick] and it dips when pressed. */
@Composable
fun PanelCard(
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
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

/** The pale grid the DS menus sit on. Static, so nothing redraws while scrolling. */
@Composable
fun GridBackground(modifier: Modifier = Modifier) {
    val panel = LocalPanel.current
    val base = MaterialTheme.colorScheme.background
    val line = if (panel.dark) Color(0xFF353C41) else Color(0xFFB4B6B5)
    Box(
        modifier
            .fillMaxSize()
            .drawWithCache {
                val step = 8.dp.toPx()
                val width = size.width
                val height = size.height
                onDrawBehind {
                    drawRect(base)
                    var x = 0f
                    while (x <= width) {
                        drawLine(line, Offset(x, 0f), Offset(x, height), strokeWidth = 1.dp.toPx())
                        x += step
                    }
                    var y = 0f
                    while (y <= height) {
                        drawLine(line, Offset(0f, y), Offset(width, y), strokeWidth = 1.dp.toPx())
                        y += step
                    }
                }
            },
    )
}

data class TabItem(val label: String, val icon: ImageVector)

/**
 * The title strip across the top, like the DS's header bar. It takes the status bar itself,
 * so screens below it don't pad for the status bar a second time.
 */
@Composable
fun DsHeader(title: String, modifier: Modifier = Modifier) {
    val dark = LocalPanel.current.dark
    val ink = if (dark) Color(0xFFE6EEF2) else Color(0xFF00131A)
    val edge = if (dark) Color(0xFF0E1416) else Color(0xFF797979)
    val fill = Brush.verticalGradient(
        if (dark) listOf(Color(0xFF4A5C68), Color(0xFF34434D)) else listOf(Color(0xFFC2D6DF), Color(0xFFA2B9C1)),
    )
    Box(
        modifier
            .fillMaxWidth()
            .background(fill)
            .statusBarsPadding()
            .height(HEADER_HEIGHT)
            .drawWithContent {
                drawContent()
                drawLine(edge, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = ink)
    }
}

/**
 * The bottom bar, like the DS's footer: a steel-blue gradient with a dark top edge, one flat
 * cell per tab, and the selected cell lit.
 */
@Composable
fun DsTabBar(tabs: List<TabItem>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val dark = LocalPanel.current.dark
    val edge = if (dark) Color(0xFF0A1820) else Color(0xFF00111B)
    val bar = Brush.verticalGradient(
        if (dark) listOf(Color(0xFF3A5E74), Color(0xFF26404F)) else listOf(Color(0xFF4D7D94), Color(0xFFC3D4DC)),
    )
    val lit = Color(0x4000131A)
    Box(
        modifier
            .fillMaxWidth()
            .background(bar)
            .navigationBarsPadding()
            .drawWithContent {
                drawContent()
                drawLine(edge, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1.dp.toPx())
            },
    ) {
        Row(Modifier.fillMaxWidth().height(BAR_HEIGHT)) {
            tabs.forEachIndexed { i, tab ->
                val on = i == selected
                val tint = Color.White
                Squishy(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(if (on) lit else Color.Transparent),
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

/** Height of the bottom bar, and the room lists keep clear of it. */
val BAR_HEIGHT = 56.dp
val BAR_SPACE = BAR_HEIGHT + 8.dp

/** The title strip, not counting the status bar. */
val HEADER_HEIGHT = 48.dp

/** A round button, for the swipe card controls. */
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
            .panel(CircleShape, panel, tint.copy(alpha = if (panel.dark) 0.35f else 0.75f)),
        enabled = enabled,
        onClick = onClick,
    ) {
        Icon(icon, contentDescription = description, tint = ink, modifier = Modifier.size(size * 0.42f))
    }
}

/** Four corner brackets around a selected item, like the cursor on the DS menus. */
fun Modifier.selectionBrackets(color: Color, arm: Dp = 10.dp, stroke: Dp = 2.dp): Modifier = drawWithContent {
    drawContent()
    val a = arm.toPx()
    val s = stroke.toPx()
    val half = s / 2
    val w = size.width
    val h = size.height
    drawLine(color, Offset(half, half), Offset(a, half), strokeWidth = s)
    drawLine(color, Offset(half, half), Offset(half, a), strokeWidth = s)
    drawLine(color, Offset(w - half, half), Offset(w - a, half), strokeWidth = s)
    drawLine(color, Offset(w - half, half), Offset(w - half, a), strokeWidth = s)
    drawLine(color, Offset(half, h - half), Offset(a, h - half), strokeWidth = s)
    drawLine(color, Offset(half, h - half), Offset(half, h - a), strokeWidth = s)
    drawLine(color, Offset(w - half, h - half), Offset(w - a, h - half), strokeWidth = s)
    drawLine(color, Offset(w - half, h - half), Offset(w - half, h - a), strokeWidth = s)
}

/** A folder or a file, drawn in outline like the DS icons. Drawn here so it needs no icon set. */
@Composable
fun DsGlyph(folder: Boolean, modifier: Modifier = Modifier) {
    val ink = MaterialTheme.colorScheme.onSurface
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val s = 2.dp.toPx()
        if (folder) {
            drawRect(ink, Offset(w * 0.08f, h * 0.12f), Size(w * 0.36f, h * 0.16f), style = Stroke(s))
            drawRect(ink, Offset(w * 0.08f, h * 0.24f), Size(w * 0.84f, h * 0.62f), style = Stroke(s))
        } else {
            val page = Path().apply {
                moveTo(w * 0.22f, h * 0.10f)
                lineTo(w * 0.62f, h * 0.10f)
                lineTo(w * 0.80f, h * 0.28f)
                lineTo(w * 0.80f, h * 0.90f)
                lineTo(w * 0.22f, h * 0.90f)
                close()
            }
            drawPath(page, ink, style = Stroke(s))
            for (i in 0..2) {
                val y = h * (0.44f + i * 0.14f)
                drawLine(ink, Offset(w * 0.34f, y), Offset(w * 0.68f, y), strokeWidth = s)
            }
        }
    }
}
