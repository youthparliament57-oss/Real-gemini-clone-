package com.example.gemini.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class SuggestionChipItem(
    val title: String,
    val icon: ImageVector,
    val prompt: String
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GeminiWelcomeScreen(
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val suggestions = listOf(
        SuggestionChipItem(
            title = "Explore new ideas",
            icon = Icons.Outlined.Lightbulb,
            prompt = "Give me some unique and creative business ideas to explore."
        ),
        SuggestionChipItem(
            title = "Draft an email",
            icon = Icons.Outlined.Create,
            prompt = "Draft a professional and polite email requesting time off."
        ),
        SuggestionChipItem(
            title = "Help with coding",
            icon = Icons.Outlined.Code,
            prompt = "Explain Kotlin Coroutines and StateFlow with a simple, clear example."
        ),
        SuggestionChipItem(
            title = "Understand concepts",
            icon = Icons.Outlined.Psychology,
            prompt = "Explain the fundamental principles of Quantum Computing in simple terms."
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Centered authentic 4-pointed Gemini Star
        GeminiStarIcon(
            size = 56.dp,
            modifier = Modifier.testTag("gemini_welcome_star")
        )

        Spacer(modifier = Modifier.height(28.dp))

        // English welcome text matching authentic Google Gemini mobile app
        Text(
            text = "What would you like\nto explore today?",
            fontSize = 32.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 40.sp,
            textAlign = TextAlign.Center,
            color = Color(0xFF1F1F1F),
            modifier = Modifier.testTag("gemini_welcome_heading")
        )

        Spacer(modifier = Modifier.height(36.dp))

        // Interactive Suggestion Chips
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            suggestions.forEach { suggestion ->
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE0E3E7)),
                    modifier = Modifier
                        .clickable { onSuggestionClick(suggestion.prompt) }
                        .testTag("suggestion_chip_${suggestion.title}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = suggestion.icon,
                            contentDescription = null,
                            tint = Color(0xFF1A73E8),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = suggestion.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF3C4043)
                        )
                    }
                }
            }
        }
    }
}
