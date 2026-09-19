package com.example.gemini.ui.stark

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StarkMissionMatrixWidget(
    isPinned: Boolean = false,
    onPinToggle: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val tasks = remember {
        mutableStateListOf(
            StarkTaskItem("1", "PRT-01", "INIT SPATIAL ROOM MESH", "ALPHA-1", isCompleted = true),
            StarkTaskItem("2", "PRT-02", "CALIBRATE GYROSCOPE HUD", "ALPHA-2", isCompleted = true),
            StarkTaskItem("3", "PRT-03", "PIN HOLOGRAPHIC PANELS", "CRITICAL", isCompleted = false),
            StarkTaskItem("4", "PRT-04", "SYNCHRONIZE GEMINI 3.5", "BETA-1", isCompleted = false)
        )
    }

    val completedCount = tasks.count { it.isCompleted }
    val progressFraction = if (tasks.isEmpty()) 0f else completedCount.toFloat() / tasks.size
    val animatedProgress by animateFloatAsState(targetValue = progressFraction, label = "task_progress")

    StarkHolographicCard(
        title = StarkWidgetType.MISSION_MATRIX.title,
        subtitle = StarkWidgetType.MISSION_MATRIX.subtitle,
        tag = StarkWidgetType.MISSION_MATRIX.tag,
        isPinned = isPinned,
        onPinToggle = onPinToggle,
        onClose = onClose,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Progress Bar and Directives Counter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PROTOCOL COMPLETION: ${(animatedProgress * 100).toInt()}%",
                    color = StarkColors.Cyan,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "$completedCount/${tasks.size} DIRECTIVES",
                    color = StarkColors.Gold,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // High-Tech Segmented Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(StarkColors.DarkVoid)
                    .border(1.dp, StarkColors.CyanDim, RoundedCornerShape(2.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .height(6.dp)
                        .background(StarkColors.Cyan)
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Task Items List
            tasks.forEachIndexed { index, task ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (task.isCompleted) StarkColors.CyanDim.copy(alpha = 0.15f) else StarkColors.DarkVoid)
                        .border(
                            1.dp,
                            if (task.isCompleted) StarkColors.Cyan.copy(alpha = 0.4f) else StarkColors.CyanDim,
                            RoundedCornerShape(4.dp)
                        )
                        .clickable {
                            tasks[index] = task.copy(isCompleted = !task.isCompleted)
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        // High-Tech Checkbox
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .border(1.dp, if (task.isCompleted) StarkColors.Cyan else StarkColors.TextDim, RoundedCornerShape(2.dp))
                                .background(if (task.isCompleted) StarkColors.Cyan else StarkColors.DarkVoid),
                            contentAlignment = Alignment.Center
                        ) {
                            if (task.isCompleted) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = StarkColors.DarkVoidOpaque,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "[${task.code}]",
                            color = StarkColors.TextMuted,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = task.title,
                            color = if (task.isCompleted) StarkColors.TextMuted else StarkColors.TextBright,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                        )
                    }

                    // Priority Badge
                    Box(
                        modifier = Modifier
                            .background(
                                when (task.priority) {
                                    "CRITICAL" -> StarkColors.DangerRed.copy(alpha = 0.2f)
                                    "ALPHA-1" -> StarkColors.Gold.copy(alpha = 0.2f)
                                    else -> StarkColors.CyanDim
                                },
                                RoundedCornerShape(2.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = task.priority,
                            color = when (task.priority) {
                                "CRITICAL" -> StarkColors.DangerRed
                                "ALPHA-1" -> StarkColors.Gold
                                else -> StarkColors.Cyan
                            },
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
