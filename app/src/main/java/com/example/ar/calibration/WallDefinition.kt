package com.example.ar.calibration

import com.example.ar.analysis.model.SpatialVector3

/**
 * Domain representation of a calibrated wall frame.
 * The wall coordinate system represents points on the wall using:
 * - Horizontal offset (u): distance along the wall tangent from the origin (left edge), 0 <= u <= length
 * - Vertical offset (v): height from the floor, 0 <= v <= height
 * - Normal offset (w): distance perpendicular to the wall plane (positive points into the room)
 */
data class WallDefinition(
    val id: String,
    val originInRoom: SpatialVector3,    // Bottom-left corner of the wall in Room Space
    val xAxis: SpatialVector3,           // Tangent (horizontal) unit vector in Room Space
    val yAxis: SpatialVector3,           // Vertical unit vector in Room Space (usually 0, 1, 0)
    val zAxis: SpatialVector3,           // Normal unit vector in Room Space (pointing into the room)
    val length: Float,                   // Length of the wall in meters
    val height: Float,                   // Height of the wall in meters (matches ceiling height)
    val startCornerIndex: Int,           // Start corner index in room corners list
    val endCornerIndex: Int              // End corner index in room corners list
)
