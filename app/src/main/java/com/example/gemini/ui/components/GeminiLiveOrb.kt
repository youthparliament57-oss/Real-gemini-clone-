package com.example.gemini.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Highly dense, ultra-premium immersive interactive Gemini Live Orb.
 * Directly modeled after professional 3D energy particle spheres and live neural fields:
 * - Extremely dense rotating 3D spherical stardust particle field (100 distinct spatial vertices)
 * - Multi-layer harmonic fluid plasma loops whose height, frequency, and turbulence are fully voice-amplitude modulated
 * - Multi-stage concentric acoustic ripple expansion
 * - 100% vector-rendered inside Compose Canvas
 */
@Composable
fun GeminiLiveOrb(
    modifier: Modifier = Modifier,
    size: Dp = 190.dp,
    isActive: Boolean = true,
    isSpeaking: Boolean = false,
    amplitude: Float = 0f, // real-time voice amplitude (0f to 1f)
    onClick: (() -> Unit)? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "gemini_orb_dense")

    // Breathing scale, amplified by active speaking and amplitude levels
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = if (isSpeaking) 0.92f else 0.95f,
        targetValue = if (isSpeaking) 1.06f else 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isSpeaking) 800 else 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_breath_dense"
    )

    // Primary rotation velocity
    val rotationPrimary by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isSpeaking) 4000 else 10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orb_rot_primary"
    )

    // Secondary reverse counter rotation
    val rotationSecondary by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isSpeaking) 3000 else 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orb_rot_secondary"
    )

    // Fluid wave dynamics phase
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isSpeaking) 1200 else 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orb_phase_dense"
    )

    // Continuous acoustic ripple progress
    val ringPulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isSpeaking) 1000 else 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orb_ring_dense"
    )

    // Core energy pulse opacity
    val coreAlpha by infiniteTransition.animateFloat(
        initialValue = if (isSpeaking) 0.78f else 0.55f,
        targetValue = if (isSpeaking) 1.0f else 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isSpeaking) 500 else 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_core_dense"
    )

    // Ultra-dense golden ratio particle stardust coordinates (100 premium coordinates)
    val totalParticles = 100
    val particles = remember {
        (0 until totalParticles).map { i ->
            val theta = (i * 137.5) * (Math.PI / 180.0) // Golden spiral distribution
            val y = 1.0 - (i / (totalParticles - 1).toDouble()) * 2.0 // Sphere height projection
            val radiusAtY = kotlin.math.sqrt((1.0 - y * y).coerceAtLeast(0.0))
            val x = kotlin.math.cos(theta) * radiusAtY
            val z = kotlin.math.sin(theta) * radiusAtY
            Triple(x.toFloat(), y.toFloat(), z.toFloat())
        }
    }

    val clickModifier = if (onClick != null) {
        Modifier
            .clip(CircleShape)
            .clickable { onClick() }
    } else {
        Modifier.clip(CircleShape)
    }

    Box(
        modifier = modifier
            .size(size)
            .then(clickModifier),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            // Real-time voice amplitude expands base radius naturally
            val realAmplitudeMod = (1f + amplitude * 0.18f)
            val baseRadius = (this.size.minDimension / 2f) * 0.74f * breathingScale * realAmplitudeMod

            // 1. Extreme Outermost Ethereal Aurora
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x6638BDF8),
                        Color(0x382563EB),
                        Color(0x121D4ED8),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 1.45f
                ),
                radius = baseRadius * 1.45f,
                center = center
            )

            // 2. Double Expanding Shockwave Rings (Acoustic resonance)
            if (isActive) {
                // Outer ring
                val ring1Radius = baseRadius * (0.85f + ringPulseProgress * 0.55f)
                val ring1Alpha = (1f - ringPulseProgress) * (if (isSpeaking) 0.7f else 0.35f)
                drawCircle(
                    color = Color(0xFF60A5FA).copy(alpha = ring1Alpha),
                    radius = ring1Radius,
                    center = center,
                    style = Stroke(width = 2.dp.toPx() * (1f - ringPulseProgress * 0.5f))
                )

                // Inner ring (interleaved for density)
                val ring2Progress = (ringPulseProgress + 0.5f) % 1f
                val ring2Radius = baseRadius * (0.85f + ring2Progress * 0.55f)
                val ring2Alpha = (1f - ring2Progress) * (if (isSpeaking) 0.5f else 0.25f)
                drawCircle(
                    color = Color(0xFF93C5FD).copy(alpha = ring2Alpha),
                    radius = ring2Radius,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx() * (1f - ring2Progress * 0.5f))
                )
            }

            // 3. Ultra-Dense 3D Particle Stardust Field
            val rotRad = Math.toRadians(rotationPrimary.toDouble())
            val cosRot = kotlin.math.cos(rotRad).toFloat()
            val sinRot = kotlin.math.sin(rotRad).toFloat()

            particles.forEach { (px, py, pz) ->
                // Rotate around Y-axis for true depth feel
                val rx = px * cosRot - pz * sinRot
                val rz = px * sinRot + pz * cosRot
                val ry = py

                // Parallax depth projection math
                val depthFactor = (rz + 1.2f) / 2.4f // Normalize depth range
                val screenX = center.x + rx * baseRadius * 0.90f
                val screenY = center.y + ry * baseRadius * 0.90f
                
                // Real amplitude makes particle stars vibrate and expand outward dynamically
                val vibration = if (amplitude > 0.05f) (sin(wavePhase * 5f + px) * amplitude * 4f) else 0f
                val finalX = screenX + cos(px * 10f) * vibration
                val finalY = screenY + sin(py * 10f) * vibration

                val particleRadius = (1.5.dp.toPx() + depthFactor * 2.5.dp.toPx())
                val particleAlpha = (0.28f + depthFactor * 0.72f) * (if (isSpeaking) 1.0f else 0.75f)

                drawCircle(
                    color = if (depthFactor > 0.65f) Color(0xFFF0F9FF).copy(alpha = particleAlpha)
                    else Color(0xFF38BDF8).copy(alpha = particleAlpha),
                    radius = particleRadius,
                    center = Offset(finalX, finalY)
                )
            }

            // 4. Layered Fluid Plasma Waveforms (Modulated by vocal amplitude)
            rotate(degrees = rotationPrimary, pivot = center) {
                drawPlasmaWave(
                    center = center,
                    radius = baseRadius * 0.92f,
                    wavePhase = wavePhase,
                    wavesCount = 7,
                    amplitude = (if (isSpeaking) 12f else 6f) + amplitude * 18f,
                    strokeColor = Color(0xBB38BDF8),
                    strokeWidth = 2.8.dp.toPx()
                )
            }

            rotate(degrees = rotationSecondary, pivot = center) {
                drawPlasmaWave(
                    center = center,
                    radius = baseRadius * 0.84f,
                    wavePhase = -wavePhase * 1.4f,
                    wavesCount = 5,
                    amplitude = (if (isSpeaking) 14f else 7f) + amplitude * 22f,
                    strokeColor = Color(0xEE60A5FA),
                    strokeWidth = 2.2.dp.toPx()
                )
            }

            // Extra third ultra-thin responsive ring for high density
            rotate(degrees = rotationPrimary * 1.5f, pivot = center) {
                drawPlasmaWave(
                    center = center,
                    radius = baseRadius * 0.76f,
                    wavePhase = wavePhase * 1.8f,
                    wavesCount = 9,
                    amplitude = (if (isSpeaking) 8f else 4f) + amplitude * 12f,
                    strokeColor = Color(0xAAFFFFFF),
                    strokeWidth = 1.5.dp.toPx()
                )
            }

            // 5. Deep Vibrant Spherical Inner Glow
            val finalCoreAlpha = (coreAlpha + amplitude * 0.2f).coerceIn(0f, 1f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF93C5FD).copy(alpha = finalCoreAlpha * 0.90f),
                        Color(0xFF38BDF8).copy(alpha = finalCoreAlpha * 0.70f),
                        Color(0xFF2563EB).copy(alpha = finalCoreAlpha * 0.45f),
                        Color(0xFF1D4ED8).copy(alpha = 0.20f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 0.76f
                ),
                radius = baseRadius * 0.76f,
                center = center
            )

            // 6. Intense White-Hot Luminescent Core
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFFFFF).copy(alpha = finalCoreAlpha),
                        Color(0xFFBAE6FD).copy(alpha = finalCoreAlpha * 0.85f),
                        Color(0xFF38BDF8).copy(alpha = finalCoreAlpha * 0.35f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 0.40f
                ),
                radius = baseRadius * 0.40f,
                center = center
            )
        }
    }
}

/**
 * Draws an organic harmonic sinusoidal fluid closed loop with customized aesthetics.
 */
private fun DrawScope.drawPlasmaWave(
    center: Offset,
    radius: Float,
    wavePhase: Float,
    wavesCount: Int,
    amplitude: Float,
    strokeColor: Color,
    strokeWidth: Float
) {
    val path = Path()
    val steps = 96 // Increased steps for supreme precision line tracing
    val stepAngle = (2 * Math.PI / steps).toFloat()

    for (i in 0..steps) {
        val angle = i * stepAngle
        val waveMod = sin(angle * wavesCount + wavePhase) * amplitude
        val r = radius + waveMod
        val x = center.x + r * cos(angle)
        val y = center.y + r * sin(angle)

        if (i == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }
    path.close()

    drawPath(
        path = path,
        color = strokeColor,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )
}
