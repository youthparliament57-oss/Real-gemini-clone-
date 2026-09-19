package com.example.ar.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.outlined.SquareFoot
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ar.analysis.model.RoomScanResult
import com.example.ar.analysis.model.ScanQuality
import com.example.ar.analysis.model.ScanningState

/**
 * Scanning and Environment Analysis HUD overlay.
 * Displays real-time scanning feedback, guidance prompts, environmental metrics,
 * and debug visualization controls.
 */
@Composable
fun ArScanningOverlay(
    scanResult: RoomScanResult,
    isDebugVisible: Boolean,
    onToggleDebug: () -> Unit,
    onRescan: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Prominent Guidance Banner
        Surface(
            color = Color(0xCC0F172A),
            shape = RoundedCornerShape(20.dp),
            shadowElevation = 4.dp,
            modifier = Modifier
                .padding(bottom = 12.dp)
                .testTag("ar_guidance_banner")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val stateColor = when (scanResult.state) {
                    ScanningState.SCAN_READY -> Color(0xFF10B981)
                    ScanningState.GEOMETRY_ANALYZING -> Color(0xFF38BDF8)
                    ScanningState.WALLS_DETECTED -> Color(0xFF6366F1)
                    ScanningState.FLOOR_DETECTED -> Color(0xFF0EA5E9)
                    ScanningState.TRACKING_LOST -> Color(0xFFF59E0B)
                    else -> Color(0xFF94A3B8)
                }

                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(stateColor)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = scanResult.guidanceText,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }

        // 2. Main Environment Analysis Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xD91E293B)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ar_environment_metrics_card")
        ) {
            Column(
                modifier = Modifier.padding(14.dp)
            ) {
                // Header row: State name + Progress bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ViewInAr,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = scanResult.state.name.replace("_", " "),
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Scan Quality Chip
                    ScanQualityBadge(quality = scanResult.quality)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Confidence Progress Indicator
                LinearProgressIndicator(
                    progress = { scanResult.confidence },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (scanResult.isReady) Color(0xFF10B981) else Color(0xFF38BDF8),
                    trackColor = Color(0x33FFFFFF),
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Metric Pills: Floor, Walls, Corners, Depth
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Floor Metric
                    MetricPill(
                        label = "Floor",
                        value = if (scanResult.floor != null) {
                            "${String.format("%.1f", scanResult.floor.area)}m²"
                        } else {
                            "Searching"
                        },
                        isPositive = scanResult.floor != null
                    )

                    // Walls Metric
                    MetricPill(
                        label = "Walls",
                        value = "${scanResult.walls.size}",
                        isPositive = scanResult.walls.size >= 2
                    )

                    // Corners Metric
                    MetricPill(
                        label = "Corners",
                        value = "${scanResult.candidateCorners.size}",
                        isPositive = scanResult.candidateCorners.isNotEmpty()
                    )

                    // Depth Support
                    MetricPill(
                        label = "Depth",
                        value = if (scanResult.isDepthAvailable) "Active" else "Off",
                        isPositive = scanResult.isDepthAvailable
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Bottom Action Bar: Rescan + Debug Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rescan button
                    IconButton(
                        onClick = onRescan,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FFFFFF))
                            .testTag("ar_rescan_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Restart Room Scan",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Debug Overlay Toggle Button
                    IconButton(
                        onClick = onToggleDebug,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isDebugVisible) Color(0xFF38BDF8) else Color(0x33FFFFFF))
                            .testTag("ar_debug_toggle_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = "Toggle Debug Overlay",
                            tint = if (isDebugVisible) Color.Black else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricPill(
    label: String,
    value: String,
    isPositive: Boolean
) {
    Surface(
        color = Color(0x33000000),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.padding(horizontal = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isPositive) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                }
                Text(
                    text = value,
                    color = if (isPositive) Color(0xFF34D399) else Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ScanQualityBadge(quality: ScanQuality) {
    val (bgColor, textColor) = when (quality) {
        ScanQuality.EXCELLENT -> Color(0x3310B981) to Color(0xFF34D399)
        ScanQuality.GOOD -> Color(0x3338BDF8) to Color(0xFF38BDF8)
        ScanQuality.MODERATE -> Color(0x33F59E0B) to Color(0xFFFBBF24)
        ScanQuality.POOR -> Color(0x3394A3B8) to Color(0xFFCBD5E1)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = quality.name,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
