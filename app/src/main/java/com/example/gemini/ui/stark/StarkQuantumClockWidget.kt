package com.example.gemini.ui.stark

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StarkQuantumClockWidget(
    isPinned: Boolean = false,
    isMinimized: Boolean = false,
    onPinToggle: (() -> Unit)? = null,
    onMinimizeToggle: (() -> Unit)? = null,
    onZoomIn: (() -> Unit)? = null,
    onZoomOut: (() -> Unit)? = null,
    onRelocate: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTimeMillis = System.currentTimeMillis()
            delay(50) // High frequency update for cyber milliseconds counter
        }
    }

    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()) }
    val millis = (currentTimeMillis % 1000).toString().padStart(3, '0')

    StarkHolographicCard(
        title = StarkWidgetType.QUANTUM_ENVIRONMENT.title,
        subtitle = StarkWidgetType.QUANTUM_ENVIRONMENT.subtitle,
        tag = StarkWidgetType.QUANTUM_ENVIRONMENT.tag,
        isPinned = isPinned,
        isMinimized = isMinimized,
        onPinToggle = onPinToggle,
        onMinimizeToggle = onMinimizeToggle,
        onZoomIn = onZoomIn,
        onZoomOut = onZoomOut,
        onRelocate = onRelocate,
        onClose = onClose,
        modifier = modifier
    ) {

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Main Cyber Time Display
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(StarkColors.DarkVoid)
                    .border(1.dp, StarkColors.CyanDim, RoundedCornerShape(4.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = timeFormat.format(Date(currentTimeMillis)),
                            color = StarkColors.Cyan,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = ".$millis",
                            color = StarkColors.Gold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(bottom = 2.dp, start = 2.dp)
                        )
                    }
                    Text(
                        text = dateFormat.format(Date(currentTimeMillis)).uppercase(),
                        color = StarkColors.TextMuted,
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.8.sp
                    )
                }

                // Sector indicator
                Box(
                    modifier = Modifier
                        .background(StarkColors.CyanDim, RoundedCornerShape(2.dp))
                        .border(1.dp, StarkColors.Cyan, RoundedCornerShape(2.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "SOLAR SECTOR",
                            color = StarkColors.TextDim,
                            fontSize = 6.5.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "EARTH // 001",
                            color = StarkColors.Cyan,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Atmosphere & Environmental Telemetry
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                EnvTile("ROOM TEMP", "26.8 °C", StarkColors.NeonGreen, Modifier.weight(1f))
                EnvTile("HUMIDITY", "44% RH", StarkColors.Cyan, Modifier.weight(1f))
                EnvTile("ATM PRESSURE", "1014 hPa", StarkColors.TextMuted, Modifier.weight(1f))
                EnvTile("UV INDEX", "1.4 LOW", StarkColors.Gold, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun EnvTile(label: String, value: String, accent: Color, modifier: Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(StarkColors.DarkVoid)
            .border(1.dp, StarkColors.CyanDim, RoundedCornerShape(3.dp))
            .padding(horizontal = 6.dp, vertical = 5.dp)
    ) {
        Column {
            Text(
                text = label,
                color = StarkColors.TextDim,
                fontSize = 7.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                color = accent,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
