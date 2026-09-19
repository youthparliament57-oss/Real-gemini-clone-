package com.example.gemini.ui.stark

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ControlCamera
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.gemini.ui.components.GeminiLiveCameraView
import kotlin.math.PI
import kotlin.math.roundToInt

/**
 * 3D Spatial Room Panel Model
 * Holds true 3D World Space coordinates (X, Y, Z in meters) in the user's room.
 * When isPinned = true, it stays fixed at worldPos.
 * When the user turns the phone, it moves dynamically with realistic perspective projection.
 */
data class StarkRoomPanel(
    val id: String,
    val type: StarkWidgetType,
    var worldPos: Vector3D,
    var baseScale: Float = 1.0f,
    var isMinimized: Boolean = false,
    var isPinned: Boolean = true // When true, locked in 3D world room coordinate
)

@Composable
fun StarkArScreen(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
        if (!hasAudioPermission) {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    var isFrontCamera by remember { mutableStateOf(false) }
    var showCatalogBar by remember { mutableStateOf(true) }
    var isGyroTrackingEnabled by remember { mutableStateOf(true) }
    var selectedCatalogType by remember { mutableStateOf(StarkWidgetType.ARC_REACTOR) }
    var activeCoordinateDialogPanelId by remember { mutableStateOf<String?>(null) }
    var isSpawnPresetMenuOpen by remember { mutableStateOf(false) }

    // Real-Time 6-DoF Sensor Fusion Engine (Gyroscope + Accelerometer + Speedometer + Magnetometer)
    val (sixDoFState, resetOrigin) = rememberStark6DoFSensor()

    // 3D Placed holographic panels anchored in the room's physical coordinate space (meters)
    val roomPanels = remember {
        mutableStateListOf(
            StarkRoomPanel(
                id = "p-1",
                type = StarkWidgetType.ARC_REACTOR,
                worldPos = Vector3D(x = -0.35f, y = 0.15f, z = 1.8f),
                baseScale = 1.0f,
                isMinimized = false,
                isPinned = true
            ),
            StarkRoomPanel(
                id = "p-2",
                type = StarkWidgetType.VISION_SCANNER,
                worldPos = Vector3D(x = 0.85f, y = -0.2f, z = 1.9f),
                baseScale = 1.0f,
                isMinimized = false,
                isPinned = true
            )
        )
    }

    // JARVIS Voice AI Recognition Engine
    val (jarvisState, jarvisEngine) = rememberJarvisVoiceEngine(
        onActionTriggered = { action, _ ->
            when (action) {
                is JarvisVoiceAction.SpawnWidget -> {
                    val newId = "p-${System.currentTimeMillis()}"
                    // Spawn 3D dashboard directly in front of camera's current 3D line-of-sight
                    val spawnPos = StarkSpatialMath.calculateWorldPointInFrontOfCamera(
                        cameraPos = sixDoFState.cameraPos,
                        yawDeg = sixDoFState.azimuthDegrees,
                        pitchDeg = sixDoFState.pitchDegrees,
                        distanceMeters = 1.7f
                    )
                    roomPanels.add(
                        StarkRoomPanel(
                            id = newId,
                            type = action.type,
                            worldPos = spawnPos,
                            baseScale = 1.0f,
                            isMinimized = false,
                            isPinned = true
                        )
                    )
                }
                is JarvisVoiceAction.SnapAllTo -> {
                    roomPanels.forEachIndexed { index, panel ->
                        val baseVec = action.preset.worldVector
                        val staggered = Vector3D(
                            x = baseVec.x + (index * 0.4f),
                            y = baseVec.y,
                            z = baseVec.z
                        )
                        panel.worldPos = staggered
                        panel.isPinned = true
                    }
                }
                is JarvisVoiceAction.MinimizeAll -> {
                    roomPanels.forEach { it.isMinimized = true }
                }
                is JarvisVoiceAction.MaximizeAll -> {
                    roomPanels.forEach { it.isMinimized = false }
                }
                is JarvisVoiceAction.LockAll -> {
                    roomPanels.forEach { it.isPinned = true }
                }
                is JarvisVoiceAction.UnlockAll -> {
                    roomPanels.forEach { it.isPinned = false }
                }
                is JarvisVoiceAction.ClearRoom -> {
                    roomPanels.clear()
                }
                is JarvisVoiceAction.ResetRoom -> {
                    roomPanels.clear()
                    resetOrigin()
                    roomPanels.add(
                        StarkRoomPanel("p-1", StarkWidgetType.ARC_REACTOR, Vector3D(-0.35f, 0.15f, 1.8f), 1.0f, false, true)
                    )
                    roomPanels.add(
                        StarkRoomPanel("p-2", StarkWidgetType.VISION_SCANNER, Vector3D(0.85f, -0.2f, 1.9f), 1.0f, false, true)
                    )
                }
                is JarvisVoiceAction.ToggleGyro -> {
                    isGyroTrackingEnabled = !isGyroTrackingEnabled
                }
            }
        }
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("stark_ar_screen")
    ) {
        val screenWidthPx = constraints.maxWidth.toFloat()
        val screenHeightPx = constraints.maxHeight.toFloat()

        // 1. Fullscreen Camera View (The Physical Room)
        if (hasCameraPermission) {
            GeminiLiveCameraView(
                isFrontCamera = isFrontCamera,
                onPreviewReady = {},
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF03080F)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = StarkColors.Cyan,
                        modifier = Modifier.size(56.dp)
                    )
                    Text(
                        text = "OPTICAL CAMERA PERMISSION REQUIRED",
                        color = StarkColors.TextBright,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = StarkColors.Cyan)
                    ) {
                        Text(
                            text = "ACTIVATE OPTICAL SENSORS",
                            color = StarkColors.DarkVoidOpaque,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // 2. Translucent Stark HUD Grid, Spatial Compass & Targeting Reticle Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cyanDim = StarkColors.CyanDim
            val cyanGlow = StarkColors.Cyan.copy(alpha = 0.4f)
            val center = Offset(size.width / 2f, size.height / 2f)

            // Stark Lab Room Horizon Level Line with Gyro tilt
            val rollOffset = if (isGyroTrackingEnabled) (sixDoFState.rollDegrees * 1.5f).coerceIn(-100f, 100f) else 0f
            val horizonY = size.height * 0.45f + (if (isGyroTrackingEnabled) sixDoFState.pitchDegrees * 3f else 0f)
            drawLine(
                color = cyanDim,
                start = Offset(0f, horizonY - rollOffset),
                end = Offset(size.width, horizonY + rollOffset),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 15f), 0f)
            )

            // Central Targeting Crosshairs
            val reticleSize = 36.dp.toPx()
            drawLine(cyanGlow, Offset(center.x - reticleSize, center.y), Offset(center.x + reticleSize, center.y), 1.dp.toPx())
            drawLine(cyanGlow, Offset(center.x, center.y - reticleSize), Offset(center.x, center.y + reticleSize), 1.dp.toPx())
            drawCircle(cyanDim, radius = reticleSize * 1.5f, center = center, style = Stroke(width = 1.dp.toPx()))

            // Corner Spatial HUD Brackets
            val cornerLen = 30.dp.toPx()
            val stroke = 2.dp.toPx()
            val pad = 16.dp.toPx()

            // Top Left
            drawLine(cyanGlow, Offset(pad, pad), Offset(pad + cornerLen, pad), stroke)
            drawLine(cyanGlow, Offset(pad, pad), Offset(pad, pad + cornerLen), stroke)

            // Top Right
            drawLine(cyanGlow, Offset(size.width - pad, pad), Offset(size.width - pad - cornerLen, pad), stroke)
            drawLine(cyanGlow, Offset(size.width - pad, pad), Offset(size.width - pad, pad + cornerLen), stroke)

            // Bottom Left
            drawLine(cyanGlow, Offset(pad, size.height - pad), Offset(pad + cornerLen, size.height - pad), stroke)
            drawLine(cyanGlow, Offset(pad, size.height - pad), Offset(pad, size.height - pad - cornerLen), stroke)

            // Bottom Right
            drawLine(cyanGlow, Offset(size.width - pad, size.height - pad), Offset(size.width - pad - cornerLen, size.height - pad), stroke)
            drawLine(cyanGlow, Offset(size.width - pad, size.height - pad), Offset(size.width - pad, size.height - pad - cornerLen), stroke)
        }

        // 3. True 3D Floating Holographic Dashboards in the Room
        // Each panel is projected from its fixed 3D room coordinate (X, Y, Z) to 2D screen coordinates.
        // If the user turns the camera away from the panel, it naturally leaves the FoV, with a Stark Direction Arrow indicating where it is!
        roomPanels.forEach { panel ->
            var panelScale by remember(panel.id) { mutableFloatStateOf(panel.baseScale) }
            var isMinimized by remember(panel.id) { mutableStateOf(panel.isMinimized) }
            var isPinned by remember(panel.id) { mutableStateOf(panel.isPinned) }

            // Project 3D room coordinate to 2D camera viewport
            val proj = StarkSpatialMath.projectWorldPointToScreen(
                worldPos = panel.worldPos,
                cameraPos = if (isPinned && isGyroTrackingEnabled) sixDoFState.cameraPos else Vector3D(0f, 0f, 0f),
                cameraYawDeg = if (isPinned && isGyroTrackingEnabled) sixDoFState.azimuthDegrees else 0f,
                cameraPitchDeg = if (isPinned && isGyroTrackingEnabled) sixDoFState.pitchDegrees else 0f,
                screenWidthPx = screenWidthPx,
                screenHeightPx = screenHeightPx,
                baseScale = panelScale
            )

            if (proj.isVisibleInFov) {
                // Widget is in front of camera and inside the FoV
                val cardWidthDp = 330.dp
                val cardWidthPx = 330f * (context.resources.displayMetrics.density)

                // Center the card on the 3D projected coordinate
                val renderX = (proj.screenX - (cardWidthPx * proj.perspectiveScale) / 2f)
                val renderY = (proj.screenY - 140f)

                Box(
                    modifier = Modifier
                        .offset { IntOffset(renderX.roundToInt(), renderY.roundToInt()) }
                        .scale(proj.perspectiveScale)
                        .width(cardWidthDp)
                        .pointerInput(panel.id) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                // Dragging moves the 3D world coordinate in the plane orthogonal to camera gaze
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
                        // 3D Telemetry spatial tag above the widget
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
                                text = "DIST: ${String.format("%.2f", proj.distanceMeters)}m",
                                color = StarkColors.Cyan,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "POS: [${String.format("%.1f", panel.worldPos.x)}, ${String.format("%.1f", panel.worldPos.y)}, ${String.format("%.1f", panel.worldPos.z)}]",
                                color = StarkColors.TextMuted,
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
                                activeCoordinateDialogPanelId = panel.id
                            },
                            onClose = {
                                roomPanels.removeIf { it.id == panel.id }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else {
                // Widget is outside the camera view field! Show a Stark HUD Edge Radar Beacon
                // directing the user where in the physical room to look to see this dashboard!
                val edgePadding = 48f
                val centerX = screenWidthPx / 2f
                val centerY = screenHeightPx / 2f

                val cosA = kotlin.math.cos(proj.directionGuideAngleRad.toDouble()).toFloat()
                val sinA = kotlin.math.sin(proj.directionGuideAngleRad.toDouble()).toFloat()

                val beaconX = (centerX + cosA * (centerX - edgePadding)).coerceIn(16f, screenWidthPx - 100f)
                val beaconY = (centerY + sinA * (centerY - edgePadding)).coerceIn(90f, screenHeightPx - 120f)
                val arrowRotationDeg = Math.toDegrees(proj.directionGuideAngleRad.toDouble()).toFloat()

                Box(
                    modifier = Modifier
                        .offset { IntOffset(beaconX.roundToInt(), beaconY.roundToInt()) }
                        .clip(RoundedCornerShape(6.dp))
                        .background(StarkColors.DarkVoidOpaque)
                        .border(1.dp, StarkColors.Cyan, RoundedCornerShape(6.dp))
                        .clickable {
                            // Tap beacon to bring panel directly in front of current gaze
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
                                text = "${String.format("%.1f", proj.distanceMeters)}m",
                                color = StarkColors.TextMuted,
                                fontSize = 7.5.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // 4. Stark Top HUD Status Bar (Real-Time 6-DoF Telemetry & Speedometer)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 26.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Stark Logo & Active Systems Telemetry
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (sixDoFState.isTrackingActive) StarkColors.Cyan else StarkColors.DangerRed)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "STARK LAB 3D // 6-DoF",
                            color = StarkColors.TextBright,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(StarkColors.Gold.copy(alpha = 0.25f), RoundedCornerShape(2.dp))
                                .border(1.dp, StarkColors.Gold, RoundedCornerShape(2.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "PHASE 2 • 3D SPATIAL",
                                color = StarkColors.Gold,
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    Text(
                        text = "POS: [${String.format("%.1f", sixDoFState.cameraPos.x)}, ${String.format("%.1f", sixDoFState.cameraPos.y)}, ${String.format("%.1f", sixDoFState.cameraPos.z)}]m | SPD: ${String.format("%.2f", sixDoFState.speedMetersPerSec)}m/s | AZI: ${sixDoFState.azimuthDegrees.toInt()}° | PANELS: ${roomPanels.size}",
                        color = StarkColors.TextMuted,
                        fontSize = 7.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Quick Actions: JARVIS Mic, 6-DoF Lock Toggle, Reset Origin, Preset Spawner, Flip Camera, Close
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // JARVIS Voice AI Recognition Button
                IconButton(
                    onClick = {
                        if (!hasAudioPermission) {
                            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else {
                            if (jarvisState.isListening) {
                                jarvisEngine.stopListening()
                            } else {
                                jarvisEngine.startListening()
                            }
                        }
                    },
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (jarvisState.isListening) StarkColors.DangerRed.copy(alpha = 0.35f) else StarkColors.DarkVoid)
                        .border(1.dp, if (jarvisState.isListening) StarkColors.DangerRed else StarkColors.CyanDim, CircleShape)
                ) {
                    Icon(
                        imageVector = if (jarvisState.isListening) Icons.Default.GraphicEq else Icons.Default.Mic,
                        contentDescription = "JARVIS Voice Commands",
                        tint = if (jarvisState.isListening) StarkColors.DangerRed else StarkColors.Cyan,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Gyroscope & 6-DoF Spatial Tracking Lock/Unlock Toggle
                IconButton(
                    onClick = { isGyroTrackingEnabled = !isGyroTrackingEnabled },
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (isGyroTrackingEnabled) StarkColors.Cyan.copy(alpha = 0.25f) else StarkColors.DarkVoid)
                        .border(1.dp, if (isGyroTrackingEnabled) StarkColors.Cyan else StarkColors.CyanDim, CircleShape)
                ) {
                    Icon(
                        imageVector = if (isGyroTrackingEnabled) Icons.Default.Explore else Icons.Default.ControlCamera,
                        contentDescription = "Toggle 6-DoF Spatial Lock",
                        tint = if (isGyroTrackingEnabled) StarkColors.Cyan else StarkColors.TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Preset Room 3D Coordinates Menu Button
                IconButton(
                    onClick = { isSpawnPresetMenuOpen = !isSpawnPresetMenuOpen },
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (isSpawnPresetMenuOpen) StarkColors.Gold.copy(alpha = 0.25f) else StarkColors.DarkVoid)
                        .border(1.dp, if (isSpawnPresetMenuOpen) StarkColors.Gold else StarkColors.CyanDim, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = "3D Room Presets",
                        tint = if (isSpawnPresetMenuOpen) StarkColors.Gold else StarkColors.Cyan,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Flip Camera
                IconButton(
                    onClick = { isFrontCamera = !isFrontCamera },
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(StarkColors.DarkVoid)
                        .border(1.dp, StarkColors.CyanDim, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.FlipCameraAndroid,
                        contentDescription = "Flip Camera",
                        tint = StarkColors.Cyan,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Reset Room & Zero 3D Origin
                IconButton(
                    onClick = {
                        resetOrigin()
                        roomPanels.clear()
                        roomPanels.add(
                            StarkRoomPanel("p-1", StarkWidgetType.ARC_REACTOR, Vector3D(-0.35f, 0.15f, 1.8f), 1.0f, false, true)
                        )
                        roomPanels.add(
                            StarkRoomPanel("p-2", StarkWidgetType.VISION_SCANNER, Vector3D(0.85f, -0.2f, 1.9f), 1.0f, false, true)
                        )
                    },
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(StarkColors.DarkVoid)
                        .border(1.dp, StarkColors.CyanDim, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = "Zero 3D Origin & Reset Panels",
                        tint = StarkColors.Gold,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Close Stark AR Mode
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(StarkColors.DarkVoid)
                        .border(1.dp, StarkColors.CyanDim, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Exit Stark AR Mode",
                        tint = StarkColors.TextBright,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // 5. JARVIS Live Voice Feedback HUD & Instruction Banner
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 70.dp, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // JARVIS Vocal Telemetry Pill
            AnimatedVisibility(
                visible = jarvisState.isListening || jarvisState.lastRecognizedText.isNotEmpty(),
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -it })
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (jarvisState.isListening) StarkColors.DangerRed.copy(alpha = 0.25f) else StarkColors.DarkVoidOpaque)
                        .border(1.dp, if (jarvisState.isListening) StarkColors.DangerRed else StarkColors.Cyan, RoundedCornerShape(8.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (jarvisState.isListening) Icons.Default.GraphicEq else Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = if (jarvisState.isListening) StarkColors.DangerRed else StarkColors.Cyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Column {
                            Text(
                                text = jarvisState.jarvisStatus,
                                color = if (jarvisState.isListening) StarkColors.DangerRed else StarkColors.Cyan,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            if (jarvisState.jarvisResponse.isNotEmpty()) {
                                Text(
                                    text = jarvisState.jarvisResponse,
                                    color = StarkColors.TextBright,
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }

            // Quick 3D Spatial Instruction Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(StarkColors.DarkVoid)
                    .border(1.dp, StarkColors.CyanDim, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "MOVE CAMERA AROUND ROOM • PANELS STAY LOCKED IN 3D SPACE • RADAR ARROWS GUIDE OFF-SCREEN",
                    color = StarkColors.Cyan,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
            }
        }

        // 6. 3D Room Coordinate Presets Quick-Bar
        AnimatedVisibility(
            visible = isSpawnPresetMenuOpen,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 105.dp, start = 16.dp, end = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(StarkColors.DarkVoidOpaque)
                    .border(1.dp, StarkColors.Gold.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ROOM 3D SPATIAL ANCHORS // METRIC OFFSETS",
                        color = StarkColors.Gold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "SPAWN OR SNAP",
                        color = StarkColors.TextMuted,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(RoomPresetCoordinate.values()) { preset ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(StarkColors.DarkVoid)
                                .border(1.dp, StarkColors.CyanDim, RoundedCornerShape(4.dp))
                                .clickable {
                                    val newId = "p-${System.currentTimeMillis()}"
                                    roomPanels.add(
                                        StarkRoomPanel(
                                            id = newId,
                                            type = selectedCatalogType,
                                            worldPos = preset.worldVector,
                                            baseScale = 1.0f,
                                            isMinimized = false,
                                            isPinned = true
                                        )
                                    )
                                    isSpawnPresetMenuOpen = false
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Column {
                                Text(
                                    text = preset.label,
                                    color = StarkColors.Cyan,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = preset.description,
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

        // 7. Relocate Coordinate Dialog (When user taps Location button on a card)
        if (activeCoordinateDialogPanelId != null) {
            val panelToRelocate = roomPanels.find { it.id == activeCoordinateDialogPanelId }
            if (panelToRelocate != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.65f))
                        .clickable { activeCoordinateDialogPanelId = null },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .width(320.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(StarkColors.DarkVoidOpaque)
                            .border(1.5.dp, StarkColors.Cyan, RoundedCornerShape(8.dp))
                            .padding(16.dp)
                            .clickable(enabled = false) {}
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SNAP TO 3D ROOM COORDINATE",
                                color = StarkColors.Cyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            IconButton(
                                onClick = { activeCoordinateDialogPanelId = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = StarkColors.TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Text(
                            text = "Choose a physical 3D sector in your room to anchor this dashboard:",
                            color = StarkColors.TextMuted,
                            fontSize = 8.5.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        RoomPresetCoordinate.values().forEach { preset ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(StarkColors.DarkVoid)
                                    .border(1.dp, StarkColors.CyanDim, RoundedCornerShape(4.dp))
                                    .clickable {
                                        panelToRelocate.worldPos = preset.worldVector
                                        panelToRelocate.isPinned = true
                                        activeCoordinateDialogPanelId = null
                                    }
                                    .padding(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = preset.label,
                                            color = StarkColors.TextBright,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = preset.description,
                                            color = StarkColors.TextMuted,
                                            fontSize = 8.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.PushPin,
                                        contentDescription = null,
                                        tint = StarkColors.Gold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 8. Bottom Catalog Dock (Tap to spawn holographic panels into current gaze)
        AnimatedVisibility(
            visible = showCatalogBar,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            StarkWidgetSelectorBar(
                selectedType = selectedCatalogType,
                onSelectType = { selectedCatalogType = it },
                onSpawnWidget = { type ->
                    val newId = "p-${System.currentTimeMillis()}"
                    // Spawn at the 3D position directly in front of the camera's gaze
                    val forwardPos = StarkSpatialMath.calculateWorldPointInFrontOfCamera(
                        cameraPos = sixDoFState.cameraPos,
                        yawDeg = sixDoFState.azimuthDegrees,
                        pitchDeg = sixDoFState.pitchDegrees,
                        distanceMeters = 1.6f
                    )
                    roomPanels.add(
                        StarkRoomPanel(
                            id = newId,
                            type = type,
                            worldPos = forwardPos,
                            baseScale = 1.0f,
                            isMinimized = false,
                            isPinned = true
                        )
                    )
                }
            )
        }
    }
}
