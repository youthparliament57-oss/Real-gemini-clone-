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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StarkHolographicCard(
    title: String,
    subtitle: String = "",
    tag: String = "HUD-01",
    isPinned: Boolean = false,
    onPinToggle: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "stark_card_scanline")
    val scanlineY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanline_position"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(StarkColors.DarkVoid)
            .border(1.dp, StarkColors.CardBorder, RoundedCornerShape(8.dp))
    ) {
        // Holographic corner bracket accents
        Canvas(modifier = Modifier.fillMaxSize()) {
            val bracketLength = 18.dp.toPx()
            val bracketStroke = 2.dp.toPx()
            val cyan = StarkColors.Cyan
            val gold = StarkColors.Gold

            // Top-Left bracket
            drawLine(cyan, Offset(0f, 0f), Offset(bracketLength, 0f), bracketStroke)
            drawLine(cyan, Offset(0f, 0f), Offset(0f, bracketLength), bracketStroke)
            drawCircle(cyan, radius = 2.5f, center = Offset(bracketLength + 6f, 0f))

            // Top-Right bracket
            drawLine(cyan, Offset(size.width, 0f), Offset(size.width - bracketLength, 0f), bracketStroke)
            drawLine(cyan, Offset(size.width, 0f), Offset(size.width, bracketLength), bracketStroke)
            drawCircle(cyan, radius = 2.5f, center = Offset(size.width - bracketLength - 6f, 0f))

            // Bottom-Left bracket
            drawLine(cyan, Offset(0f, size.height), Offset(bracketLength, size.height), bracketStroke)
            drawLine(cyan, Offset(0f, size.height), Offset(0f, size.height - bracketLength), bracketStroke)

            // Bottom-Right bracket (with Gold accent dot)
            drawLine(cyan, Offset(size.width, size.height), Offset(size.width - bracketLength, size.height), bracketStroke)
            drawLine(cyan, Offset(size.width, size.height), Offset(size.width, size.height - bracketLength), bracketStroke)
            drawCircle(gold, radius = 3f, center = Offset(size.width - 6f, size.height - 6f))

            // Dynamic holographic scanline beam sweeping downwards
            val currentY = size.height * scanlineY
            drawLine(
                color = Color(0x3300E5FF),
                start = Offset(0f, currentY),
                end = Offset(size.width, currentY),
                strokeWidth = 2.dp.toPx()
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            // High-Tech Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(StarkColors.CyanDim, RoundedCornerShape(2.dp))
                            .border(1.dp, StarkColors.Cyan, RoundedCornerShape(2.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = tag,
                            color = StarkColors.Cyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = title.uppercase(),
                            color = StarkColors.TextBright,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.2.sp
                        )
                        if (subtitle.isNotEmpty()) {
                            Text(
                                text = subtitle.uppercase(),
                                color = StarkColors.TextMuted,
                                fontSize = 8.5.sp,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onPinToggle != null) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isPinned) StarkColors.Cyan.copy(alpha = 0.25f) else Color.Transparent)
                                .clickable { onPinToggle() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPinned) Icons.Default.PushPin else Icons.Default.LocationOn,
                                contentDescription = if (isPinned) "Pinned to Room" else "Pin to Room",
                                tint = if (isPinned) StarkColors.Gold else StarkColors.TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    if (onClose != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { onClose() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss Widget",
                                tint = StarkColors.TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Thin holographic divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(StarkColors.CardBorder)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Body Content
            content()

            Spacer(modifier = Modifier.height(10.dp))

            // High-Tech Footer Ticker
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "STARK IND // PROTOCOL ACTIVE",
                    color = StarkColors.TextDim.copy(alpha = 0.6f),
                    fontSize = 7.5.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = "STATUS: NOMINAL",
                    color = StarkColors.NeonGreen,
                    fontSize = 7.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
            }
        }
    }
}
