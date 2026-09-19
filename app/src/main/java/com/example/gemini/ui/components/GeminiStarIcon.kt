package com.example.gemini.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Google Gemini iconic brand gradient colors
val GeminiBlue = Color(0xFF4285F4)
val GeminiPurple = Color(0xFF9B51E0)
val GeminiCoral = Color(0xFFEA4335)
val GeminiAmber = Color(0xFFFBBC04)
val GeminiCyan = Color(0xFF24C6DC)

val GeminiGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFF4285F4), // Google Blue
        Color(0xFF7B1FA2), // Violet
        Color(0xFFE91E63), // Pink
        Color(0xFFEA4335), // Red/Coral
        Color(0xFFFBBC04)  // Amber Yellow
    ),
    start = Offset(0f, 0f),
    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
)

/**
 * Draws the authentic Google Gemini 4-pointed curved sparkle star.
 */
@Composable
fun GeminiStarIcon(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    isPulsing: Boolean = false
) {
    val scale = if (isPulsing) {
        val infiniteTransition = rememberInfiniteTransition(label = "gemini_pulse")
        val animatedScale by infiniteTransition.animateFloat(
            initialValue = 0.88f,
            targetValue = 1.12f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_scale"
        )
        animatedScale
    } else {
        1f
    }

    Canvas(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        drawGeminiStar(this)
    }
}

fun drawGeminiStar(drawScope: DrawScope) {
    val width = drawScope.size.width
    val height = drawScope.size.height
    val cx = width / 2f
    val cy = height / 2f
    val rx = width / 2f
    val ry = height / 2f

    val path = Path().apply {
        // Top tip
        moveTo(cx, cy - ry)
        // Curve to right tip
        cubicTo(
            cx, cy - ry * 0.28f,
            cx + rx * 0.28f, cy,
            cx + rx, cy
        )
        // Curve to bottom tip
        cubicTo(
            cx + rx * 0.28f, cy,
            cx, cy + ry * 0.28f,
            cx, cy + ry
        )
        // Curve to left tip
        cubicTo(
            cx, cy + ry * 0.28f,
            cx - rx * 0.28f, cy,
            cx - rx, cy
        )
        // Curve back to top tip
        cubicTo(
            cx - rx * 0.28f, cy,
            cx, cy - ry * 0.28f,
            cx, cy - ry
        )
        close()
    }

    val brush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF388BFD),
            Color(0xFF6E40C9),
            Color(0xFFE34C26),
            Color(0xFFF9A825)
        ),
        start = Offset(0f, 0f),
        end = Offset(width, height)
    )

    drawScope.drawPath(path = path, brush = brush)
}
