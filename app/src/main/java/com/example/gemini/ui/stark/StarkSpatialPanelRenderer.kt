package com.example.gemini.ui.stark

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.ControlCamera
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.ar.core.Anchor
import com.google.ar.core.Pose
import kotlin.math.roundToInt

/**
 * Enhanced Stark Spatial Room Panel Model supporting both:
 * 1. ARCore physical SLAM anchors (attached to floor, desk or physical room space)
 * 2. 6-DoF Metric world vectors (X, Y, Z in meters)
 */
data class StarkSpatialAnchorPanel(
    val id: String,
    val type: StarkWidgetType,
    var worldPos: Vector3D,
    var arCoreAnchor: Anchor? = null,
    var baseScale: Float = 1.0f,
    var isMinimized: Boolean = false,
    var isPinned: Boolean = true,
    var isSurfaceAnchored: Boolean = false
)

@Composable
fun StarkSpatialPanelRenderer(
    panels: List<StarkSpatialAnchorPanel>,
    arCoreState: StarkArCoreState,
    sixDoFState: Stark6DoFState,
    isGyroEnabled: Boolean,
    viewMatrix: FloatArray,
    projMatrix: FloatArray,
    screenWidthPx: Float,
    screenHeightPx: Float,
    onRemovePanel: (String) -> Unit,
    onRelocatePanel: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = context.resources.displayMetrics.density

    panels.forEach { panel ->
        var panelScale by remember(panel.id) { mutableFloatStateOf(panel.baseScale) }
        var isMinimized by remember(panel.id) { mutableStateOf(panel.isMinimized) }
        var isPinned by remember(panel.id) { mutableStateOf(panel.isPinned) }

        // Compute 3D perspective projection
        // If ARCore anchor is active and valid, use ARCore's Pose matrix projection
        val anchor = panel.arCoreAnchor
        val projResult: Stark3DProjectionResult = if (anchor != null && anchor.trackingState == com.google.ar.core.TrackingState.TRACKING) {
            StarkArCoreMath.projectAnchorPoseToScreen(
                pose = anchor.pose,
                viewMatrix = viewMatrix,
                projMatrix = projMatrix,
                screenWidthPx = screenWidthPx,
                screenHeightPx = screenHeightPx,
                baseScale = panelScale
            )
        } else {
            // Fallback to high-frequency 6-DoF Sensor Fusion Euclidean projection
            StarkSpatialMath.projectWorldPointToScreen(
                worldPos = panel.worldPos,
                cameraPos = if (isPinned && isGyroEnabled) sixDoFState.cameraPos else Vector3D(0f, 0f, 0f),
                cameraYawDeg = if (isPinned && isGyroEnabled) sixDoFState.azimuthDegrees else 0f,
                cameraPitchDeg = if (isPinned && isGyroEnabled) sixDoFState.pitchDegrees else 0f,
                screenWidthPx = screenWidthPx,
                screenHeightPx = screenHeightPx,
                baseScale = panelScale
            )
        }

        if (projResult.isVisibleInFov) {
            val cardWidthDp = 330.dp
            val cardWidthPx = 330f * density

            val renderX = (projResult.screenX - (cardWidthPx * projResult.perspectiveScale) / 2f)
            val renderY = (projResult.screenY - 140f)

            Box(
                modifier = Modifier
                    .offset { IntOffset(renderX.roundToInt(), renderY.roundToInt()) }
                    .scale(projResult.perspectiveScale)
                    .width(cardWidthDp)
                    .pointerInput(panel.id) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            // Dragging shifts the 3D coordinate along camera viewport plane
                            val yawRad = Math.toRadians(sixDoFState.azimuthDegrees.toDouble())
                            val cosY = kotlin.math.cos(yawRad).toFloat()
                            val sinY = kotlin.math.sin(yawRad).toFloat()

                            val worldDx = (dragAmount.x * 0.003f) * cosY
                            val worldDz = -(dragAmount.x * 0.003f) * sinY
                            val worldDy = -(dragAmount.y * 0.003f)

                            panel.worldPos = Vector3D(
                                x = panel.worldPos.x + worldDx,
                                y = panel.worldPos.y + worldDy,
                                z = panel.worldPos.z + worldDz
                            )
                        }
                    }
            ) {
                Column {
                    // Telemetry Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .padding(bottom = 3.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(StarkColors.DarkVoidOpaque)
                            .border(0.8.dp, StarkColors.CyanDim, RoundedCornerShape(3.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "DIST: ${String.format("%.2f", projResult.distanceMeters)}m",
                            color = StarkColors.Cyan,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = if (panel.isSurfaceAnchored) "SURFACE-ANCHORED" else "SPATIAL-VOX: [${String.format("%.1f", panel.worldPos.x)}, ${String.format("%.1f", panel.worldPos.y)}, ${String.format("%.1f", panel.worldPos.z)}]",
                            color = if (panel.isSurfaceAnchored) StarkColors.Cyan else StarkColors.TextMuted,
                            fontSize = 7.5.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        if (isPinned) {
                            Text(
                                text = "3D-LOCKED",
                                color = StarkColors.Gold,
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    StarkWidgetView(
                        type = panel.type,
                        isPinned = isPinned,
                        isMinimized = isMinimized,
                        onPinToggle = {
                            isPinned = !isPinned
                            panel.isPinned = isPinned
                        },
                        onMinimizeToggle = {
                            isMinimized = !isMinimized
                            panel.isMinimized = isMinimized
                        },
                        onZoomIn = {
                            if (panelScale < 1.6f) {
                                panelScale = (panelScale + 0.15f).coerceAtMost(1.6f)
                                panel.baseScale = panelScale
                            }
                        },
                        onZoomOut = {
                            if (panelScale > 0.55f) {
                                panelScale = (panelScale - 0.15f).coerceAtLeast(0.55f)
                                panel.baseScale = panelScale
                            }
                        },
                        onRelocate = {
                            onRelocatePanel(panel.id)
                        },
                        onClose = {
                            onRemovePanel(panel.id)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        } else {
            // Widget is off-screen! Display Stark Holographic Radar Direction Indicator Arrow
            val edgePadding = 48f
            val centerX = screenWidthPx / 2f
            val centerY = screenHeightPx / 2f

            val cosA = kotlin.math.cos(projResult.directionGuideAngleRad.toDouble()).toFloat()
            val sinA = kotlin.math.sin(projResult.directionGuideAngleRad.toDouble()).toFloat()

            val beaconX = (centerX + cosA * (centerX - edgePadding)).coerceIn(16f, screenWidthPx - 100f)
            val beaconY = (centerY + sinA * (centerY - edgePadding)).coerceIn(90f, screenHeightPx - 120f)
            val arrowRotationDeg = Math.toDegrees(projResult.directionGuideAngleRad.toDouble()).toFloat()

            Box(
                modifier = Modifier
                    .offset { IntOffset(beaconX.roundToInt(), beaconY.roundToInt()) }
                    .clip(RoundedCornerShape(6.dp))
                    .background(StarkColors.DarkVoidOpaque)
                    .border(1.dp, StarkColors.Cyan, RoundedCornerShape(6.dp))
                    .clickable {
                        // Bringing panel to current camera gaze point
                        panel.worldPos = StarkSpatialMath.calculateWorldPointInFrontOfCamera(
                            cameraPos = sixDoFState.cameraPos,
                            yawDeg = sixDoFState.azimuthDegrees,
                            pitchDeg = sixDoFState.pitchDegrees,
                            distanceMeters = 1.6f
                        )
                    }
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NearMe,
                        contentDescription = "Look towards panel",
                        tint = StarkColors.Cyan,
                        modifier = Modifier
                            .size(14.dp)
                            .rotate(arrowRotationDeg)
                    )
                    Column {
                        Text(
                            text = panel.type.tag,
                            color = StarkColors.Cyan,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${String.format("%.1f", projResult.distanceMeters)}m",
                            color = StarkColors.TextMuted,
                            fontSize = 7.5.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
