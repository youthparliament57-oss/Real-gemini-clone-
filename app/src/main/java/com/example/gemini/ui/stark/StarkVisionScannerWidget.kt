package com.example.gemini.ui.stark

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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun StarkVisionScannerWidget(
    isPinned: Boolean = false,
    onPinToggle: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isTargetLocked by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "vision_scanner")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_sweep"
    )
    val reticleExpand by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "reticle_expand"
    )

    StarkHolographicCard(
        title = StarkWidgetType.VISION_SCANNER.title,
        subtitle = StarkWidgetType.VISION_SCANNER.subtitle,
        tag = StarkWidgetType.VISION_SCANNER.tag,
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
            // Radar Reticle Canvas
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clickable { isTargetLocked = !isTargetLocked },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(110.dp)) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val r = (size.width / 2f - 4.dp.toPx()) * reticleExpand

                    // Outer range ring
                    drawCircle(
                        color = StarkColors.CyanDim,
                        radius = r,
                        center = center,
                        style = Stroke(width = 1.dp.toPx())
                    )

                    // Inner range ring
                    drawCircle(
                        color = if (isTargetLocked) StarkColors.DangerRed.copy(alpha = 0.5f) else StarkColors.CyanDim,
                        radius = r * 0.6f,
                        center = center,
                        style = Stroke(width = 1.dp.toPx())
                    )

                    // Crosshair lines
                    drawLine(
                        color = if (isTargetLocked) StarkColors.DangerRed else StarkColors.Cyan,
                        start = Offset(center.x - r, center.y),
                        end = Offset(center.x + r, center.y),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = if (isTargetLocked) StarkColors.DangerRed else StarkColors.Cyan,
                        start = Offset(center.x, center.y - r),
                        end = Offset(center.x, center.y + r),
                        strokeWidth = 1.dp.toPx()
                    )

                    // Rotating radar beam
                    val sweepRad = sweepAngle * (Math.PI / 180.0)
                    val beamEnd = Offset(
                        x = center.x + (r * cos(sweepRad)).toFloat(),
                        y = center.y + (r * sin(sweepRad)).toFloat()
                    )
                    drawLine(
                        color = StarkColors.Cyan,
                        start = center,
                        end = beamEnd,
                        strokeWidth = 2.dp.toPx()
                    )

                    // Target acquisition box
                    val boxSize = 24.dp.toPx()
                    drawRect(
                        color = if (isTargetLocked) StarkColors.DangerRed else StarkColors.Gold,
                        topLeft = Offset(center.x - boxSize / 2f, center.y - boxSize / 2f),
                        size = Size(boxSize, boxSize),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }

                // Center range indicator
                Text(
                    text = if (isTargetLocked) "LOCKED" else "1.85m",
                    color = if (isTargetLocked) StarkColors.DangerRed else StarkColors.Gold,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 36.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Spatial Recon Details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                ScannerRow("SURFACE MESH", "PLANE: DETECTED", StarkColors.NeonGreen)
                ScannerRow("OBJECT IDENT", "SURROUNDINGS ACTIVE", StarkColors.Cyan)
                ScannerRow("DEPTH LIDAR", "± 0.02m PRECISION", StarkColors.TextMuted)
                ScannerRow("SYSTEM TRACK", if (isTargetLocked) "OPTICAL LOCK // ENGAGED" else "SCANNING SPACE...", if (isTargetLocked) StarkColors.DangerRed else StarkColors.Gold)

                Spacer(modifier = Modifier.height(4.dp))

                // Interactive Trigger Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (isTargetLocked) StarkColors.DangerRed.copy(alpha = 0.2f) else StarkColors.CyanDim)
                        .border(1.dp, if (isTargetLocked) StarkColors.DangerRed else StarkColors.Cyan, RoundedCornerShape(3.dp))
                        .clickable { isTargetLocked = !isTargetLocked }
                        .padding(vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isTargetLocked) "DISENGAGE TARGET LOCK" else "ENGAGE SPATIAL LOCK",
                        color = if (isTargetLocked) StarkColors.DangerRed else StarkColors.TextBright,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.8.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ScannerRow(label: String, value: String, accent: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = StarkColors.TextDim,
            fontSize = 8.5.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = value,
            color = accent,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
