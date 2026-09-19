package com.example.gemini.ui.stark

enum class StarkWidgetType(
    val title: String,
    val subtitle: String,
    val tag: String
) {
    ARC_REACTOR(
        title = "ARC CORE // MK-85",
        subtitle = "POWER & AI TELEMETRY",
        tag = "PWR-01"
    ),
    VISION_SCANNER(
        title = "TACTICAL SCANNER",
        subtitle = "SPATIAL RECON & LIDAR",
        tag = "SCN-02"
    ),
    MISSION_MATRIX(
        title = "DIRECTIVES MATRIX",
        subtitle = "STARK PROTOCOL QUEUE",
        tag = "MTX-03"
    ),
    QUANTUM_ENVIRONMENT(
        title = "QUANTUM CHRONO",
        subtitle = "ATMOSPHERE & GEO-SENSORS",
        tag = "ENV-04"
    )
}

data class StarkTaskItem(
    val id: String,
    val code: String,
    val title: String,
    val priority: String, // "ALPHA-1", "BETA-2", "CRITICAL"
    val isCompleted: Boolean = false
)

data class StarkPlacedWidget(
    val id: String,
    val type: StarkWidgetType,
    val yaw: Float = 0f,
    val pitch: Float = 0f,
    val distanceMeters: Float = 1.8f,
    val isPinned: Boolean = true
)
