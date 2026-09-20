package com.example.ar.calibration

import com.example.ar.analysis.model.SpatialVector3
import kotlin.math.abs

/**
 * Mathematically handles rigid body coordinate transformations between ARCore World Space
 * and the application-level Room Space.
 *
 * Coordinate System Definition:
 * - Handedness: Right-Handed Cartesian Coordinate System (RHS)
 * - Origin (0, 0, 0): Physical location of Corner 1 on the floor plane
 * - +X Axis: Normalized horizontal direction from Corner 1 along Wall 1 toward Corner 2
 * - +Y Axis: Normalized vertical upward direction perpendicular to the floor plane (gravity-aligned)
 * - +Z Axis: Derived horizontal axis pointing into the room interior (Z = X x Y)
 *
 * All transformations are isometric (strictly Euclidean rigid transformations: Rotation + Translation, Scale = 1.0)
 * and all units remain strictly in meters.
 *
 * Pure domain class: Independent of ARCore Session, Frame, Plane, Anchor, or Pose.
 */
data class CoordinateTransform(
    val roomOrigin: SpatialVector3,
    val xAxis: SpatialVector3,
    val yAxis: SpatialVector3,
    val zAxis: SpatialVector3
) {
    companion object {
        private const val EPSILON = 1e-4f

        /**
         * Constructs a strictly orthonormal Room coordinate frame from Corner 1, Corner 2, and the floor normal.
         * Returns null if Corner 1 and Corner 2 are degenerate/collinear with the vertical normal.
         */
        fun createOrthonormal(
            corner1: SpatialVector3,
            corner2: SpatialVector3,
            floorNormal: SpatialVector3 = SpatialVector3(0f, 1f, 0f)
        ): CoordinateTransform? {
            val yUnit = floorNormal.normalized()
            val rawX = corner2 - corner1

            // Project rawX onto the floor plane perpendicular to yUnit
            val xProj = rawX - (yUnit * rawX.dot(yUnit))
            val xLength = xProj.length()
            if (xLength < 0.10f) {
                // Corner 1 and Corner 2 are nearly collinear with the vertical axis or too close
                return null
            }

            val xUnit = xProj.normalized()
            // Right-handed Z axis = X x Y
            val zUnit = xUnit.cross(yUnit).normalized()

            return CoordinateTransform(
                roomOrigin = corner1,
                xAxis = xUnit,
                yAxis = yUnit,
                zAxis = zUnit
            )
        }
    }

    /**
     * Checks if the coordinate frame basis vectors satisfy orthonormal properties:
     * - ||X|| == 1, ||Y|| == 1, ||Z|| == 1
     * - X . Y == 0, Y . Z == 0, Z . X == 0
     */
    fun isOrthonormal(): Boolean {
        val xNorm = abs(xAxis.length() - 1f) < EPSILON
        val yNorm = abs(yAxis.length() - 1f) < EPSILON
        val zNorm = abs(zAxis.length() - 1f) < EPSILON
        val xyDot = abs(xAxis.dot(yAxis)) < EPSILON
        val yzDot = abs(yAxis.dot(zAxis)) < EPSILON
        val zxDot = abs(zAxis.dot(xAxis)) < EPSILON
        return xNorm && yNorm && zNorm && xyDot && yzDot && zxDot
    }

    /**
     * Converts a 3D point from ARCore World Space to Room Space.
     */
    fun worldToRoom(worldPoint: SpatialVector3): SpatialVector3 {
        val delta = worldPoint - roomOrigin
        return SpatialVector3(
            delta.dot(xAxis),
            delta.dot(yAxis),
            delta.dot(zAxis)
        )
    }

    /**
     * Converts a 3D point from Room Space to ARCore World Space.
     */
    fun roomToWorld(roomPoint: SpatialVector3): SpatialVector3 {
        return roomOrigin +
                (xAxis * roomPoint.x) +
                (yAxis * roomPoint.y) +
                (zAxis * roomPoint.z)
    }

    /**
     * Converts a 3D directional vector from ARCore World Space to Room Space (without translation).
     */
    fun worldToRoomDir(worldDir: SpatialVector3): SpatialVector3 {
        return SpatialVector3(
            worldDir.dot(xAxis),
            worldDir.dot(yAxis),
            worldDir.dot(zAxis)
        )
    }

    /**
     * Converts a 3D directional vector from Room Space to ARCore World Space (without translation).
     */
    fun roomToWorldDir(roomDir: SpatialVector3): SpatialVector3 {
        return (xAxis * roomDir.x) +
                (yAxis * roomDir.y) +
                (zAxis * roomDir.z)
    }
}
