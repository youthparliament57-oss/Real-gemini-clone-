package com.example.ar.ui

import android.Manifest
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ar.capability.ArAvailability
import com.example.ar.rendering.ArCameraView
import com.example.ar.session.ArRuntimeState
import com.example.ar.calibration.CalibrationState

/**
 * Dedicated minimal AR screen shell for verifying ARCore session lifecycle,
 * camera background rendering, capability detection, and tracking state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArWorkspaceScreen(
    onDismiss: () -> Unit,
    viewModel: ArWorkspaceViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current

    val availability by viewModel.availability.collectAsState()
    val runtimeState by viewModel.runtimeState.collectAsState()
    val scanResult by viewModel.scanResult.collectAsState()
    val isDebugVisible by viewModel.isDebugOverlayVisible.collectAsState()
    val calibrationState by viewModel.calibrationState.collectAsState()
    val ceilingHeight by viewModel.ceilingHeight.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted && activity != null) {
            viewModel.startSession(activity)
        }
    }

    // Lifecycle coordination
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    if (viewModel.capabilityChecker.hasCameraPermission() && activity != null) {
                        viewModel.startSession(activity)
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    viewModel.pauseSession()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.closeSession()
        }
    }

    // Auto-request camera permission if not granted on start
    LaunchedEffect(Unit) {
        if (!viewModel.capabilityChecker.hasCameraPermission()) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        } else if (activity != null) {
            viewModel.startSession(activity)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("ar_workspace_screen")
    ) {
        // Camera Viewport layer
        if (viewModel.capabilityChecker.hasCameraPermission() &&
            availability == ArAvailability.SUPPORTED_INSTALLED &&
            runtimeState !is ArRuntimeState.InstallRequired &&
            runtimeState !is ArRuntimeState.Unavailable &&
            runtimeState !is ArRuntimeState.Error &&
            runtimeState !is ArRuntimeState.PermissionRequired
        ) {
            ArCameraView(
                sessionManager = viewModel.sessionManager,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Top Status Header and Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Back Button
                IconButton(
                    onClick = {
                        viewModel.closeSession()
                        onDismiss()
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0x80000000))
                        .testTag("ar_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Exit AR Mode",
                        tint = Color.White
                    )
                }

                // Title Chip
                Surface(
                    color = Color(0x99000000),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "AR Foundation",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Status Badge (State Indicator)
                ArStateBadge(state = runtimeState)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Secondary Info Badges (Tracking detail, Depth support)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                when (val state = runtimeState) {
                    is ArRuntimeState.Tracking -> {
                        Surface(
                            color = Color(0xB310B981),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (state.isDepthSupported) "6DoF Tracking Active (Depth On)" else "6DoF Tracking Active",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    is ArRuntimeState.TrackingLost -> {
                        Surface(
                            color = Color(0xB3F59E0B),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = state.reason,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    is ArRuntimeState.Running -> {
                        Surface(
                            color = Color(0xB33B82F6),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Initializing tracking...",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    else -> {}
                }
            }
        }

        // Bottom Scanning and Environment Analysis Overlay / Calibration Overlay
        if (viewModel.capabilityChecker.hasCameraPermission() &&
            availability == ArAvailability.SUPPORTED_INSTALLED &&
            (runtimeState is ArRuntimeState.Tracking || runtimeState is ArRuntimeState.Running || runtimeState is ArRuntimeState.TrackingLost)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            ) {
                if (calibrationState is CalibrationState.Scan) {
                    if (isDebugVisible) {
                        ArDebugOverlay(
                            scanResult = scanResult,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    ArScanningOverlay(
                        scanResult = scanResult,
                        isDebugVisible = isDebugVisible,
                        onToggleDebug = { viewModel.toggleDebugOverlay() },
                        onRescan = { viewModel.resetScan() },
                        onCalibrate = { viewModel.attemptAutoCalibration() },
                        onStartManual = { viewModel.startManualCalibration() }
                    )
                } else {
                    ArCalibrationOverlay(
                        calibrationState = calibrationState,
                        ceilingHeight = ceilingHeight,
                        onStartManual = { viewModel.startManualCalibration() },
                        onStartAssisted = { viewModel.startAssistedCalibration() },
                        onAttemptAuto = { viewModel.attemptAutoCalibration() },
                        onConfirmAuto = { viewModel.confirmAutoCalibration() },
                        onConfirmAssistedCorner = { viewModel.confirmAssistedCorner(it) },
                        onCaptureManualCorner = { viewModel.captureManualCorner(it) },
                        onUpdateCeilingHeight = { viewModel.updateCeilingHeight(it) },
                        onRetry = { viewModel.retryCalibration() },
                        sessionManager = viewModel.sessionManager
                    )
                }
            }
        }

        // Informational Guidance Overlay for Non-Running States
        when {
            !viewModel.capabilityChecker.hasCameraPermission() || runtimeState is ArRuntimeState.PermissionRequired -> {
                PermissionRequiredCard(
                    onRequestPermission = {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    onDismiss = onDismiss
                )
            }
            availability == ArAvailability.UNSUPPORTED_DEVICE_NOT_CAPABLE || runtimeState is ArRuntimeState.Unavailable -> {
                UnsupportedDeviceCard(onDismiss = onDismiss)
            }
            availability == ArAvailability.SUPPORTED_NOT_INSTALLED ||
            availability == ArAvailability.SUPPORTED_APK_TOO_OLD ||
            runtimeState is ArRuntimeState.InstallRequired -> {
                InstallRequiredCard(
                    onInstall = {
                        if (activity != null) viewModel.requestInstall(activity)
                    },
                    onDismiss = onDismiss
                )
            }
            runtimeState is ArRuntimeState.Error -> {
                ErrorCard(
                    message = (runtimeState as ArRuntimeState.Error).message,
                    onRetry = {
                        if (activity != null) viewModel.startSession(activity)
                    },
                    onDismiss = onDismiss
                )
            }
        }
    }
}

@Composable
private fun ArStateBadge(state: ArRuntimeState) {
    val (color, text) = when (state) {
        is ArRuntimeState.Tracking -> Color(0xFF10B981) to "TRACKING"
        is ArRuntimeState.Running -> Color(0xFF38BDF8) to "RUNNING"
        is ArRuntimeState.TrackingLost -> Color(0xFFF59E0B) to "TRACKING LOST"
        is ArRuntimeState.Ready -> Color(0xFF94A3B8) to "READY"
        is ArRuntimeState.Paused -> Color(0xFF64748B) to "PAUSED"
        is ArRuntimeState.PermissionRequired -> Color(0xFFEF4444) to "PERM REQ"
        is ArRuntimeState.InstallRequired -> Color(0xFFF97316) to "INSTALL REQ"
        is ArRuntimeState.Unavailable -> Color(0xFFDC2626) to "UNSUPPORTED"
        is ArRuntimeState.Error -> Color(0xFFDC2626) to "ERROR"
    }

    Surface(
        color = Color(0x99000000),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun PermissionRequiredCard(
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color(0xF01E293B),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Camera,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Camera Permission Needed",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "ARCore requires access to the camera to track physical surfaces and display spatial overlays.",
                    fontSize = 14.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onRequestPermission,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Grant Camera Access")
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun UnsupportedDeviceCard(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color(0xF01E293B),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "ARCore Not Supported",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "This device or emulator does not support Google Play Services for AR. Physical AR tracking requires an ARCore-supported device.",
                    fontSize = 14.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Return to Gemini")
                }
            }
        }
    }
}

@Composable
private fun InstallRequiredCard(
    onInstall: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color(0xF01E293B),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "ARCore Update Required",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Google Play Services for AR must be installed or updated to run the AR workspace.",
                    fontSize = 14.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onInstall,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Install / Update ARCore")
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun ErrorCard(
    message: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color(0xF01E293B),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "AR Initialization Error",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    fontSize = 14.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Retry")
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Return to Gemini", color = Color.White)
                }
            }
        }
    }
}
