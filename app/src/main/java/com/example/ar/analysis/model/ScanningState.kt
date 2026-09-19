package com.example.ar.analysis.model

/**
 * State machine for the room environment scanning phase.
 */
enum class ScanningState {
    /** Actively searching for surfaces; prompts user to move device. */
    SCANNING,

    /** A plausible room floor plane has been detected. */
    FLOOR_DETECTED,

    /** Plausible vertical wall surfaces have been detected. */
    WALLS_DETECTED,

    /** Analyzing intersecting geometry, corners, and room extents. */
    GEOMETRY_ANALYZING,

    /** Sufficient environment geometry collected; ready for subsequent operations. */
    SCAN_READY,

    /** Environment information is currently insufficient (e.g. low area, no walls). */
    SCAN_INSUFFICIENT,

    /** ARCore camera tracking was temporarily degraded or lost. */
    TRACKING_LOST
}
