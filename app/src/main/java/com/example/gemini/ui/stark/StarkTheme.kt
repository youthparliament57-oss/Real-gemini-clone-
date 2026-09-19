package com.example.gemini.ui.stark

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object StarkColors {
    val Cyan = Color(0xFF00E5FF)
    val CyanGlow = Color(0x6600E5FF)
    val CyanDeep = Color(0xFF0097A7)
    val CyanDim = Color(0x3300E5FF)
    
    val ElectricBlue = Color(0xFF00B0FF)
    val DarkVoid = Color(0x73050B14) // ~45% opacity translucent Stark lab HUD glass
    val DarkVoidOpaque = Color(0xB3050B14) // ~70% opacity for inner tiles
    val CardBorder = Color(0x6600E5FF)
    val CardBorderActive = Color(0xCC00E5FF)
    
    val Gold = Color(0xFFFFD600)
    val Amber = Color(0xFFFF9100)
    val DangerRed = Color(0xFFFF1744)
    val NeonGreen = Color(0xFF00E676)
    
    val TextBright = Color(0xFFE0F7FA)
    val TextMuted = Color(0xFF80DEEA)
    val TextDim = Color(0xFF4DD0E1)
    
    val HologramGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0x3300E5FF),
            Color(0x0D00E5FF),
            Color(0x2600B0FF)
        )
    )

    val ArcReactorGradient = Brush.radialGradient(
        colors = listOf(
            Color(0xFFFFFFFF),
            Color(0xFF80D8FF),
            Color(0xFF00E5FF),
            Color(0x0000E5FF)
        )
    )
}
