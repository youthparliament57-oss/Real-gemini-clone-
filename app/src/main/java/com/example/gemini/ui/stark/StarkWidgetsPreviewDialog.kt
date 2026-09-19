package com.example.gemini.ui.stark

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch

@Composable
fun StarkWidgetsPreviewDialog(
    onDismiss: () -> Unit
) {
    val activeWidgets = remember {
        mutableStateListOf(
            StarkPlacedWidget(id = "1", type = StarkWidgetType.ARC_REACTOR, isPinned = true),
            StarkPlacedWidget(id = "2", type = StarkWidgetType.VISION_SCANNER, isPinned = true),
            StarkPlacedWidget(id = "3", type = StarkWidgetType.MISSION_MATRIX, isPinned = true),
            StarkPlacedWidget(id = "4", type = StarkWidgetType.QUANTUM_ENVIRONMENT, isPinned = true)
        )
    }

    var selectedCatalogType by remember { mutableStateOf(StarkWidgetType.ARC_REACTOR) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(StarkColors.DarkVoidOpaque)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp)
            ) {
                // Top Stark Command Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(StarkColors.Cyan)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "STARK AR HUD",
                                    color = StarkColors.TextBright,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.2.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .background(StarkColors.Gold.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
                                        .border(1.dp, StarkColors.Gold, RoundedCornerShape(2.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "PHASE 2: STEP 4",
                                        color = StarkColors.Gold,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                            Text(
                                text = "HOLOGRAPHIC DASHBOARDS LIBRARY",
                                color = StarkColors.TextMuted,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }

                    // Close Button
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(StarkColors.CyanDim)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Exit Stark Preview",
                            tint = StarkColors.TextBright,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Spatial Status Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(StarkColors.DarkVoid)
                        .border(1.dp, StarkColors.CardBorder, RoundedCornerShape(4.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ACTIVE ROOM DASHBOARDS: ${activeWidgets.size}",
                        color = StarkColors.Cyan,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "F.R.I.D.A.Y. HUD // READY",
                        color = StarkColors.NeonGreen,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Scrollable Widgets Gallery
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(activeWidgets, key = { it.id }) { placed ->
                        StarkWidgetView(
                            type = placed.type,
                            isPinned = placed.isPinned,
                            onPinToggle = {
                                val idx = activeWidgets.indexOfFirst { it.id == placed.id }
                                if (idx != -1) {
                                    activeWidgets[idx] = placed.copy(isPinned = !placed.isPinned)
                                }
                            },
                            onClose = {
                                activeWidgets.removeIf { it.id == placed.id }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (activeWidgets.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(StarkColors.DarkVoid)
                                    .border(1.dp, StarkColors.CyanDim, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "ALL WIDGETS DISMISSED",
                                        color = StarkColors.TextMuted,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "USE CATALOG BELOW TO SPAWN DASHBOARDS",
                                        color = StarkColors.TextDim,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom Catalog Selector Bar
                StarkWidgetSelectorBar(
                    selectedType = selectedCatalogType,
                    onSelectType = { selectedCatalogType = it },
                    onSpawnWidget = { type ->
                        val newId = System.currentTimeMillis().toString()
                        activeWidgets.add(StarkPlacedWidget(id = newId, type = type, isPinned = true))
                        coroutineScope.launch {
                            listState.animateScrollToItem(activeWidgets.size - 1)
                        }
                    }
                )
            }
        }
    }
}
