package io.github.teamomuito.octofiles.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
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

val LightPanel = Panel(fill = Color.White, rim = Color(0xFF7F93A8), dark = false)

val DarkPanel = Panel(fill = Color(0xFF213349), rim = Color(0xFF4A6384), dark = true)

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
    val line = if (panel.dark) Color(0xFF203750) else Color(0xFFCADDEE)
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
 * The bottom bar, like the DS's bottom screen footer: a steel-blue strip along the bottom edge,
 * one flat cell per tab, and the selected cell lit.
 */
@Composable
fun DsTabBar(tabs: List<TabItem>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val dark = LocalPanel.current.dark
    val bar = if (dark) Color(0xFF1E3350) else Color(0xFF4A78AD)
    val lit = if (dark) Color(0xFF3A5E8C) else Color(0xFF7FA6D1)
    Box(modifier.fillMaxWidth().background(bar).navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(BAR_HEIGHT)) {
            tabs.forEachIndexed { i, tab ->
                val on = i == selected
                val tint = if (on) Color.White else Color(0xFFD2E2F3)
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
