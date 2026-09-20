package com.example.ar.calibration

import com.example.ar.analysis.model.RoomScanResult
import com.example.ar.analysis.model.SpatialVector3
import kotlin.math.abs
import kotlin.math.atan2

/**
 * Manager component responsible for room calibration logic and transformations.
 * Performs mathematical validations, coordinates states, sorts corners, and creates results.
 */
class CalibrationManager {

    companion object {
        const val DEFAULT_CEILING_HEIGHT_METERS = 3.048f // ~10 feet
        const val MIN_CEILING_HEIGHT_METERS = 1.5f
        const val MAX_CEILING_HEIGHT_METERS = 10.0f
        const val MIN_CORNER_DISTANCE_METERS = 0.5f
        const val MAX_FLOOR_DEVIATION_METERS = 0.15f
    }

    /**
     * Sorts 3D corners in a clockwise winding order on the horizontal floor plane.
     */
    fun sortCornersClockwise(corners: List<SpatialVector3>): List<SpatialVector3> {
        if (corners.size < 3) return corners
        val centerX = corners.map { it.x }.average().toFloat()
        val centerZ = corners.map { it.z }.average().toFloat()
        return corners.sortedBy { c ->
            atan2(c.z - centerZ, c.x - centerX)
        }
    }

    /**
     * Validates whether the 4 collected corners form a physically plausible room.
     */
    fun validateCalibration(
        corners: List<SpatialVector3>,
        ceilingHeight: Float
    ): Pair<Boolean, String?> {
        if (corners.size != 4) {
            return false to "Calibration requires exactly 4 distinct floor corners."
        }

        // 1. Check distinct corners
        for (i in 0 until 3) {
            for (j in i + 1 until 4) {
                if (corners[i].distanceTo(corners[j]) < MIN_CORNER_DISTANCE_METERS) {
                    return false to "Corners are too close to each other. Please select distinct corners."
                }
            }
        }

        // 2. Coplanar check (all corners must sit on roughly the same horizontal floor elevation)
        val averageY = corners.map { it.y }.average().toFloat()
        for (c in corners) {
            if (abs(c.y - averageY) > MAX_FLOOR_DEVIATION_METERS) {
                return false to "Selected corners do not lie on the same level floor plane."
            }
        }

        // 3. Orthonormal axis generation check (collinear/degenerate check)
        val rawX = corners[1] - corners[0]
        val yAxis = SpatialVector3(0f, 1f, 0f)
        val xProj = rawX - (yAxis * rawX.dot(yAxis))
        if (xProj.length() < 0.1f) {
            return false to "Corner 1 and Corner 2 are aligned vertically, creating a degenerate coordinate axis."
        }

        // 4. Transform corners to temporary Room Space to check bounding box dimensions
        val roomXBasis = xProj.normalized()
        val roomYBasis = yAxis
        val roomZBasis = roomXBasis.cross(roomYBasis).normalized()

        val roomOrigin = corners[0]
        val roomCorners = corners.map { c ->
            val d = c - roomOrigin
            SpatialVector3(d.dot(roomXBasis), d.dot(roomYBasis), d.dot(roomZBasis))
        }

        val xs = roomCorners.map { it.x }
        val zs = roomCorners.map { it.z }
        val width = xs.maxOrNull()!! - xs.minOrNull()!!
        val depth = zs.maxOrNull()!! - zs.minOrNull()!!

        if (width < 1.0f || width > 30.0f || depth < 1.0f || depth > 30.0f) {
            return false to "Plausible room size exceeded. Calculated width: ${String.format("%.2f", width)}m, depth: ${String.format("%.2f", depth)}m (Max 30m, Min 1m)."
        }

        // 5. Ceiling height validation
        if (ceilingHeight < MIN_CEILING_HEIGHT_METERS || ceilingHeight > MAX_CEILING_HEIGHT_METERS) {
            return false to "Ceiling height (${String.format("%.2f", ceilingHeight)}m) must be between 1.5m and 10.0m."
        }

        return true to null
    }

