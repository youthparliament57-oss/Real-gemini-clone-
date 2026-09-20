package com.example.ar.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.SquareFoot
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ar.analysis.model.SpatialVector3
import com.example.ar.calibration.CalibrationMethod
import com.example.ar.calibration.CalibrationState
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArCalibrationOverlay(
    calibrationState: CalibrationState,
    ceilingHeight: Float,
    onStartManual: () -> Unit,
    onStartAssisted: () -> Unit,
    onAttemptAuto: () -> Unit,
    onConfirmAuto: () -> Unit,
    onConfirmAssistedCorner: (SpatialVector3) -> Unit,
    onCaptureManualCorner: (SpatialVector3) -> Unit,
    onUpdateCeilingHeight: (Float) -> Boolean,
    onRetry: () -> Unit,
    sessionManager: com.example.ar.session.ArSessionManager,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    var hitTestError by remember { mutableStateOf<String?>(null) }

    // Clear temporary error upon state change
    LaunchedEffect(calibrationState) {
        hitTestError = null
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        // 1. Center Precision Reticle for Manual Calibration Mode
        if (calibrationState is CalibrationState.ManualCalibration) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .align(Alignment.Center)
                    .testTag("ar_precision_reticle")
            ) {
                // Outer ring
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(2.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                )
                // Inner dot
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color(0xFF38BDF8), CircleShape)
                        .align(Alignment.Center)
                )
                // Crosshairs
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(16.dp)
                        .background(Color.White)
                        .align(Alignment.TopCenter)
                )
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(16.dp)
                        .background(Color.White)
                        .align(Alignment.BottomCenter)
                )
                Box(
                    modifier = Modifier
                        .width(16.dp)
                        .height(2.dp)
                        .background(Color.White)
                        .align(Alignment.CenterStart)
                )
                Box(
                    modifier = Modifier
                        .width(16.dp)
                        .height(2.dp)
                        .background(Color.White)
                        .align(Alignment.CenterEnd)
                )
            }
        }

        // 2. Active overlay state content based on calibration state machine
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Temporary error toast for failed hits
            AnimatedVisibility(
                visible = hitTestError != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    color = Color(0xEFDC2626),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = hitTestError ?: "",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            when (calibrationState) {
                is CalibrationState.Scan -> {
                    // Handled externally or embedded as starting calibration triggers
                    // Let's show a prompt to initiate auto-calibration when scanning is ready
                }

                is CalibrationState.AutoCalibration -> {
                    CalibrationCard(
                        title = "Automatic Calibration",
                        icon = Icons.Default.Sensors,
                        description = "Analyzing detected floor planes and wall geometry intersections to configure coordinates..."
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF38BDF8),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                is CalibrationState.AutoCalibrated -> {
                    CalibrationCard(
                        title = "Calibration Complete",
                        icon = Icons.Default.CheckCircle,
                        description = "Room geometry successfully calculated from scanned plane data."
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onStartManual,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("re_calibrate_manual_btn")
                            ) {
                                Text("Manual Fine-Tune", fontSize = 12.sp)
                            }
                            Button(
                                onClick = onConfirmAuto,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("confirm_auto_calibration_btn")
                            ) {
                                Text("Confirm Room", fontSize = 12.sp, color = Color.White)
                            }
                        }
                    }
                }

                is CalibrationState.AssistedCalibration -> {
                    val remaining = 4 - calibrationState.confirmedCorners.size
                    CalibrationCard(
                        title = "Assisted Calibration",
                        icon = Icons.Default.CheckCircle,
                        description = "Reviewing auto-detected room features. Please select and confirm each room corner."
                    ) {
                        Text(
                            text = "Confirmed Corners: ${calibrationState.confirmedCorners.size}/4 ($remaining remaining)",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // If candidate corners are available, allow user to tap-confirm them
                        if (calibrationState.candidateCorners.isNotEmpty()) {
                            val nextCandidate = calibrationState.candidateCorners.getOrNull(calibrationState.confirmedCorners.size)
                            if (nextCandidate != null) {
                                Button(
                                    onClick = { onConfirmAssistedCorner(nextCandidate) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("confirm_candidate_corner_btn")
                                ) {
                                    Text("Confirm Highlighted Corner")
                                }
                            }
                        } else {
                            Text(
                                text = "No candidate corners detected. Please use manual fallback below.",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        OutlinedButton(
                            onClick = onStartManual,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("assisted_fallback_to_manual_btn")
                        ) {
                            Text("Manual 4-Corner Fallback")
                        }
                    }
                }

                is CalibrationState.ManualCalibration -> {
                    val index = calibrationState.cornerIndex
                    val stepText = when (index) {
                        0 -> "Corner 1 (Origin)"
                        1 -> "Corner 2 (X-Axis direction)"
                        2 -> "Corner 3"
                        3 -> "Corner 4"
                        else -> "Corner"
                    }

                    CalibrationCard(
                        title = "Manual 4-Corner Calibration",
                        icon = Icons.Default.Camera,
                        description = "Walk and point the reticle directly at the physical floor corner ($stepText) then capture."
                    ) {
                        // Steps indicator
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            for (i in 0 until 4) {
                                val circleColor = when {
                                    i < index -> Color(0xFF10B981) // Confirmed
                                    i == index -> Color(0xFF38BDF8) // Active
                                    else -> Color(0xFF475569) // Unreached
                                }
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(circleColor, CircleShape)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                val point = sessionManager.hitTestFloor(widthPx / 2f, heightPx / 2f)
                                if (point != null) {
                                    onCaptureManualCorner(point)
                                    hitTestError = null
                                } else {
                                    hitTestError = "Aim failure: Center reticle directly on a tracked floor surface."
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("capture_corner_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Capture $stepText", fontWeight = FontWeight.Bold, color = Color.Black)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = onRetry,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("cancel_manual_btn")
                        ) {
                            Text("Cancel Manual Fallback")
                        }
                    }
                }

                is CalibrationState.Calibrated -> {
                    val result = calibrationState.result
                    CalibrationCard(
                        title = "Room Coordinate Frame",
                        icon = Icons.Default.Sensors,
                        description = "System fully calibrated! Room Space origin locked at Corner 1. Physical walls synthesized."
                    ) {
                        // Display room metrics
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MetricBlock(
                                title = "Dimensions",
                                value = "${String.format("%.2f", result.roomWidth)} x ${String.format("%.2f", result.roomDepth)} m"
                            )
                            MetricBlock(
                                title = "Est. Floor Area",
                                value = "${String.format("%.1f", result.roomWidth * result.roomDepth)} m²"
                            )
                            MetricBlock(
                                title = "Walls Built",
                                value = "${result.wallDefinitions.size}"
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Ceiling height adjustable input
                        CeilingHeightController(
                            currentHeightMeters = ceilingHeight,
                            onUpdateHeight = onUpdateCeilingHeight
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onRetry,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("reset_calibration_btn")
                            ) {
                                Text("Re-Calibrate", fontSize = 12.sp)
                            }
                        }
                    }
                }

                is CalibrationState.CalibrationFailed -> {
                    CalibrationCard(
                        title = "Calibration Rejected",
                        icon = Icons.Default.Error,
                        description = "Geometry check failed. Details:\n${calibrationState.reason}"
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onStartManual,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("failed_to_manual_btn")
                            ) {
                                Text("Try Manual Mode", fontSize = 12.sp)
                            }
                            Button(
                                onClick = onRetry,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("failed_retry_btn")
                            ) {
                                Text("Scan Retry", fontSize = 12.sp, color = Color.White)
                            }
                        }
                    }
                }

                is CalibrationState.CalibrationRetry -> {
                    CalibrationCard(
                        title = "Re-Calibrating...",
                        icon = Icons.Default.Refresh,
                        description = "Restoring scanning and clearing cached spatial transformations..."
                    ) {
                        Button(
                            onClick = onRetry,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("retry_trigger_btn")
                        ) {
                            Text("Retry scan", color = Color.Black)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalibrationCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xF20F172A)),
        shape = RoundedCornerShape(24.dp),
        border = BoxDefaults.ActiveBorder,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("calibration_status_card")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                color = Color(0xFF94A3B8),
                fontSize = 12.sp,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun MetricBlock(title: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            color = Color(0xFF94A3B8),
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun CeilingHeightController(
    currentHeightMeters: Float,
    onUpdateHeight: (Float) -> Boolean
) {
    var textValue by remember(currentHeightMeters) {
        // Round to 2 decimals for presentation
        mutableStateOf(String.format("%.2f", currentHeightMeters))
    }
    var validationError by remember { mutableStateOf(false) }

    // Convert meters to approximate feet + inches for presentation
    val totalInches = (currentHeightMeters * 39.3701f).roundToInt()
    val feet = totalInches / 12
    val inches = totalInches % 12

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0x13FFFFFF), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.SquareFoot,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Ceiling Height",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Feet-inches friendly presentation label
            Text(
                text = "${feet}ft ${inches}in",
                color = Color(0xFF38BDF8),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Decrement Button
            IconButton(
                onClick = {
                    val newVal = (currentHeightMeters - 0.1f).coerceIn(1.5f, 10.0f)
                    onUpdateHeight(newVal)
                },
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0x26FFFFFF), CircleShape)
                    .testTag("ceiling_decrement_btn")
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = Color.White, modifier = Modifier.size(16.dp))
            }

            // Text Input Field for custom input
            OutlinedTextField(
                value = textValue,
                onValueChange = { input ->
                    textValue = input
                    val parsed = input.toFloatOrNull()
                    if (parsed != null && onUpdateHeight(parsed)) {
                        validationError = false
                    } else {
                        validationError = true
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                isError = validationError,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    errorTextColor = Color.Red,
                    focusedBorderColor = Color(0xFF38BDF8),
                    unfocusedBorderColor = Color(0xFF475569)
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("ceiling_height_input"),
                suffix = { Text("m", color = Color.White.copy(alpha = 0.6f)) }
            )

            // Increment Button
            IconButton(
                onClick = {
                    val newVal = (currentHeightMeters + 0.1f).coerceIn(1.5f, 10.0f)
                    onUpdateHeight(newVal)
                },
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0x26FFFFFF), CircleShape)
                    .testTag("ceiling_increment_btn")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Increase", tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }

        if (validationError) {
            Text(
                text = "Height must be between 1.5m (~5ft) and 10.0m (~33ft).",
                color = Color.Red,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
        }
    }
}

private object BoxDefaults {
    val ActiveBorder = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f))
}
