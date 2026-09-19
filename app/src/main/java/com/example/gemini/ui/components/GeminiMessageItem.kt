package com.example.gemini.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.gemini.data.model.ChatMessage

@Composable
fun GeminiMessageItem(
    message: ChatMessage,
    onLikeToggle: (Boolean?) -> Unit,
    onModifyResponse: (String) -> Unit,
    onSpeak: (String) -> Unit,
    isSpeaking: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    if (message.isUser) {
        // User Message Bubble (Right-aligned, soft neutral background)
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.fillMaxWidth(0.85f)
            ) {
                if (message.imageUri != null) {
                    AsyncImage(
                        model = Uri.parse(message.imageUri),
                        contentDescription = "Attached image",
                        modifier = Modifier
                            .padding(bottom = 6.dp)
                            .size(160.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, Color(0xFFE0E3E7), RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFF0F4F9),
                    modifier = Modifier.testTag("user_message_${message.id}")
                ) {
                    Text(
                        text = message.content,
                        fontSize = 16.sp,
                        color = Color(0xFF1F1F1F),
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            }
        }
    } else {
        // Gemini Response (Left-aligned, star icon + formatted markdown + action bar)
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header with Gemini Star
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    GeminiStarIcon(
                        size = 24.dp,
                        isPulsing = message.isThinking
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Gemini",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1F1F1F)
                    )
                }

                if (message.isThinking) {
                    // Shimmering thinking state
                    ThinkingIndicator()
                } else {
                    // Formatted Markdown Content
                    GeminiMarkdownContent(
                        content = message.content,
                        onCopyCode = { code ->
                            copyToClipboard(context, code, "Code copied to clipboard")
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Gemini Action Toolbar (Like, Dislike, Modify, Share, Copy, Grounding, Listen)
                    GeminiActionBar(
                        isLiked = message.isLiked,
                        onLike = { onLikeToggle(if (message.isLiked == true) null else true) },
                        onDislike = { onLikeToggle(if (message.isLiked == false) null else false) },
                        onCopy = {
                            copyToClipboard(context, message.content, "Response copied to clipboard")
                        },
                        onShare = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, message.content)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Gemini response"))
                        },
                        onModify = onModifyResponse,
                        onSpeak = { onSpeak(message.content) },
                        isSpeaking = isSpeaking,
                        onGroundingCheck = {
                            val browserIntent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://www.google.com/search?q=" + Uri.encode(message.content.take(80)))
                            )
                            context.startActivity(browserIntent)
                        }
                    )
                }
            }
        }
    }
}

/**
 * Renders Markdown formatting: Headings, bullet points, bold/italic, and code blocks.
 */
@Composable
fun GeminiMarkdownContent(
    content: String,
    onCopyCode: (String) -> Unit
) {
    val paragraphs = content.split("\n\n")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp)
    ) {
        for (paragraph in paragraphs) {
            val trimmed = paragraph.trim()
            if (trimmed.isEmpty()) continue

            when {
                // Code block (```lang ... ```)
                trimmed.startsWith("```") -> {
                    val lines = trimmed.lines()
                    val lang = lines.firstOrNull()?.removePrefix("```")?.trim()?.ifEmpty { "CODE" } ?: "CODE"
                    val codeContent = lines.drop(1).dropLastWhile { it.startsWith("```") }.joinToString("\n")

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1E1F22),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Column {
                            // Code Header Bar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF2B2D30))
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = lang.uppercase(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB0B0B0)
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { onCopyCode(codeContent) }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy code",
                                        tint = Color(0xFFDFE1E5),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Copy",
                                        fontSize = 12.sp,
                                        color = Color(0xFFDFE1E5)
                                    )
                                }
                            }
                            // Code Content
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(14.dp)
                            ) {
                                Text(
                                    text = codeContent,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    color = Color(0xFFE6EDF3),
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
                // Heading 1 or 2 or 3 (#, ##, ###)
                trimmed.startsWith("###") -> {
                    Text(
                        text = trimmed.removePrefix("###").trim(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F1F1F),
                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                    )
                }
                trimmed.startsWith("##") -> {
                    Text(
                        text = trimmed.removePrefix("##").trim(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F1F1F),
                        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
                    )
                }
                trimmed.startsWith("#") -> {
                    Text(
                        text = trimmed.removePrefix("#").trim(),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F1F1F),
                        modifier = Modifier.padding(top = 14.dp, bottom = 8.dp)
                    )
                }
                // Bullet or Numbered Lists
                trimmed.lines().any { it.trimStart().startsWith("- ") || it.trimStart().startsWith("• ") || it.trimStart().matches(Regex("^\\d+\\..*")) } -> {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        for (line in trimmed.lines()) {
                            val l = line.trim()
                            if (l.isEmpty()) continue
                            Row(
                                modifier = Modifier.padding(vertical = 3.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                if (l.startsWith("- ") || l.startsWith("• ") || l.startsWith("* ")) {
                                    Text(
                                        text = "•",
                                        fontSize = 16.sp,
                                        color = Color(0xFF0B57D0),
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    FormattedInlineText(
                                        text = l.replace(Regex("^[-*•]\\s*"), ""),
                                        modifier = Modifier.weight(1f)
                                    )
                                } else if (l.matches(Regex("^\\d+\\..*"))) {
                                    val prefix = l.substringBefore(".") + "."
                                    val rest = l.substringAfter(".").trim()
                                    Text(
                                        text = prefix,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1F1F1F),
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    FormattedInlineText(text = rest, modifier = Modifier.weight(1f))
                                } else {
                                    FormattedInlineText(text = l, modifier = Modifier.fillMaxWidth())
                                }
                            }
                        }
                    }
                }
                // Regular Paragraph
                else -> {
                    FormattedInlineText(
                        text = trimmed,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * Formats inline bold (**text**), italic (*text*), and inline code (`code`).
 */
@Composable
fun FormattedInlineText(
    text: String,
    modifier: Modifier = Modifier
) {
    val annotated = remember(text) {
        buildAnnotatedString {
            // Simple robust regex parsing for **bold**, *italic*, `code`
            val pattern = Regex("(\\*\\*.*?\\*\\*|\\*.*?\\*|`.*?`)")
            var lastIndex = 0

            for (match in pattern.findAll(text)) {
                val range = match.range
                if (range.first > lastIndex) {
                    append(text.substring(lastIndex, range.first))
                }
                val token = match.value
                when {
                    token.startsWith("**") && token.endsWith("**") && token.length >= 4 -> {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF1F1F1F))) {
                            append(token.substring(2, token.length - 2))
                        }
                    }
                    token.startsWith("`") && token.endsWith("`") && token.length >= 2 -> {
                        withStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                background = Color(0xFFEFF2F6),
                                color = Color(0xFF0B57D0)
                            )
                        ) {
                            append(" " + token.substring(1, token.length - 1) + " ")
                        }
                    }
                    token.startsWith("*") && token.endsWith("*") && token.length >= 2 -> {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(token.substring(1, token.length - 1))
                        }
                    }
                    else -> append(token)
                }
                lastIndex = range.last + 1
            }
            if (lastIndex < text.length) {
                append(text.substring(lastIndex))
            }
        }
    }

    Text(
        text = annotated,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        color = Color(0xFF1F1F1F),
        modifier = modifier
    )
}

