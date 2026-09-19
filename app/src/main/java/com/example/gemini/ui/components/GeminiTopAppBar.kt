package com.example.gemini.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gemini.data.model.GeminiModel

/**
 * Top App Bar matching the Gemini mobile interface screenshot:
 * - Two-bar hamburger icon on the left
 * - "Flash Extended v" dropdown selector in the center-left
 * - Pen / new chat icon on right
 * - User profile avatar circle on far right
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiTopAppBar(
    currentModel: GeminiModel,
    onModelSelected: (GeminiModel) -> Unit,
    onMenuClick: () -> Unit,
    onNewChatClick: () -> Unit,
    onOpenArClick: (() -> Unit)? = null,
    userEmail: String = "roshanyadavofficial4@gmail.com",
    modifier: Modifier = Modifier
) {
    var modelMenuExpanded by remember { mutableStateOf(false) }
    var showAccountDialog by remember { mutableStateOf(false) }

    Surface(
        color = Color.Transparent,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Hamburger Menu (exact two parallel lines as seen in screenshot)
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onMenuClick,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("drawer_menu_button")
                ) {
                    Column(
                        modifier = Modifier.size(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .width(18.dp)
                                .height(2.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(Color(0xFF1F1F1F))
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        Box(
                            modifier = Modifier
                                .width(18.dp)
                                .height(2.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(Color(0xFF1F1F1F))
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Model Selector Pill (e.g. "Flash Extended v")
                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                modelMenuExpanded = true
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .testTag("model_selector_button")
                    ) {
                        Text(
                            text = "Flash",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1F1F1F)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Extended",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF757575)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Select Model",
                            tint = Color(0xFF757575),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Model Selection Dropdown
                    DropdownMenu(
                        expanded = modelMenuExpanded,
                        onDismissRequest = { modelMenuExpanded = false },
                        modifier = Modifier
                            .background(Color.White)
                            .padding(vertical = 4.dp)
                    ) {
                        GeminiModel.values().forEach { model ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = model.displayName,
                                                fontWeight = if (model == currentModel) FontWeight.Bold else FontWeight.Medium,
                                                color = Color(0xFF1F1F1F)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                color = if (model == currentModel) Color(0xFFE8F0FE) else Color(0xFFF1F3F4),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Text(
                                                    text = model.badge,
                                                    fontSize = 11.sp,
                                                    color = if (model == currentModel) Color(0xFF1A73E8) else Color(0xFF5F6368),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = model.description,
                                            fontSize = 12.sp,
                                            color = Color(0xFF757575)
                                        )
                                    }
                                },
                                trailingIcon = {
                                    if (model == currentModel) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color(0xFF1A73E8)
                                        )
                                    }
                                },
                                onClick = {
                                    onModelSelected(model)
                                    modelMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Right side: New Chat pen icon + Profile Avatar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // AR Workspace Mode Button
                if (onOpenArClick != null) {
                    IconButton(
                        onClick = onOpenArClick,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("open_ar_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ViewInAr,
                            contentDescription = "Open AR Workspace",
                            tint = Color(0xFF1A73E8),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Pen / Edit sparkle icon
                IconButton(
                    onClick = onNewChatClick,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("new_chat_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "New Chat",
                        tint = Color(0xFF444746),
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Profile Avatar (Google Blue circle with white 'r')
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0B57D0))
                        .clickable { showAccountDialog = true }
                        .testTag("user_avatar_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userEmail.firstOrNull()?.uppercase() ?: "R",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    if (showAccountDialog) {
        AlertDialog(
            onDismissRequest = { showAccountDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0B57D0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userEmail.firstOrNull()?.uppercase() ?: "R",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Google Account",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = userEmail,
                            fontSize = 13.sp,
                            color = Color(0xFF5F6368)
                        )
                    }
                }
            },
            text = {
                Column {
                    Surface(
                        color = Color(0xFFEFF6FF),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFF1A73E8),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Gemini Advanced Active",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1A73E8)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Your chats are saved locally and synced with your Gemini workspace.",
                        fontSize = 13.sp,
                        color = Color(0xFF444746)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAccountDialog = false }) {
                    Text("Done")
                }
            }
        )
    }
}
