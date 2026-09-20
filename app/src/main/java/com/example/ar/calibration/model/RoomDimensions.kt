package com.example.ar.calibration.model

/**
 * Geometric dimensions of a calibrated room footprint.
 * All units are in standard SI meters and square meters.
 */
data class RoomDimensions(
    val widthMeters: Float,
    val lengthMeters: Float,
    val areaSqMeters: Float,
    val perimeterMeters: Float
)
