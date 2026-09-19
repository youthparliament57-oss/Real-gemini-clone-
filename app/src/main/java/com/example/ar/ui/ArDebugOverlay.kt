package com.example.ar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ar.analysis.model.RoomScanResult

/**
 * Development & Debug Overlay for inspecting internal environment analysis geometry:
 * - Detected floor attributes (elevation Y, normal, extents, area)
 * - Detected wall candidates (azimuth, normal vector, extents, floor distance)
 * - Candidate corner intersections (world coordinates, wall pairs, angles, confidences)
 * - Tracking state and observation counts
 *
 * Can be completely toggled off without any impact on normal AR scanning.
 */
@Composable
fun ArDebugOverlay(
    scanResult: RoomScanResult,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xF0090D16)),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 280.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("ar_debug_overlay_panel")
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "GEOMETRY DEBUG HUD",
                    color = Color(0xFF38BDF8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "Conf: ${(scanResult.confidence * 100).toInt()}%",
                    color = Color(0xFF34D399),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            HorizontalDivider(
                color = Color(0x3338BDF8),
                modifier = Modifier.padding(vertical = 6.dp)
            )

            // 1. Floor Analysis Section
            Text(
                text = "FLOOR CANDIDATE",
                color = Color(0xFF94A3B8),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            )
            val floor = scanResult.floor
            if (floor != null) {
                DebugLine(
                    label = "ID / Elev",
                    value = "${floor.id} | Y=${String.format("%.2f", floor.elevationY)}m"
                )
                DebugLine(
                    label = "Area / Extent",
                    value = "${String.format("%.2f", floor.area)}m² (${String.format("%.1f", floor.extentX)}x${String.format("%.1f", floor.extentZ)}m)"
                )
                DebugLine(
                    label = "Stability / Conf",
                    value = "${floor.stabilityCount} obs | ${(floor.confidence * 100).toInt()}% (Plausible: ${floor.isPlausibleRoomFloor})"
                )
            } else {
                Text(
                    text = "No stable floor detected yet.",
                    color = Color(0xFFF59E0B),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            HorizontalDivider(
                color = Color(0x22FFFFFF),
                modifier = Modifier.padding(vertical = 6.dp)
            )

            // 2. Wall Candidates Section
            Text(
                text = "WALL CANDIDATES (${scanResult.walls.size})",
                color = Color(0xFF94A3B8),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            )
            if (scanResult.walls.isNotEmpty()) {
                scanResult.walls.forEachIndexed { idx, wall ->
                    DebugLine(
                        label = "W$idx (${wall.id})",
                        value = "Norm=[${String.format("%.1f", wall.normal.x)}, ${String.format("%.1f", wall.normal.z)}] ${wall.azimuthDegrees.toInt()}°"
                    )
                    DebugLine(
                        label = "  Dims / Dist",
                        value = "W:${String.format("%.1f", wall.width)}m H:${String.format("%.1f", wall.height)}m | DistFlr:${String.format("%.2f", wall.distanceToFloor)}m"
                    )
                    DebugLine(
                        label = "  Conf / Obs",
                        value = "${(wall.confidence * 100).toInt()}% | ${wall.stabilityCount} obs"
                    )
                }
            } else {
                Text(
                    text = "No vertical wall planes detected.",
                    color = Color(0xFFCBD5E1),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            HorizontalDivider(
                color = Color(0x22FFFFFF),
                modifier = Modifier.padding(vertical = 6.dp)
            )

            // 3. Candidate Corners Section
            Text(
                text = "CANDIDATE CORNERS (${scanResult.candidateCorners.size})",
                color = Color(0xFF94A3B8),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            )
            if (scanResult.candidateCorners.isNotEmpty()) {
                scanResult.candidateCorners.forEachIndexed { idx, corner ->
                    DebugLine(
                        label = "C$idx (${corner.wallIdA}x${corner.wallIdB})",
                        value = "[${String.format("%.2f", corner.position.x)}, ${String.format("%.2f", corner.position.z)}] ${corner.angleDegrees.toInt()}° (${(corner.confidence * 100).toInt()}%)"
                    )
                }
            } else {
                Text(
                    text = "No corner intersections verified.",
                    color = Color(0xFFCBD5E1),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun DebugLine(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
    ) {
        Text(
            text = label,
            color = Color(0xFF64748B),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(105.dp)
        )
        Text(
            text = value,
            color = Color(0xFFE2E8F0),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
