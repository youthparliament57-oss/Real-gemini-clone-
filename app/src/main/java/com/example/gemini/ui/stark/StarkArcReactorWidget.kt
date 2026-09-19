package com.example.gemini.ui.stark

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun StarkArcReactorWidget(
    isPinned: Boolean = false,
    onPinToggle: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isOvercharged by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "arc_reactor")
    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outer_rotation"
    )
    val innerRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "inner_rotation"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    StarkHolographicCard(
        title = StarkWidgetType.ARC_REACTOR.title,
        subtitle = StarkWidgetType.ARC_REACTOR.subtitle,
        tag = StarkWidgetType.ARC_REACTOR.tag,
        isPinned = isPinned,
        onPinToggle = onPinToggle,
        onClose = onClose,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Rotating Holographic Arc Reactor Core Canvas
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clickable { isOvercharged = !isOvercharged },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(110.dp)) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val maxRadius = size.width / 2f - 4.dp.toPx()

                    // Outer border ring
                    drawCircle(
                        color = StarkColors.CyanDim,
                        radius = maxRadius,
                        center = center,
                        style = Stroke(width = 1.5.dp.toPx())
                    )

                    // Outer segmented teeth (Rotating clockwise)
                    val teethCount = 12
                    for (i in 0 until teethCount) {
                        val angleDeg = (i * (360f / teethCount) + outerRotation) * (Math.PI / 180.0)
                        val startR = maxRadius - 6.dp.toPx()
                        val endR = maxRadius
                        val start = Offset(
                            x = center.x + (startR * cos(angleDeg)).toFloat(),
                            y = center.y + (startR * sin(angleDeg)).toFloat()
                        )
                        val end = Offset(
                            x = center.x + (endR * cos(angleDeg)).toFloat(),
                            y = center.y + (endR * sin(angleDeg)).toFloat()
                        )
                        drawLine(
                            color = StarkColors.Cyan,
                            start = start,
                            end = end,
                            strokeWidth = 2.dp.toPx()
                        )
                    }

                    // Middle dashed ring (Rotating counter-clockwise)
                    drawCircle(
                        color = if (isOvercharged) StarkColors.Gold else StarkColors.Cyan,
                        radius = maxRadius * 0.72f,
                        center = center,
                        style = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 8f), innerRotation)
                        )
                    )

                    // Inner reactor ring
                    drawCircle(
                        color = StarkColors.ElectricBlue.copy(alpha = pulseAlpha),
                        radius = maxRadius * 0.45f,
                        center = center,
                        style = Stroke(width = 3.dp.toPx())
                    )

                    // Center glowing energy orb
                    drawCircle(
                        color = if (isOvercharged) StarkColors.Gold.copy(alpha = pulseAlpha) else StarkColors.Cyan.copy(alpha = pulseAlpha),
                        radius = maxRadius * 0.28f,
                        center = center
                    )
                    drawCircle(
                        color = Color.White,
                        radius = maxRadius * 0.12f,
                        center = center
                    )
                }

                // Center percentage text
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isOvercharged) "100%" else "98.7%",
                        color = StarkColors.DarkVoidOpaque,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Telemetry Columns
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TelemetryRow(label = "CORE OUTPUT", value = if (isOvercharged) "4.8 GJ/s" else "3.2 GJ/s", accent = StarkColors.Cyan)
                TelemetryRow(label = "THERMAL FLUX", value = if (isOvercharged) "62.1 °C" else "38.4 °C", accent = if (isOvercharged) StarkColors.Amber else StarkColors.NeonGreen)
                TelemetryRow(label = "REACTOR COIL", value = "STAGE 4 // STABLE", accent = StarkColors.TextMuted)
                TelemetryRow(label = "AI PROTOCOL", value = "GEMINI-3.5 ONLINE", accent = StarkColors.Gold)

                // Mini Waveform Power Visualizer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    val heights = listOf(14.dp, 22.dp, 8.dp, 26.dp, 18.dp, 30.dp, 12.dp, 20.dp, 16.dp, 28.dp)
                    heights.forEachIndexed { idx, h ->
                        val dynamicFactor = if (idx % 2 == 0) pulseAlpha else (1.5f - pulseAlpha)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(h * dynamicFactor)
                                .clip(RoundedCornerShape(1.dp))
                                .background(if (isOvercharged) StarkColors.Gold else StarkColors.Cyan)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TelemetryRow(label: String, value: String, accent: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = StarkColors.TextDim,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.5.sp
        )
        Text(
            text = value,
            color = accent,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.5.sp
        )
    }
}
