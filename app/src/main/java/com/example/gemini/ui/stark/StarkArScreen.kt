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
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
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
import kotlin.math.roundToInt

data class StarkRoomPanel(
    val id: String,
    val type: StarkWidgetType,
    var offsetX: Float = 0f,
    var offsetY: Float = 0f,
    var scale: Float = 1.0f,
    var isMinimized: Boolean = false,
    var isPinned: Boolean = true, // When true, locked in room coordinate
    // Angular spatial coordinates in the room (degrees)
    var anchorAzimuth: Float = 0f,
    var anchorPitch: Float = 0f
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

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var isFrontCamera by remember { mutableStateOf(false) }
    var showCatalogBar by remember { mutableStateOf(true) }
    var isGyroTrackingEnabled by remember { mutableStateOf(true) }
    var selectedCatalogType by remember { mutableStateOf(StarkWidgetType.ARC_REACTOR) }
    var activeCoordinateDialogPanelId by remember { mutableStateOf<String?>(null) }
    var isSpawnPresetMenuOpen by remember { mutableStateOf(false) }

    // Real-Time Gyroscope & Spatial Fusion Sensor
    val spatialOrientation = rememberStarkSpatialSensor()

    // Placed holographic panels in the room
    val roomPanels = remember {
        mutableStateListOf(
            StarkRoomPanel(
                id = "p-1",
                type = StarkWidgetType.ARC_REACTOR,
                offsetX = 20f,
                offsetY = 160f,
                scale = 1.0f,
                isMinimized = false,
                isPinned = true,
                anchorAzimuth = 0f,
                anchorPitch = 0f
            ),
            StarkRoomPanel(
                id = "p-2",
                type = StarkWidgetType.VISION_SCANNER,
                offsetX = 20f,
                offsetY = 460f,
                scale = 1.0f,
                isMinimized = false,
                isPinned = true,
                anchorAzimuth = 10f,
                anchorPitch = -5f
            )
        )
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("stark_ar_screen")
    ) {
        val screenWidthPx = constraints.maxWidth.toFloat()
        val screenHeightPx = constraints.maxHeight.toFloat()

        // 1. Fullscreen Back-Camera Live View (The Real World Room)
        if (hasCameraPermission) {
            GeminiLiveCameraView(
                isFrontCamera = isFrontCamera,
                onPreviewReady = {},
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Camera permission request fallback
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(StarkColors.DarkVoidOpaque),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = StarkColors.Cyan,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "OPTICAL SENSORS OFFLINE",
                        color = StarkColors.TextBright,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Camera access is required for Stark AR Room Spatial Projection.",
                        color = StarkColors.TextMuted,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(20.dp))
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
            val rollOffset = if (isGyroTrackingEnabled) (spatialOrientation.rollDegrees * 1.5f).coerceIn(-100f, 100f) else 0f
            val horizonY = size.height * 0.45f + (if (isGyroTrackingEnabled) spatialOrientation.pitchDegrees * 3f else 0f)
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

        // Tap-on-screen anywhere in the room to quickly spawn a widget at that exact touch coordinate
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(selectedCatalogType) {
                    detectTapGestures { tapOffset ->
                        // Double tap to spawn directly at coordinate
                    }
                }
        )

        // 3. Floating Holographic Dashboards in the Room (Translucent, Draggable, Resizable, Pinnable)
        roomPanels.forEach { panel ->
            var panelX by remember(panel.id) { mutableFloatStateOf(panel.offsetX) }
            var panelY by remember(panel.id) { mutableFloatStateOf(panel.offsetY) }
            var panelScale by remember(panel.id) { mutableFloatStateOf(panel.scale) }
            var isMinimized by remember(panel.id) { mutableStateOf(panel.isMinimized) }
            var isPinned by remember(panel.id) { mutableStateOf(panel.isPinned) }

            // Spatial Motion Drift calculation: If pinned and Gyro tracking enabled, panel subtly anchors to world angle
            val gyroDeltaX = if (isPinned && isGyroTrackingEnabled && spatialOrientation.isTrackingActive) {
                // Azimuth drift (yaw)
                val diffYaw = (spatialOrientation.azimuthDegrees - panel.anchorAzimuth + 540f) % 360f - 180f
                (-diffYaw * 12f).coerceIn(-400f, 400f)
            } else 0f

            val gyroDeltaY = if (isPinned && isGyroTrackingEnabled && spatialOrientation.isTrackingActive) {
                // Pitch drift
                val diffPitch = spatialOrientation.pitchDegrees - panel.anchorPitch
                (-diffPitch * 10f).coerceIn(-300f, 300f)
            } else 0f

            val finalX = panelX + gyroDeltaX
            val finalY = panelY + gyroDeltaY

            Box(
                modifier = Modifier
                    .offset { IntOffset(finalX.roundToInt(), finalY.roundToInt()) }
                    .scale(panelScale)
                    .width(340.dp)
                    .pointerInput(panel.id) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            panelX = (panelX + dragAmount.x).coerceIn(0f, (screenWidthPx - 700f).coerceAtLeast(0f))
                            panelY = (panelY + dragAmount.y).coerceIn(80f, (screenHeightPx - 500f).coerceAtLeast(80f))
                            panel.offsetX = panelX
                            panel.offsetY = panelY
                            // Update spatial anchor when dragged to a new spot
                            if (spatialOrientation.isTrackingActive) {
                                panel.anchorAzimuth = spatialOrientation.azimuthDegrees
                                panel.anchorPitch = spatialOrientation.pitchDegrees
                            }
                        }
                    }
            ) {
                StarkWidgetView(
                    type = panel.type,
                    isPinned = isPinned,
                    isMinimized = isMinimized,
                    onPinToggle = {
                        isPinned = !isPinned
                        panel.isPinned = isPinned
                        if (isPinned && spatialOrientation.isTrackingActive) {
                            panel.anchorAzimuth = spatialOrientation.azimuthDegrees
                            panel.anchorPitch = spatialOrientation.pitchDegrees
                        }
                    },
                    onMinimizeToggle = {
                        isMinimized = !isMinimized
                        panel.isMinimized = isMinimized
                    },
                    onZoomIn = {
                        if (panelScale < 1.4f) {
                            panelScale = (panelScale + 0.15f).coerceAtMost(1.4f)
                            panel.scale = panelScale
                        }
                    },
                    onZoomOut = {
                        if (panelScale > 0.65f) {
                            panelScale = (panelScale - 0.15f).coerceAtLeast(0.65f)
                            panel.scale = panelScale
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

        // 4. Stark Top HUD Status Bar
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
                        .background(StarkColors.Cyan)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "STARK LAB AR // HUD",
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
                                text = "PHASE 2 • STEP 2",
                                color = StarkColors.Gold,
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    Text(
                        text = "GYRO: ${if (isGyroTrackingEnabled && spatialOrientation.isTrackingActive) "LOCKED" else "MANUAL"} | AZI: ${spatialOrientation.azimuthDegrees.toInt()}° | PIT: ${spatialOrientation.pitchDegrees.toInt()}° | PANELS: ${roomPanels.size}",
                        color = StarkColors.TextMuted,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Quick Actions: Gyro Toggle, Preset Coordinate Spawn, Camera Flip, Clear Room, Close
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Gyroscope Tracking Lock/Unlock Toggle
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
                        contentDescription = "Toggle Gyroscope Spatial Lock",
                        tint = if (isGyroTrackingEnabled) StarkColors.Cyan else StarkColors.TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Preset Room Coordinates Menu Button
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
                        contentDescription = "Room Coordinate Presets",
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

                // Reset Room (Restore default panels)
                IconButton(
                    onClick = {
                        roomPanels.clear()
                        roomPanels.add(StarkRoomPanel("p-1", StarkWidgetType.ARC_REACTOR, 20f, 160f, 1f, false, true, spatialOrientation.azimuthDegrees, spatialOrientation.pitchDegrees))
                        roomPanels.add(StarkRoomPanel("p-2", StarkWidgetType.VISION_SCANNER, 20f, 460f, 1f, false, true, spatialOrientation.azimuthDegrees, spatialOrientation.pitchDegrees))
                    },
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(StarkColors.DarkVoid)
                        .border(1.dp, StarkColors.CyanDim, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = "Reset Room",
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

        // 5. Instruction Pill Banner
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 70.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(StarkColors.DarkVoid)
                .border(1.dp, StarkColors.CyanDim, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 5.dp)
        ) {
            Text(
                text = "DRAG TO MOVE • ZOOM (+/-) • COLLAPSE (^) • PIN TO ROOM • SPATIAL GYRO ON",
                color = StarkColors.Cyan,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp
            )
        }

        // 6. Room Coordinate Presets Quick-Bar (Allows instant teleporting of panels or spawning at North Wall, Desk, etc.)
        AnimatedVisibility(
            visible = isSpawnPresetMenuOpen,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 100.dp, start = 16.dp, end = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(StarkColors.DarkVoidOpaque)
                    .border(1.dp, StarkColors.Gold, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ROOM SPATIAL ANCHORS // TARGET COORDINATES",
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
                                    // Spawn selected catalog type at this specific room coordinate
                                    val newId = "p-${System.currentTimeMillis()}"
                                    val targetX = (screenWidthPx * preset.defaultXOffset - 170f).coerceIn(10f, screenWidthPx - 360f)
                                    val targetY = (screenHeightPx * preset.defaultYOffset - 100f).coerceIn(80f, screenHeightPx - 300f)

                                    roomPanels.add(
                                        StarkRoomPanel(
                                            id = newId,
                                            type = selectedCatalogType,
                                            offsetX = targetX,
                                            offsetY = targetY,
                                            scale = 1.0f,
                                            isMinimized = false,
                                            isPinned = true,
                                            anchorAzimuth = spatialOrientation.azimuthDegrees,
                                            anchorPitch = spatialOrientation.pitchDegrees
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
                                text = "SNAP TO ROOM COORDINATE",
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
                            text = "Select an AR coordinate in your room to anchor this holographic panel:",
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
                                        panelToRelocate.offsetX = (screenWidthPx * preset.defaultXOffset - 170f).coerceIn(10f, screenWidthPx - 360f)
                                        panelToRelocate.offsetY = (screenHeightPx * preset.defaultYOffset - 100f).coerceIn(80f, screenHeightPx - 300f)
                                        panelToRelocate.anchorAzimuth = spatialOrientation.azimuthDegrees
                                        panelToRelocate.anchorPitch = spatialOrientation.pitchDegrees
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

        // 8. Bottom Catalog Dock (Tap to spawn holographic panels into the room)
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
                    // Spawn staggered near center of room
                    val spawnY = (160f + (roomPanels.size * 60f) % (screenHeightPx - 400f))
                    roomPanels.add(
                        StarkRoomPanel(
                            id = newId,
                            type = type,
                            offsetX = 20f,
                            offsetY = spawnY,
                            scale = 1.0f,
                            isMinimized = false,
                            isPinned = true,
                            anchorAzimuth = spatialOrientation.azimuthDegrees,
                            anchorPitch = spatialOrientation.pitchDegrees
                        )
                    )
                }
            )
        }
    }
}
