package com.example.gemini.ui.stark

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Videocam
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
    var isPinned: Boolean = true
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
    var selectedCatalogType by remember { mutableStateOf(StarkWidgetType.ARC_REACTOR) }

    // Placed holographic panels in the room
    val roomPanels = remember {
        mutableStateListOf(
            StarkRoomPanel(
                id = "p-1",
                type = StarkWidgetType.ARC_REACTOR,
                offsetX = 20f,
                offsetY = 160f,
                isPinned = true
            ),
            StarkRoomPanel(
                id = "p-2",
                type = StarkWidgetType.VISION_SCANNER,
                offsetX = 20f,
                offsetY = 460f,
                isPinned = true
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

        // 2. Translucent Stark HUD Grid & Targeting Reticle Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cyanDim = StarkColors.CyanDim
            val cyanGlow = StarkColors.Cyan.copy(alpha = 0.4f)
            val center = Offset(size.width / 2f, size.height / 2f)

            // Stark Lab Room Horizon Level Line
            val horizonY = size.height * 0.45f
            drawLine(
                color = cyanDim,
                start = Offset(0f, horizonY),
                end = Offset(size.width, horizonY),
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

        // Tap-on-screen gesture to spawn a selected widget in the room at that exact position
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(selectedCatalogType) {
                    detectTapGestures { tapOffset ->
                        // If tapped anywhere in room, spawn or focus
                    }
                }
        )

        // 3. Floating Holographic Dashboards in the Room (Translucent & Draggable)
        roomPanels.forEach { panel ->
            var panelX by remember(panel.id) { mutableFloatStateOf(panel.offsetX) }
            var panelY by remember(panel.id) { mutableFloatStateOf(panel.offsetY) }
            var isPinned by remember(panel.id) { mutableStateOf(panel.isPinned) }

            Box(
                modifier = Modifier
                    .offset { IntOffset(panelX.roundToInt(), panelY.roundToInt()) }
                    .width(340.dp)
                    .pointerInput(panel.id) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            panelX = (panelX + dragAmount.x).coerceIn(0f, (screenWidthPx - 700f).coerceAtLeast(0f))
                            panelY = (panelY + dragAmount.y).coerceIn(80f, (screenHeightPx - 500f).coerceAtLeast(80f))
                            panel.offsetX = panelX
                            panel.offsetY = panelY
                        }
                    }
            ) {
                StarkWidgetView(
                    type = panel.type,
                    isPinned = isPinned,
                    onPinToggle = {
                        isPinned = !isPinned
                        panel.isPinned = isPinned
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
                .padding(horizontal = 16.dp, vertical = 28.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Stark Logo & Active Systems
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
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(StarkColors.Gold.copy(alpha = 0.25f), RoundedCornerShape(2.dp))
                                .border(1.dp, StarkColors.Gold, RoundedCornerShape(2.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "PHASE 2",
                                color = StarkColors.Gold,
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    Text(
                        text = "OPTICAL MESH: ONLINE | PANELS: ${roomPanels.size}",
                        color = StarkColors.TextMuted,
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Quick Actions: Camera Flip, Clear Room, Catalog Toggle, Close
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Flip Camera
                IconButton(
                    onClick = { isFrontCamera = !isFrontCamera },
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(StarkColors.DarkVoid)
                        .border(1.dp, StarkColors.CyanDim, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.FlipCameraAndroid,
                        contentDescription = "Flip Camera",
                        tint = StarkColors.Cyan,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Reset Room (Restore default panels)
                IconButton(
                    onClick = {
                        roomPanels.clear()
                        roomPanels.add(StarkRoomPanel("p-1", StarkWidgetType.ARC_REACTOR, 20f, 160f, true))
                        roomPanels.add(StarkRoomPanel("p-2", StarkWidgetType.VISION_SCANNER, 20f, 460f, true))
                    },
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(StarkColors.DarkVoid)
                        .border(1.dp, StarkColors.CyanDim, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = "Reset Room",
                        tint = StarkColors.Gold,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Close Stark AR Mode
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(StarkColors.DarkVoid)
                        .border(1.dp, StarkColors.CyanDim, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Exit Stark AR Mode",
                        tint = StarkColors.TextBright,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // 5. Instruction Pill Banner
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 76.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(StarkColors.DarkVoid)
                .border(1.dp, StarkColors.CyanDim, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(
                text = "DRAG PANELS TO POSITION IN ROOM // SELECT BELOW TO SPAWN",
                color = StarkColors.Cyan,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp
            )
        }

        // 6. Bottom Catalog Dock (Tap to add holographic panels into the room)
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
                    // Spawn near center
                    val spawnY = (160f + (roomPanels.size * 60f) % (screenHeightPx - 400f))
                    roomPanels.add(
                        StarkRoomPanel(
                            id = newId,
                            type = type,
                            offsetX = 20f,
                            offsetY = spawnY,
                            isPinned = true
                        )
                    )
                }
            )
        }
    }
}
