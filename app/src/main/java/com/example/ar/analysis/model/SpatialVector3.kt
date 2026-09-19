package com.example.ar.analysis.model

import kotlin.math.sqrt

/**
 * Pure domain representation of a 3D coordinate or vector.
 * Completely decoupled from ARCore Pose or native matrix math.
 */
data class SpatialVector3(
    val x: Float,
    val y: Float,
    val z: Float
) {
    fun length(): Float = sqrt(x * x + y * y + z * z)

    fun distanceTo(other: SpatialVector3): Float {
        val dx = x - other.x
        val dy = y - other.y
        val dz = z - other.z
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    fun normalized(): SpatialVector3 {
        val len = length()
        return if (len > 0.00001f) {
            SpatialVector3(x / len, y / len, z / len)
        } else {
            SpatialVector3(0f, 0f, 0f)
        }
    }

    fun dot(other: SpatialVector3): Float = x * other.x + y * other.y + z * other.z

    operator fun plus(other: SpatialVector3): SpatialVector3 =
        SpatialVector3(x + other.x, y + other.y, z + other.z)

    operator fun minus(other: SpatialVector3): SpatialVector3 =
        SpatialVector3(x - other.x, y - other.y, z - other.z)

    operator fun times(scalar: Float): SpatialVector3 =
        SpatialVector3(x * scalar, y * scalar, z * scalar)
}