    /**
     * Builds a full CalibrationResult from 4 confirmed/collected corners.
     */
    fun buildCalibrationResult(
        rawCorners: List<SpatialVector3>,
        ceilingHeight: Float,
        method: CalibrationMethod,
        confidence: Float
    ): CalibrationResult? {
        val sortedCorners = sortCornersClockwise(rawCorners)
        val (isValid, _) = validateCalibration(sortedCorners, ceilingHeight)
        if (!isValid) return null

        val roomOrigin = sortedCorners[0]
        val floorNormal = SpatialVector3(0f, 1f, 0f) // gravity-aligned vertical in ARCore

        // Compute orthonormal Room Space axes
        val rawX = sortedCorners[1] - roomOrigin
        val xProj = rawX - (floorNormal * rawX.dot(floorNormal))
        val xAxis = xProj.normalized()
        val yAxis = floorNormal
        val zAxis = xAxis.cross(yAxis).normalized()

        // Map corners to Room coordinates
        val calibratedCornersRoom = sortedCorners.map { c ->
            val d = c - roomOrigin
            SpatialVector3(d.dot(xAxis), d.dot(yAxis), d.dot(zAxis))
        }

        val xs = calibratedCornersRoom.map { it.x }
        val zs = calibratedCornersRoom.map { it.z }
        val roomWidth = xs.maxOrNull()!! - xs.minOrNull()!!
        val roomDepth = zs.maxOrNull()!! - zs.minOrNull()!!

        // Reconstruct local wall frames
        val wallDefinitions = generateWallDefinitions(calibratedCornersRoom, ceilingHeight)

        return CalibrationResult(
            roomOrigin = roomOrigin,
            floorNormal = floorNormal,
            xAxis = xAxis,
            yAxis = yAxis,
            zAxis = zAxis,
            calibratedCornersWorld = sortedCorners,
            calibratedCornersRoom = calibratedCornersRoom,
            wallDefinitions = wallDefinitions,
            roomWidth = roomWidth,
            roomDepth = roomDepth,
            ceilingHeight = ceilingHeight,
            calibrationMethod = method,
            confidence = confidence
        )
    }

    /**
     * Attempts automatic calibration using RoomAnalyzer scan results.
     */
    fun attemptAutoCalibration(
        scanResult: RoomScanResult,
        ceilingHeight: Float = DEFAULT_CEILING_HEIGHT_METERS
    ): CalibrationResult? {
        if (!scanResult.isReady || scanResult.confidence < 0.60f) {
            return null
        }
        val corners = scanResult.candidateCorners.map { it.position }
        if (corners.size < 4) {
            return null
        }

        // Take top 4 highest confidence corners
        val top4Corners = scanResult.candidateCorners
            .sortedByDescending { it.confidence }
            .take(4)
            .map { it.position }

        return buildCalibrationResult(
            rawCorners = top4Corners,
            ceilingHeight = ceilingHeight,
            method = CalibrationMethod.AUTOMATIC,
            confidence = scanResult.confidence
        )
    }

    /**
     * Generates a set of wall frames from the sorted room corners.
     */
    private fun generateWallDefinitions(
        cornersInRoom: List<SpatialVector3>,
        ceilingHeight: Float
    ): List<WallDefinition> {
        val walls = mutableListOf<WallDefinition>()
        val n = cornersInRoom.size

        // Geometric centroid in Room coordinates
        val center = SpatialVector3(
            cornersInRoom.map { it.x }.average().toFloat(),
            cornersInRoom.map { it.y }.average().toFloat(),
            cornersInRoom.map { it.z }.average().toFloat()
        )

        for (i in 0 until n) {
            val startIdx = i
            val endIdx = (i + 1) % n
            val start = cornersInRoom[startIdx]
            val end = cornersInRoom[endIdx]

            val wallId = "wall_${i + 1}"
            val origin = start
            val length = start.distanceTo(end)

            // Horizontal wall tangent in Room Space
            val tangentRaw = end - start
            val tangent = SpatialVector3(tangentRaw.x, 0f, tangentRaw.z).normalized()
            val vertical = SpatialVector3(0f, 1f, 0f)

            // Normal vector (tangent x vertical)
            var normal = tangent.cross(vertical).normalized()

            // Ensure normal points into the interior of the room
            val toCenter = center - start
            if (toCenter.dot(normal) < 0f) {
                normal = normal * -1f
            }

            walls.add(
                WallDefinition(
                    id = wallId,
                    originInRoom = origin,
                    xAxis = tangent,
                    yAxis = vertical,
                    zAxis = normal,
                    length = length,
                    height = ceilingHeight,
                    startCornerIndex = startIdx,
                    endCornerIndex = endIdx
                )
            )
        }
        return walls
    }
}
