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

enum class RoomPresetCoordinate(
    val label: String,
    val description: String,
    val worldVector: Vector3D
) {
    NORTH_WALL("FRONT SECTOR", "Central front anchor (Z +2.0m)", Vector3D(0.0f, 0.2f, 2.0f)),
    EAST_DESK("EAST PERIMETER", "Right wing 3D anchor (X +1.8m, Z +1.2m)", Vector3D(1.8f, -0.1f, 1.2f)),
    WEST_LAB("WEST TERMINAL", "Left wing 3D anchor (X -1.8m, Z +1.2m)", Vector3D(-1.8f, -0.1f, 1.2f)),
    CEILING_GRID("AERIAL MATRIX", "High ceiling overhead (Y +1.5m, Z +1.0m)", Vector3D(0.0f, 1.5f, 1.0f)),
    FLOOR_CORE("PEDESTAL BASE", "Lower ground core (Y -1.0m, Z +1.5f)", Vector3D(0.0f, -1.0f, 1.5f))
}

