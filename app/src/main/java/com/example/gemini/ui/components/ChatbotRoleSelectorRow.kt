package com.example.gemini.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gemini.data.model.ChatbotRole

@Composable
fun ChatbotRoleSelectorRow(
    currentRole: ChatbotRole,
    onRoleSelected: (ChatbotRole) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Subtle decorative header for the role section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Color(0xFF1A73E8),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "SPECIALIZED AI CHATBOT ROLES",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF5F6368),
                letterSpacing = 1.sp
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(ChatbotRole.values()) { role ->
                val isSelected = role == currentRole

                // Smooth background transitions matching premium Material 3 specs
                val backgroundColor by animateColorAsState(
                    targetValue = if (isSelected) Color(0xFFE8F0FE) else Color(0x99FFFFFF),
                    animationSpec = spring(),
                    label = "bgColor"
                )

                val borderColor by animateColorAsState(
                    targetValue = if (isSelected) Color(0xFF1A73E8) else Color(0xFFE0E0E0),
                    animationSpec = spring(),
                    label = "borderColor"
                )

                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) Color(0xFF1A73E8) else Color(0xFF444746),
                    animationSpec = spring(),
                    label = "contentColor"
                )

                val icon = when (role) {
                    ChatbotRole.GENERAL -> Icons.Default.AutoAwesome
                    ChatbotRole.CODER -> Icons.Default.Code
                    ChatbotRole.REASONING -> Icons.Default.Functions
                    ChatbotRole.FAST -> Icons.Default.Bolt
                }

                Surface(
                    color = backgroundColor,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                        .clickable { onRoleSelected(role) }
                        .testTag("role_pill_${role.id}"),
                    tonalElevation = if (isSelected) 2.dp else 0.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = role.displayName,
                            tint = contentColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = role.displayName,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isSelected) Color(0xFF1967D2) else Color(0xFF1F1F1F)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                // Badge showing the backing model short name
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = if (isSelected) Color(0xFFD2E3FC) else Color(0xFFF1F3F4),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = when (role.defaultModel.displayName) {
                                            "Gemini 3.1 Pro" -> "Pro"
                                            "Flash Lite" -> "Lite"
                                            else -> "Flash"
                                        },
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color(0xFF1A73E8) else Color(0xFF5F6368)
                                    )
                                }
                            }
                            Text(
                                text = role.description,
                                fontSize = 11.sp,
                                color = Color(0xFF5F6368)
                            )
                        }
                    }
                }
            }
        }
    }
}
