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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.sin

/**
 * Model class for horizontal capsule stardust particles.
 */
private data class CapsuleParticle(
    val xRatio: Float,
    val yRatio: Float,
    val speed: Float,
    val size: Float,
    val colorGroup: Int,
    val amplitudeOffset: Float
)

/**
 * Premium, voice-amplitude-reactive, particle-streamed Gemini Live Capsule.
 * Houses 220 horizontal stardust particles flowing like a cosmic river, beautifully synchronized
 * with real-time vocal amplitude dynamics.
 */
@Composable
fun GeminiLiveGlowingCapsule(
    modifier: Modifier = Modifier,
    width: Dp = 110.dp,
    height: Dp = 52.dp,
    isActive: Boolean = true,
    isSpeaking: Boolean = false,
    amplitude: Float = 0f, // real-time voice amplitude (0f to 1f)
    onClick: (() -> Unit)? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "gemini_capsule_flux")

    // Core particle flow progress animation
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isSpeaking) 1500 else 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "capsule_wave_phase"
    )

    // Base fluid height pulsating gently
    val basePulseHeight by infiniteTransition.animateFloat(
        initialValue = 0.38f,
        targetValue = 0.44f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "capsule_base_height"
    )

    // Modulate liquid level directly with live amplitude for 100% natural responsiveness
    val liquidHeightRatio = (basePulseHeight + amplitude * 0.45f).coerceIn(0.15f, 0.95f)

    // Generate 220 premium stardust stream coordinates
    val totalParticles = 220
    val particles = remember {
        (0 until totalParticles).map { i ->
            val xRatio = (i % 25) / 25f + (i * 0.007f) % 0.04f
            val yRatio = 0.15f + (i % 11) * 0.07f // spread evenly across the vertical capsule space
            val speed = 0.35f + (i % 5) * 0.15f
            val size = 0.5f + (i % 4) * 0.45f
            val colorGroup = i % 4
            val amplitudeOffset = (i * 0.08f).toFloat()

            CapsuleParticle(
                xRatio = xRatio,
                yRatio = yRatio,
                speed = speed,
                size = size,
                colorGroup = colorGroup,
                amplitudeOffset = amplitudeOffset
            )
        }
    }

    Box(
        modifier = modifier
            .width(width)
            .height(height),
        contentAlignment = Alignment.Center
    ) {
        // 1. Slightly larger outer overlay shadow / glowing ring boundary
        Canvas(modifier = Modifier.size(width = width + 8.dp, height = height + 8.dp)) {
            val glowRadius = size.width / 2f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x2E4285F4),
                        Color(0x0E60A5FA),
                        Color.Transparent
                    ),
                    center = Offset(size.width / 2f, size.height * 0.5f),
                    radius = glowRadius
                ),
                radius = glowRadius,
                center = Offset(size.width / 2f, size.height * 0.5f)
            )
        }

        // 2. High-Tech Particle Capsule surface
        Surface(
            onClick = { onClick?.invoke() },
            shape = CircleShape,
            color = Color(0xFF0F172A), // Premium dark navy backdrop to let the stardust pop brilliantly
            border = BorderStroke(1.2.dp, Color(0xFF334155)),
            shadowElevation = 3.dp,
            modifier = Modifier
                .width(width - 4.dp)
                .height(height - 4.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cornerRadiusPx = size.height / 2f
                    val capsulePath = Path().apply {
                        addRoundRect(
                            RoundRect(
                                left = 0f,
                                top = 0f,
                                right = size.width,
                                bottom = size.height,
                                cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
                            )
                        )
                    }

                    clipPath(capsulePath) {
                        // Background gradient (Deep cosmic navy to dark slate)
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B)),
                                start = Offset(0f, 0f),
                                end = Offset(size.width, size.height)
                            )
                        )

                        // Ambient glowing celestial nebula core
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0x2A38BDF8),
                                    Color(0x120284C7),
                                    Color.Transparent
                                ),
                                center = Offset(size.width / 2f, size.height / 2f),
                                radius = size.width * 0.65f
                            ),
                            radius = size.width * 0.65f,
                            center = Offset(size.width / 2f, size.height / 2f)
                        )

                        if (isActive) {
                            // Render flowing stardust stream
                            particles.forEach { p ->
                                // Horizontal motion phase calculation
                                val currentXRatio = (p.xRatio + (wavePhase / (2 * Math.PI).toFloat()) * p.speed) % 1.0f
                                val px = currentXRatio * size.width

                                // Dynamic vocal vertical wave modulation
                                val waveFactor = sin(currentXRatio * 2.2 * Math.PI + wavePhase + p.amplitudeOffset).toFloat()
                                // Vertical displacement increases with speech amplitude
                                val maxDisplacement = 4.5f + amplitude * 14f
                                val py = p.yRatio * size.height + waveFactor * maxDisplacement

                                // Soft horizontal edge fade to prevent sharp clipping
                                val edgeFade = (currentXRatio * (1f - currentXRatio) * 4f).coerceIn(0f, 1f)
                                val baseAlpha = when (p.colorGroup) {
                                    0 -> 0.90f  // Bright white
                                    1 -> 0.78f  // Ice blue
                                    2 -> 0.65f  // Cyan
                                    else -> 0.45f // Sky blue
                                }
                                val finalAlpha = baseAlpha * edgeFade * (if (isSpeaking) 1.0f else 0.72f)

                                val particleRadius = p.size.dp.toPx() * (0.8f + amplitude * 0.5f)
                                val color = when (p.colorGroup) {
                                    0 -> Color(0xFFFFFFFF).copy(alpha = finalAlpha)
                                    1 -> Color(0xFFE0F2FE).copy(alpha = finalAlpha)
                                    2 -> Color(0xFF38BDF8).copy(alpha = finalAlpha)
                                    else -> Color(0xFF60A5FA).copy(alpha = finalAlpha)
                                }

                                drawCircle(
                                    color = color,
                                    radius = particleRadius,
                                    center = Offset(px, py)
                                )
                            }

                            // Vocal energy hot spot / highlight glow at the center
                            val coreGlowAlpha = (0.28f + amplitude * 0.45f).coerceIn(0.1f, 0.85f)
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFFE0F2FE).copy(alpha = coreGlowAlpha * 0.8f),
                                        Color(0xFF0284C7).copy(alpha = coreGlowAlpha * 0.3f),
                                        Color.Transparent
                                    ),
                                    center = Offset(size.width / 2f, size.height / 2f),
                                    radius = size.width * 0.38f
                                ),
                                radius = size.width * 0.38f,
                                center = Offset(size.width / 2f, size.height / 2f)
                            )
                        }
                    }
                }
            }
        }
    }
}