/**
 * Action toolbar matching Google Gemini: Like, Dislike, Modify, Share, Copy, Google Search, Speak
 */
@Composable
fun GeminiActionBar(
    isLiked: Boolean?,
    onLike: () -> Unit,
    onDislike: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onModify: (String) -> Unit,
    onSpeak: () -> Unit,
    isSpeaking: Boolean,
    onGroundingCheck: () -> Unit
) {
    var modifyMenuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        // Thumbs Up
        IconButton(
            onClick = onLike,
            modifier = Modifier.size(34.dp)
        ) {
            Icon(
                imageVector = if (isLiked == true) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                contentDescription = "Good response",
                tint = if (isLiked == true) Color(0xFF0B57D0) else Color(0xFF5F6368),
                modifier = Modifier.size(17.dp)
            )
        }

        // Thumbs Down
        IconButton(
            onClick = onDislike,
            modifier = Modifier.size(34.dp)
        ) {
            Icon(
                imageVector = if (isLiked == false) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
                contentDescription = "Bad response",
                tint = if (isLiked == false) Color(0xFFD93025) else Color(0xFF5F6368),
                modifier = Modifier.size(17.dp)
            )
        }

        // Modify Response Menu (Shorter, Longer, Simpler, Casual, Professional)
        Box {
            IconButton(
                onClick = { modifyMenuExpanded = true },
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Modify response",
                    tint = Color(0xFF5F6368),
                    modifier = Modifier.size(18.dp)
                )
            }

            DropdownMenu(
                expanded = modifyMenuExpanded,
                onDismissRequest = { modifyMenuExpanded = false },
                modifier = Modifier.background(Color.White)
            ) {
                listOf(
                    "Shorter",
                    "Longer",
                    "Simpler",
                    "More casual",
                    "More professional"
                ).forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            modifyMenuExpanded = false
                            onModify(option)
                        }
                    )
                }
            }
        }

        // Share
        IconButton(
            onClick = onShare,
            modifier = Modifier.size(34.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "Share",
                tint = Color(0xFF5F6368),
                modifier = Modifier.size(17.dp)
            )
        }

        // Copy
        IconButton(
            onClick = onCopy,
            modifier = Modifier.size(34.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.ContentCopy,
                contentDescription = "Copy text",
                tint = Color(0xFF5F6368),
                modifier = Modifier.size(17.dp)
            )
        }

        // Speak / Read Aloud (Android TTS)
        IconButton(
            onClick = onSpeak,
            modifier = Modifier.size(34.dp)
        ) {
            Icon(
                imageVector = if (isSpeaking) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = if (isSpeaking) "Stop reading" else "Listen",
                tint = if (isSpeaking) Color(0xFF0B57D0) else Color(0xFF5F6368),
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Google Double-check grounding chip
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFF1F3F4),
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onGroundingCheck)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Color(0xFF5F6368),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Double-check",
                    fontSize = 11.sp,
                    color = Color(0xFF5F6368),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Animated thinking indicator with gentle shimmer.
 */
@Composable
fun ThinkingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "thinking_shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "thinking_alpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Gemini is thinking…",
            fontSize = 14.sp,
            color = Color(0xFF0B57D0).copy(alpha = alpha),
            fontWeight = FontWeight.Medium
        )
    }
}

private fun copyToClipboard(context: Context, text: String, toastMessage: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Gemini Response", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
}
