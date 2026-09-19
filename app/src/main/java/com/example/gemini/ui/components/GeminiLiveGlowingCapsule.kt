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
 * Premium, voice-amplitude-reactive, double-layered Gemini Live Fluid Capsule.
 * Ensures perfectly circular bounding box ripple (0% square shadow) by delegating click to the rounded Surface:
 * - Beautiful outer glowing protective boundary overlay (slightly larger)
 * - Smaller high-resolution core interactive capsule
 * - Dynamic undulating multi-wave fluid whose height & speed are directly modulated by voice amplitude
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

    // Core wave phase animation
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isSpeaking) 1400 else 2800, easing = LinearEasing),
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

    Box(
        modifier = modifier
            .width(width)
            .height(height),
        contentAlignment = Alignment.Center
    ) {
        // 1. Slightly larger outer overlay shadow / glowing ring boundary
        Canvas(modifier = Modifier.size(width = width + 8.dp, height = height + 8.dp)) {
            val cornerRadiusPx = (size.height / 2f)
            val glowRadius = size.width / 2f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x224285F4),
                        Color(0x0D60A5FA),
                        Color.Transparent
                    ),
                    center = Offset(size.width / 2f, size.height * 0.65f),
                    radius = glowRadius
                ),
                radius = glowRadius,
                center = Offset(size.width / 2f, size.height * 0.65f)
            )
        }

        // 2. Original inner button (made slightly smaller & elegant for perfect contrast)
        Surface(
            onClick = { onClick?.invoke() },
            shape = CircleShape,
            color = Color.White,
            border = BorderStroke(1.2.dp, Color(0xFFE2E8F0)),
            shadowElevation = 2.dp,
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
                        // Background base layer
                        drawRect(color = Color(0xFFFFFFFF))

                        if (isActive) {
                            // First wave layer (Slower, background)
                            val wave1Path = Path().apply {
                                val baseHeight = size.height * (1f - liquidHeightRatio)
                                moveTo(0f, size.height)
                                lineTo(0f, baseHeight)

                                val steps = 24
                                val stepWidth = size.width / steps
                                for (i in 0..steps) {
                                    val x = i * stepWidth
                                    val normalizedX = x / size.width
                                    // Make wave frequency and height responsive to speech
                                    val speechMod = 1f + amplitude * 1.5f
                                    val waveVal = sin(normalizedX * 2 * Math.PI + wavePhase * speechMod).toFloat() * (4f + amplitude * 6f)
                                    lineTo(x, baseHeight + waveVal)
                                }

                                lineTo(size.width, size.height)
                                close()
                            }

                            drawPath(
                                path = wave1Path,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0x2260A5FA),
                                        Color(0x6638BDF8),
                                        Color(0xAA2563EB)
                                    ),
                                    startY = size.height * 0.25f,
                                    endY = size.height
                                )
                            )

                            // Second wave layer (Faster, dense foreground)
                            val wave2Path = Path().apply {
                                val baseHeight = size.height * (1f - liquidHeightRatio * 0.95f)
                                moveTo(0f, size.height)
                                lineTo(0f, baseHeight)

                                val steps = 24
                                val stepWidth = size.width / steps
                                for (i in 0..steps) {
                                    val x = i * stepWidth
                                    val normalizedX = x / size.width
                                    val speechMod = 1.3f + amplitude * 2.0f
                                    val waveVal = sin(normalizedX * 3.5 * Math.PI - wavePhase * speechMod).toFloat() * (3f + amplitude * 5f)
                                    lineTo(x, baseHeight + waveVal)
                                }

                                lineTo(size.width, size.height)
                                close()
                            }

                            drawPath(
                                path = wave2Path,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0x0038BDF8),
                                        Color(0x8838BDF8),
                                        Color(0xDD2563EB),
                                        Color(0xFF1D4ED8)
                                    ),
                                    startY = size.height * 0.35f,
                                    endY = size.height
                                )
                            )

                            // Premium inner highlight glow core
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFFBAE6FD).copy(alpha = 0.9f),
                                        Color(0xFF60A5FA).copy(alpha = 0.6f),
                                        Color.Transparent
                                    ),
                                    center = Offset(size.width / 2f + sin(wavePhase) * 8f, size.height * 0.82f),
                                    radius = size.width * 0.42f
                                ),
                                center = Offset(size.width / 2f + sin(wavePhase) * 8f, size.height * 0.82f),
                                radius = size.width * 0.42f
                            )
                        }
                    }
                }
            }
        }
    }
}
