package com.example.gemini.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

/**
 * Exact Floating Pill Input Bar matching Google Gemini mobile screenshot:
 * - Floating pill shape with elevation and subtle border
 * - Plus icon (+) on the left for attachments
 * - "Gemini से कहें" placeholder
 * - Mic icon
 * - Gemini Live light-blue waveform button (or blue send arrow when text entered)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onMicClick: () -> Unit,
    onLiveClick: () -> Unit,
    selectedImageUri: Uri?,
    onImageSelected: (Uri?) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAttachmentSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    // Photo picker launcher (zero-permission Android Photo Picker)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onImageSelected(uri)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Thumbnail preview if an image is selected
        if (selectedImageUri != null) {
            Box(
                modifier = Modifier
                    .padding(bottom = 8.dp, start = 12.dp)
                    .size(68.dp)
            ) {
                AsyncImage(
                    model = selectedImageUri,
                    contentDescription = "Selected image",
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFFD3E3FD), RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF303030))
                        .clickable { onImageSelected(null) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove image",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        // Floating Pill Card
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = Color.White,
            shadowElevation = 5.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE3E8EF)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("gemini_floating_input_pill")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Plus Attachment Button (+)
                IconButton(
                    onClick = { showAttachmentSheet = true },
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("attachment_plus_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Attach file or image",
                        tint = Color(0xFF444746),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Text Input Area with "Ask Gemini" placeholder
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 40.dp, max = 120.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (text.isEmpty()) {
                        Text(
                            text = "Ask Gemini",
                            color = Color(0xFF747775),
                            fontSize = 17.sp
                        )
                    }
                    BasicTextField(
                        value = text,
                        onValueChange = onTextChange,
                        textStyle = TextStyle(
                            color = Color(0xFF1F1F1F),
                            fontSize = 17.sp,
                            lineHeight = 22.sp
                        ),
                        cursorBrush = SolidColor(Color(0xFF0B57D0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("gemini_text_input")
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Microphone Icon
                IconButton(
                    onClick = onMicClick,
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("gemini_mic_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice search",
                        tint = Color(0xFF444746),
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Dynamic Right Action: Gemini Live waveform button OR Send button
                if (text.isBlank()) {
                    // Gemini Live Pill Button (Light blue pill with waveform vertical bars)
                    Box(
                        modifier = Modifier
                            .size(width = 46.dp, height = 42.dp)
                            .clip(RoundedCornerShape(21.dp))
                            .background(Color(0xFFD3E3FD))
                            .clickable { onLiveClick() }
                            .testTag("gemini_live_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        // Drawing authentic 3 waveform vertical bars
                        GeminiLiveWaveformIcon(tint = Color(0xFF041E49))
                    }
                } else {
                    // Blue Send Button with upward arrow
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0B57D0))
                            .clickable { onSend() }
                            .testTag("gemini_send_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Send message",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }

    // Attachment Modal Sheet
    if (showAttachmentSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAttachmentSheet = false },
            sheetState = sheetState,
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Add to prompt",
                    fontSize = 18.sp,
                    color = Color(0xFF1F1F1F)
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            showAttachmentSheet = false
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "Gallery",
                        tint = Color(0xFF0B57D0),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Gallery / Photos",
                            fontSize = 16.sp,
                            color = Color(0xFF1F1F1F)
                        )
                        Text(
                            text = "Upload an image for multimodal analysis",
                            fontSize = 12.sp,
                            color = Color(0xFF757575)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/**
 * Draws the vertical audio waveform bars for the Gemini Live button.
 */
@Composable
fun GeminiLiveWaveformIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color(0xFF041E49)
) {
    Canvas(
        modifier = modifier.size(width = 18.dp, height = 18.dp)
    ) {
        val barWidth = 2.4.dp.toPx()
        val cornerRadius = CornerRadius(1.2.dp.toPx(), 1.2.dp.toPx())

        // Bar 1 (left)
        val h1 = 10.dp.toPx()
        drawRoundRect(
            color = tint,
            topLeft = Offset(1.5.dp.toPx(), (size.height - h1) / 2f),
            size = Size(barWidth, h1),
            cornerRadius = cornerRadius
        )

        // Bar 2 (middle - tallest)
        val h2 = 16.dp.toPx()
        drawRoundRect(
            color = tint,
            topLeft = Offset(7.5.dp.toPx(), (size.height - h2) / 2f),
            size = Size(barWidth, h2),
            cornerRadius = cornerRadius
        )

        // Bar 3 (right)
        val h3 = 11.dp.toPx()
        drawRoundRect(
            color = tint,
            topLeft = Offset(13.5.dp.toPx(), (size.height - h3) / 2f),
            size = Size(barWidth, h3),
            cornerRadius = cornerRadius
        )
    }
}
