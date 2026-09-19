package com.example.ar.analysis.model

import kotlin.math.sqrt

/**
 * Pure domain representation of a 2D planar coordinate.
 */
data class SpatialPoint2D(
    val x: Float,
    val y: Float
) {
    fun distanceTo(other: SpatialPoint2D): Float {
        val dx = x - other.x
        val dy = y - other.y
        return sqrt(dx * dx + dy * dy)
    }
}
