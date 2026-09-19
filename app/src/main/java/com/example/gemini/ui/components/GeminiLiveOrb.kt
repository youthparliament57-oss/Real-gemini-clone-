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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Model class representing properties of individual stardust particles.
 */
private data class ParticleData(
    val x: Float,
    val y: Float,
    val z: Float,
    val speedFactor: Float,
    val baseSize: Float,
    val colorGroup: Int,
    val planeOffset: Float,
    val phaseOffset: Float
)

/**
 * High-Fidelity Neural Stardust Particle Orb.
 * Recreates the ultra-dense, organic particle simulation from Screenshot 10:
 * - 600 individual 3D simulated stardust coordinates arranged in an organic spherical field.
 * - Live differential Y-axis rotation (Saturn-like differential velocities for organic turbulence).
 * - Full 3D depth sorting (occlusion) rendering front-most particles larger and brighter, back-most fainter.
 * - Dynamic acoustic particle vibrations and outwards expulsion mapped directly to real-time voice amplitude.
 * - Hyper-realistic, tiny high-density particle visuals with soft glowing ambient core.
 */
@Composable
fun GeminiLiveOrb(
    modifier: Modifier = Modifier,
    size: Dp = 210.dp,
    isActive: Boolean = true,
    isSpeaking: Boolean = false,
    amplitude: Float = 0f, // real-time voice amplitude (0f to 1f)
    particleCount: Int = 600,
    baseParticleSizeMultiplier: Float = 1.0f,
    onClick: (() -> Unit)? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "gemini_orb_stardust")

    // Breathing pulse scale matching voice activity
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = if (isSpeaking) 0.94f else 0.97f,
        targetValue = if (isSpeaking) 1.05f else 1.01f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isSpeaking) 750 else 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_breath_stardust"
    )

    // Orbital angle of rotation
    val orbitalAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isSpeaking) 5000 else 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orb_rot_orbital"
    )

    // Secondary wave/turbulent phase factor
    val turbulencePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isSpeaking) 1000 else 2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orb_turbulence"
    )

    // Golden ratio spherical particle generation (premium stardust coordinates)
    val particles = remember(particleCount) {
        (0 until particleCount).map { i ->
            val theta = (i * 137.5) * (Math.PI / 180.0) // Golden angle distribution
            val y = 1.0 - (i / (particleCount - 1).toDouble()) * 2.0 // Y-axis projection
            val radiusAtY = sqrt((1.0 - y * y).coerceAtLeast(0.0))
            val x = cos(theta) * radiusAtY
            val z = sin(theta) * radiusAtY

            // Speed, size and inclination variation for organic flow
            val speedFactor = 0.5f + (i % 6) * 0.15f
            val baseSize = 0.35f + (i % 5) * 0.22f // small stardust radius scale (dp)
            val colorGroup = i % 4 // 0 = bright white, 1 = cyan/ice blue, 2 = cool grey, 3 = pale sky blue
            val orbitalPlaneOffset = (i % 12 - 6) * 0.04f // slight vertical wiggle incline
            val phaseOffset = (i * 0.035f).toFloat()

            ParticleData(
                x = x.toFloat(),
                y = y.toFloat(),
                z = z.toFloat(),
                speedFactor = speedFactor,
                baseSize = baseSize,
                colorGroup = colorGroup,
                planeOffset = orbitalPlaneOffset,
                phaseOffset = phaseOffset
            )
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
            // Real-time vocal expansion
            val expansion = (1f + amplitude * 0.22f)
            val baseRadius = (this.size.minDimension / 2f) * 0.78f * breathingScale * expansion

            // 1. Ultra-soft background radial glow to blend particles
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x2B38BDF8),
                        Color(0x131D4ED8),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 1.35f
                ),
                radius = baseRadius * 1.35f,
                center = center
            )

            // 2. Real-time Acoustic Wave Outer Ripple (Screenshot 10 - extremely thin and faint)
            if (isActive) {
                val rippleRadius1 = baseRadius * (0.85f + (turbulencePhase / (2 * Math.PI).toFloat()) * 0.4f)
                val rippleAlpha1 = (1f - (turbulencePhase / (2 * Math.PI).toFloat())) * 0.15f
                drawCircle(
                    color = Color(0xFF60A5FA).copy(alpha = rippleAlpha1),
                    radius = rippleRadius1,
                    center = center,
                    style = Stroke(width = 0.8.dp.toPx())
                )
            }

            // 3. Process, Project, and Depth-Sort Particles in 3D Space
            val angleRad = Math.toRadians(orbitalAngle.toDouble())

            val projectedParticles = particles.map { p ->
                // Apply individual differential rotation (different speeds create shear and galaxy-like spiral flow)
                val individualAngle = angleRad * p.speedFactor + p.phaseOffset
                val cosA = cos(individualAngle).toFloat()
                val sinA = sin(individualAngle).toFloat()

                // Rotate around Y-axis
                val rx = p.x * cosA - p.z * sinA
                val rz = p.x * sinA + p.z * cosA
                // Add minor orbital plane wobble
                val ry = p.y + p.planeOffset * sin(turbulencePhase + p.phaseOffset)

                // Perspective projection mapping
                val depth = (rz + 1.2f) / 2.4f // Normalize depth range (0 = far, 1 = near)
                
                // Scale distance based on depth and voice amplitude vibration
                val audioVibration = if (amplitude > 0.02f) {
                    val noise = sin(turbulencePhase * 6f + p.phaseOffset) * amplitude * 18f
                    noise
                } else 0f

                val distanceMultiplier = 0.88f + audioVibration / baseRadius
                val screenX = center.x + rx * baseRadius * distanceMultiplier
                val screenY = center.y + ry * baseRadius * distanceMultiplier

                // Dynamic particle radius and transparency depending on depth (3D Occlusion)
                val particleRadius = (p.baseSize.dp.toPx() * (0.4f + depth * 1.2f) * baseParticleSizeMultiplier)
                val baseAlpha = when (p.colorGroup) {
                    0 -> 0.85f  // Bright white
                    1 -> 0.70f  // Cyan
                    2 -> 0.40f  // Cool grey
                    else -> 0.65f // Sky blue
                }
                val finalAlpha = baseAlpha * (0.25f + depth * 0.75f) * (if (isSpeaking) 1.0f else 0.78f)

                val color = when (p.colorGroup) {
                    0 -> Color(0xFFFFFFFF).copy(alpha = finalAlpha)
                    1 -> Color(0xFFE0F2FE).copy(alpha = finalAlpha) // Ice blue
                    2 -> Color(0xFF94A3B8).copy(alpha = finalAlpha) // Slate/stardust grey
                    else -> Color(0xFF38BDF8).copy(alpha = finalAlpha) // Cyan sky
                }

                Triple(Offset(screenX, screenY), particleRadius, color) to rz
            }

            // Sort particles by Z-depth (from back to front) to prevent rendering overlapping errors
            val sortedParticles = projectedParticles.sortedBy { it.second }

            // Draw the sorted stardust particles
            sortedParticles.forEach { (particleProps, _) ->
                val (offset, radius, color) = particleProps
                drawCircle(
                    color = color,
                    radius = radius,
                    center = offset
                )
            }

            // 4. Subtle glowing stellar core inside the stardust field
            val coreAlpha = (0.35f + amplitude * 0.25f).coerceIn(0.1f, 0.7f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFE0F2FE).copy(alpha = coreAlpha * 0.8f),
                        Color(0xFF0284C7).copy(alpha = coreAlpha * 0.4f),
                        Color(0x000284C7)
                    ),
                    center = center,
                    radius = baseRadius * 0.45f
                ),
                radius = baseRadius * 0.45f,
                center = center
            )
        }
    }
}
