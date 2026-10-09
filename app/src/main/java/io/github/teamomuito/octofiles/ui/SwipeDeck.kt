package io.github.teamomuito.octofiles.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import io.github.teamomuito.octofiles.ui.theme.PanelCircle
import io.github.teamomuito.octofiles.ui.theme.LocalPanel
import io.github.teamomuito.octofiles.ui.theme.Pastel
import io.github.teamomuito.octofiles.ui.theme.pixelCorners
import kotlin.math.abs
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Where the top card is. Kept outside the card so the one underneath can react to it.
 * Shared by both swipe screens so they feel the same.
 */
internal class CardMotion {
    val x = Animatable(0f)
    val y = Animatable(0f)
    var leaving = false

    /** 0 while resting, 1 once it's far enough to count as a swipe. */
    fun progress(width: Float) = (abs(x.value) / (width * THRESHOLD)).coerceIn(0f, 1f)

    suspend fun flyOut(keep: Boolean, width: Float) = coroutineScope {
        launch { y.animateTo(y.value + 80f, tween(230)) }
        x.animateTo(if (keep) width * 1.6f else -width * 1.6f, tween(230))
    }

    suspend fun settle() = coroutineScope {
        launch { y.animateTo(0f, spring(dampingRatio = 0.55f)) }
        x.animateTo(0f, spring(dampingRatio = 0.55f))
    }

    companion object {
        const val THRESHOLD = 0.3f
    }
}

/**
 * The swipe deck both swipe screens use. The top card follows your finger, the one under it
 * grows as you go, and bin / undo / keep sit under the deck. [card] draws one card; the two
 * stamp lambdas say how far it's been dragged, and [onClick] is a tap on it.
 */
@Composable
fun <T> SwipeDeck(
    deck: List<T>,
    keyOf: (T) -> Any,
    canUndo: Boolean,
    onDecide: (T, Boolean) -> Unit,
    onUndo: () -> Unit,
    onOpen: (T) -> Unit,
    card: @Composable (item: T, modifier: Modifier, keepStamp: () -> Float, byeStamp: () -> Float, onClick: () -> Unit) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val top = deck.first()
    val motion = remember(keyOf(top)) { CardMotion() }
    var width by remember { mutableFloatStateOf(1f) }

    fun decide(keep: Boolean) {
        if (motion.leaving) return
        motion.leaving = true
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        scope.launch {
            motion.flyOut(keep, width)
            onDecide(top, keep)
        }
    }

    Column(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .onSizeChanged { width = it.width.toFloat() },
        ) {
            deck.getOrNull(1)?.let { next ->
                key(keyOf(next)) {
                    card(
                        next,
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                val grow = 0.93f + 0.07f * motion.progress(width)
                                scaleX = grow
                                scaleY = grow
                            },
                        { 0f },
                        { 0f },
                        {},
                    )
                }
            }
            key(keyOf(top)) {
                card(
                    top,
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationX = motion.x.value
                            translationY = motion.y.value
                            rotationZ = motion.x.value / 45f
                        }
                        .pointerInput(keyOf(top)) {
                            detectDragGestures(
                                onDragEnd = {
                                    val limit = width * CardMotion.THRESHOLD
                                    when {
                                        motion.x.value > limit -> decide(keep = true)
                                        motion.x.value < -limit -> decide(keep = false)
                                        else -> scope.launch { motion.settle() }
                                    }
                                },
                                onDragCancel = { scope.launch { motion.settle() } },
                            ) { change, drag ->
                                change.consume()
                                scope.launch {
                                    motion.x.snapTo(motion.x.value + drag.x)
                                    motion.y.snapTo(motion.y.value + drag.y)
                                }
                            }
                        },
                    { (motion.x.value / (width * CardMotion.THRESHOLD)).coerceIn(0f, 1f) },
                    { (-motion.x.value / (width * CardMotion.THRESHOLD)).coerceIn(0f, 1f) },
                    { onOpen(top) },
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(22.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, bottom = 16.dp),
        ) {
            PanelCircle(Icons.Rounded.Close, "delete", Color(0xFFFFC9C2), Color(0xFFA3291C), 68.dp) { decide(keep = false) }
            PanelCircle(Icons.Rounded.Refresh, "undo", Color.White, MaterialTheme.colorScheme.onSurfaceVariant, 48.dp, enabled = canUndo, onClick = onUndo)
            PanelCircle(Icons.Rounded.Favorite, "keep", Pastel.mint, Pastel.mintInk, 68.dp) { decide(keep = true) }
        }
    }
}

/**
 * The card frame both swipe screens draw: rounded, shadowed, with the title and details over
 * the bottom of the picture and the keep / bye stamps that fade in as you drag.
 */
@Composable
fun SwipeCard(
    modifier: Modifier,
    title: String,
    details: String,
    keepStamp: () -> Float,
    byeStamp: () -> Float,
    onClick: () -> Unit,
    picture: @Composable BoxScope.() -> Unit,
) {
    val shape = pixelCorners(28.dp)
    Box(
        modifier
            .shadow(10.dp, shape)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(2.dp, LocalPanel.current.rim, shape)
            .clickable(onClick = onClick),
    ) {
        picture()

        Column(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))))
                .padding(start = 20.dp, end = 20.dp, top = 36.dp, bottom = 18.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White, maxLines = 2)
            Text(details, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f))
        }

        Stamp(
            "keep",
            Pastel.mintInk,
            Modifier
                .align(Alignment.TopStart)
                .padding(24.dp)
                .graphicsLayer {
                    alpha = keepStamp()
                    rotationZ = -14f
                },
        )
        Stamp(
            "bye",
            Color(0xFFD9641B),
            Modifier
                .align(Alignment.TopEnd)
                .padding(24.dp)
                .graphicsLayer {
                    alpha = byeStamp()
                    rotationZ = 14f
                },
        )
    }
}

@Composable
private fun Stamp(text: String, color: Color, modifier: Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.headlineMedium,
        color = color,
        modifier = modifier
            .background(Color.White.copy(alpha = 0.85f), pixelCorners(14.dp))
            .border(BorderStroke(3.dp, color), pixelCorners(14.dp))
            .padding(horizontal = 14.dp, vertical = 4.dp),
    )
}
