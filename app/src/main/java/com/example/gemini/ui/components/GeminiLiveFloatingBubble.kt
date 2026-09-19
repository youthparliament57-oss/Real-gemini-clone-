package com.example.gemini.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Highly premium, fully animated immersive Gemini Live Floating Bubble.
 * Packed with "love and feel":
 * - Multi-layer breathing ambient aura
 * - Expanding acoustic ripple rings
 * - Smoothly morphing internal plasma core & rotating stardust particles
 * - Purely circular bounds with a stunning white-hot core
 */
@Composable
fun GeminiLiveFloatingBubble(
    isMuted: Boolean,
    isSpeaking: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bubble_lux")

    // Slow organic breathing pulse
    val breathingPulse by infiniteTransition.animateFloat(
        initialValue = if (isSpeaking) 0.93f else 0.96f,
        targetValue = if (isSpeaking) 1.07f else 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isSpeaking) 800 else 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bubble_breath"
    )

    // Smooth rotation for internal particle stars
    val particleRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bubble_particles"
    )

    // Wave/plasma pulsation level
    val fluidPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bubble_fluid"
    )

    // Concentric acoustic wave ring expanding out of the bubble
    val waveRingProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isSpeaking) 1100 else 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bubble_ring"
    )

    Box(
        modifier = modifier
            .padding(16.dp)
            .size(76.dp)
            .clip(CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(76.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val bubbleRadius = (size.minDimension / 2f) * 0.72f * breathingPulse

            // 1. Ethereal Outer Glow Aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x7738BDF8),
                        Color(0x332563EB),
                        Color(0x101D4ED8),
                        Color.Transparent
                    ),
                    center = center,
                    radius = bubbleRadius * 1.38f
                ),
                radius = bubbleRadius * 1.38f,
                center = center
            )

            // 2. Outward Shockwave Ring
            if (!isMuted) {
                val ringRadius = bubbleRadius * (0.9f + waveRingProgress * 0.48f)
                val ringAlpha = (1f - waveRingProgress) * (if (isSpeaking) 0.65f else 0.35f)
                drawCircle(
                    color = Color(0xFF60A5FA).copy(alpha = ringAlpha),
                    radius = ringRadius,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx() * (1f - waveRingProgress * 0.4f))
                )
            }
        }

        // 3. Main Bubble Body - Double-layered Glass/Plasma design with premium Material 3 elevation
        Surface(
            shape = CircleShape,
            color = Color.White,
            border = BorderStroke(1.5.dp, Color(0xFFE2E8F0)),
            shadowElevation = 6.dp,
            modifier = Modifier.size(54.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Internal high-resolution live gradient
                Canvas(modifier = Modifier.size(54.dp)) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val internalRadius = size.width / 2f

                    // Radial dynamic backdrop
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFFFFF),
                                Color(0xFFF1F5F9),
                                Color(0xFFE2E8F0)
                            ),
                            center = center,
                            radius = internalRadius
                        ),
                        radius = internalRadius
                    )

                    // 4. Immersive Fluid Core Gradient
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF93C5FD).copy(alpha = 0.9f),
                                Color(0xFF38BDF8).copy(alpha = 0.7f),
                                Color(0xFF2563EB).copy(alpha = 0.4f),
                                Color.Transparent
                            ),
                            center = Offset(size.width / 2f, size.height * 0.85f),
                            radius = internalRadius * 0.95f
                        ),
                        radius = internalRadius
                    )

                    // 5. Rotating micro particles
                    rotate(degrees = particleRotation, pivot = center) {
                        val pCount = 8
                        for (i in 0 until pCount) {
                            val angle = (i * (2 * Math.PI / pCount)).toFloat()
                            val dist = internalRadius * 0.52f + sin(fluidPhase + i) * 4f
                            val px = center.x + cos(angle) * dist
                            val py = center.y + sin(angle) * dist
                            drawCircle(
                                color = Color.White.copy(alpha = 0.75f),
                                radius = 1.5.dp.toPx(),
                                center = Offset(px, py)
                            )
                        }
                    }

                    // 6. Glowing core star
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.95f),
                                Color(0xFFBAE6FD).copy(alpha = 0.6f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = internalRadius * 0.38f
                        ),
                        radius = internalRadius * 0.38f
                    )
                }

                // Interactive Mic Icon
                Icon(
                    imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = "Gemini Live Bubble",
                    tint = if (isMuted) Color(0xFFEA4335) else Color(0xFF1E293B),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
