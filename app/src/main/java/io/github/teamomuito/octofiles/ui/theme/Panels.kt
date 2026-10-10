package io.github.teamomuito.octofiles.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** The colors of one surface. The light set is sampled from pictoclip's DS menu. */
@Immutable
data class Panel(
    val fill: Color,
    val rim: Color,
    val shadow: Color,
    val grid: Color,
    val bar: Color,
    val dark: Boolean,
)

val LightPanel = Panel(
    fill = Color.White,
    rim = Color(0xFF4A4F55),
    shadow = Color(0xFFC3C8CD),
    grid = Color(0xFFE1E4E7),
    bar = Color(0xFFE3E6E9),
    dark = false,
)

val DarkPanel = Panel(
    fill = Color(0xFF3A4449),
    rim = Color(0xFF0E1416),
    shadow = Color(0xFF252B2F),
    grid = Color(0xFF353C41),
    bar = Color(0xFF23292D),
    dark = true,
)

val LocalPanel = staticCompositionLocalOf { LightPanel }

/** How much room the bottom bar takes, so lists can scroll clear of it. */
val LocalBarSpace = compositionLocalOf { 0.dp }

@Composable
fun bottomSpace(): Dp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + LocalBarSpace.current

/** The rim width, and the size of the notch at each corner. */
private val EDGE = 2.dp

/** The blue of the selection brackets, and the lit tab. */
val CursorBlue = Color(0xFF3E6DB0)

/**
 * A panel: a 2dp rim cut at the corners, a solid fill inside, and a darker strip along the bottom
 * and right, like the DS buttons. [tint] goes over the fill, so the panel stays solid underneath.
 */
fun Modifier.panel(shape: Shape, panel: Panel, tint: Color? = null): Modifier =
    clip(shape)
        .background(panel.fill)
        .then(if (tint != null) Modifier.background(tint) else Modifier)
        .drawBehind {
            // the strips sit just inside the rim, so the border covers their outer edge
            val edge = EDGE.toPx()
            drawRect(panel.shadow, Offset(edge, size.height - 2 * edge), Size(size.width - 2 * edge, edge))
            drawRect(panel.shadow, Offset(size.width - 2 * edge, edge), Size(edge, size.height - 2 * edge))
        }
        .border(EDGE, panel.rim, shape)

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
    shape: Shape = PanelShape,
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

/** The pale grid the DS menus sit on, in 16dp cells with one-pixel lines. Static, so nothing redraws while scrolling. */
@Composable
fun GridBackground(modifier: Modifier = Modifier) {
    val panel = LocalPanel.current
    val base = MaterialTheme.colorScheme.background
    Box(
        modifier
            .fillMaxSize()
            .drawWithCache {
                val step = 16.dp.toPx()
                val width = size.width
                val height = size.height
                onDrawBehind {
                    drawRect(base)
                    var x = 0f
                    while (x <= width) {
                        drawLine(panel.grid, Offset(x, 0f), Offset(x, height), strokeWidth = 1f)
                        x += step
                    }
                    var y = 0f
                    while (y <= height) {
                        drawLine(panel.grid, Offset(0f, y), Offset(width, y), strokeWidth = 1f)
                        y += step
                    }
                }
            },
    )
}

data class TabItem(val label: String, val icon: ImageVector)

/**
 * The title bar across the top, like the DS menu's header: a slate gradient, the screen name on the
 * left and the clock on the right. It takes the status bar itself, so screens below don't pad for it.
 */
@Composable
fun DsHeader(title: String, modifier: Modifier = Modifier) {
    val dark = LocalPanel.current.dark
    val ink = if (dark) Color(0xFFE6EEF2) else Color.White
    val fill = Brush.verticalGradient(
        if (dark) listOf(Color(0xFF3E525C), Color(0xFF2C3B44)) else listOf(Color(0xFF6F8C99), Color(0xFF4A6573)),
    )
    Box(modifier.fillMaxWidth().background(fill).statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().height(HEADER_HEIGHT).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, color = ink, maxLines = 1)
            DsClock(ink)
        }
    }
}

/** The clock in the header's corner, as on the DS menus. It ticks every ten seconds. */
@Composable
private fun DsClock(color: Color) {
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(10_000)
        }
    }
    val format = remember { SimpleDateFormat("HH:mm  MM/dd", Locale.getDefault()) }
    Text(format.format(Date(now)), style = MaterialTheme.typography.titleSmall, color = color)
}

/**
 * The bottom bar, like the DS button row: a flat pale strip with one notched cell per tab.
 * The lit cell gets the pressed fill and the blue brackets.
 */
@Composable
fun DsTabBar(tabs: List<TabItem>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val panel = LocalPanel.current
    val ink = MaterialTheme.colorScheme.onSurface
    Row(
        modifier
            .fillMaxWidth()
            .background(panel.bar)
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        tabs.forEachIndexed { i, tab ->
            val on = i == selected
            Squishy(
                modifier = Modifier
                    .weight(1f)
                    .height(BAR_CELL)
                    .panel(PanelShape, panel, tint = if (on) MaterialTheme.colorScheme.primaryContainer else null)
                    .then(if (on) Modifier.selectionBrackets() else Modifier),
                onClick = { onSelect(i) },
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(tab.icon, contentDescription = null, tint = ink, modifier = Modifier.size(20.dp))
                    // the pixel face is half as wide as it is tall, so the longest label needs a step smaller to fit a cell
                    Text(tab.label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = ink, maxLines = 1)
                }
            }
        }
    }
}

/** Height of the bottom bar, and the room lists keep clear of it. */
val BAR_CELL = 52.dp
val BAR_HEIGHT = BAR_CELL + 16.dp
val BAR_SPACE = BAR_HEIGHT + 8.dp

/** The title bar, not counting the status bar. */
val HEADER_HEIGHT = 32.dp

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

/** Four blue corner brackets around a selected item, like the cursor on the DS menus. */
fun Modifier.selectionBrackets(
    color: Color = CursorBlue,
    arm: Dp = 14.dp,
    stroke: Dp = 3.dp,
    inset: Dp = 3.dp,
): Modifier = drawWithCache {
    val s = stroke.toPx()
    val a = arm.toPx()
    val i = inset.toPx() + s / 2
    val l = i
    val t = i
    val r = size.width - i
    val b = size.height - i
    val path = Path().apply {
        moveTo(l, t + a); lineTo(l, t); lineTo(l + a, t)
        moveTo(r - a, t); lineTo(r, t); lineTo(r, t + a)
        moveTo(r, b - a); lineTo(r, b); lineTo(r - a, b)
        moveTo(l + a, b); lineTo(l, b); lineTo(l, b - a)
    }
    onDrawWithContent {
        drawContent()
        drawPath(path, color, style = Stroke(width = s))
    }
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
