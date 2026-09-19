package com.example.gemini.ui.stark

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StarkWidgetSelectorBar(
    selectedType: StarkWidgetType,
    onSelectType: (StarkWidgetType) -> Unit,
    onSpawnWidget: (StarkWidgetType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(StarkColors.DarkVoid)
            .border(1.dp, StarkColors.CardBorder, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .padding(vertical = 12.dp)
    ) {
        // Bar Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(StarkColors.Cyan)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "STARK HOLOGRAPHIC CATALOG",
                    color = StarkColors.TextBright,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }

            Text(
                text = "TAP TO SPAWN IN ROOM",
                color = StarkColors.Gold,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Widget Cards Carousel
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(StarkWidgetType.values()) { type ->
                val isSelected = type == selectedType
                val icon = when (type) {
                    StarkWidgetType.ARC_REACTOR -> Icons.Default.Bolt
                    StarkWidgetType.VISION_SCANNER -> Icons.Default.Radar
                    StarkWidgetType.MISSION_MATRIX -> Icons.Default.Checklist
                    StarkWidgetType.QUANTUM_ENVIRONMENT -> Icons.Default.Schedule
                }

                Box(
                    modifier = Modifier
                        .width(135.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) StarkColors.CyanDim else StarkColors.DarkVoidOpaque)
                        .border(
                            1.dp,
                            if (isSelected) StarkColors.Cyan else StarkColors.CardBorder,
                            RoundedCornerShape(6.dp)
                        )
                        .clickable {
                            onSelectType(type)
                            onSpawnWidget(type)
                        }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSelected) StarkColors.Cyan else StarkColors.TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Spawn",
                                tint = if (isSelected) StarkColors.Gold else StarkColors.TextDim,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = type.title,
                            color = if (isSelected) StarkColors.TextBright else StarkColors.TextMuted,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1
                        )

                        Text(
                            text = type.tag,
                            color = if (isSelected) StarkColors.Gold else StarkColors.TextDim,
                            fontSize = 7.5.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
