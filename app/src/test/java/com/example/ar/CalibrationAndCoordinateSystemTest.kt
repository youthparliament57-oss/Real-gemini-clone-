package com.example.ar

import com.example.ar.analysis.model.SpatialVector3
import com.example.ar.calibration.CalibrationManager
import com.example.ar.calibration.CalibrationMethod
import com.example.ar.calibration.CoordinateTransform
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.math.abs

class CalibrationAndCoordinateSystemTest {

    private lateinit var calibrationManager: CalibrationManager

    @Before
    fun setUp() {
        calibrationManager = CalibrationManager()
    }

    @Test
    fun sortCornersClockwise_correctlyOrdersUnsortedCorners() {
        val c1 = SpatialVector3(1f, 0f, 1f)
        val c2 = SpatialVector3(-1f, 0f, 1f)
        val c3 = SpatialVector3(-1f, 0f, -1f)
        val c4 = SpatialVector3(1f, 0f, -1f)

        // Shuffled corners input
        val unsorted = listOf(c3, c1, c4, c2)
        val sorted = calibrationManager.sortCornersClockwise(unsorted)

        assertEquals(4, sorted.size)
        // Clockwise winding on the XZ plane with atan2
        // Since we sort by angle: atan2(z, x)
        // Center is (0, 0)
        // c3: atan2(-1, -1) = -3pi/4
        // c4: atan2(-1, 1) = -pi/4
        // c1: atan2(1, 1) = pi/4
        // c2: atan2(1, -1) = 3pi/4
        // Sorted output should be c3, c4, c1, c2 (increasing order of angle)
        assertEquals(c3, sorted[0])
        assertEquals(c4, sorted[1])
        assertEquals(c1, sorted[2])
        assertEquals(c2, sorted[3])
    }

    @Test
    fun validateCalibration_rejectsTooCloseCorners() {
        val corners = listOf(
            SpatialVector3(0f, 0f, 0f),
            SpatialVector3(0.2f, 0f, 0f), // Too close! (Limit is 0.5m)
            SpatialVector3(0f, 0f, 4f),
            SpatialVector3(4f, 0f, 4f)
        )
        val (isValid, errorReason) = calibrationManager.validateCalibration(corners, 2.5f)
        assertFalse(isValid)
        assertTrue(errorReason?.contains("too close") == true)
    }

    @Test
    fun validateCalibration_rejectsNonCoplanarCorners() {
        val corners = listOf(
            SpatialVector3(0f, 0f, 0f),
            SpatialVector3(4f, 0.3f, 0f), // Too high! (Limit is 0.15m deviation)
            SpatialVector3(4f, 0f, 4f),
            SpatialVector3(0f, 0f, 4f)
        )
        val (isValid, errorReason) = calibrationManager.validateCalibration(corners, 2.5f)
        assertFalse(isValid)
        assertTrue(errorReason?.contains("floor plane") == true)
    }

    @Test
    fun validateCalibration_rejectsInvalidDimensions() {
        // Room width too small (width < 1.0m)
        val tinyCorners = listOf(
            SpatialVector3(0f, 0f, 0f),
            SpatialVector3(0.8f, 0f, 0f),
            SpatialVector3(0.8f, 0f, 4f),
            SpatialVector3(0f, 0f, 4f)
        )
        val (isValid, errorReason) = calibrationManager.validateCalibration(tinyCorners, 2.5f)
        assertFalse(isValid)
        assertTrue(errorReason?.contains("Plausible room size exceeded") == true)
    }

    @Test
    fun validateCalibration_acceptsValidGeometry() {
        val validCorners = listOf(
            SpatialVector3(0f, 0f, 0f),
            SpatialVector3(4f, 0f, 0f),
            SpatialVector3(4f, 0.05f, 4f), // Minor height deviation is okay (<0.15m)
            SpatialVector3(0f, 0f, 4f)
        )
        val (isValid, errorReason) = calibrationManager.validateCalibration(validCorners, 2.5f)
        assertTrue(errorReason, isValid)
    }

    @Test
    fun buildCalibrationResult_generatesValidOrthonormalBasis() {
        val rawCorners = listOf(
            SpatialVector3(1f, 0.1f, 1f),
            SpatialVector3(5f, 0.12f, 1f),
            SpatialVector3(5f, 0.08f, 5f),
            SpatialVector3(1f, 0.1f, 5f)
        )

        val result = calibrationManager.buildCalibrationResult(
            rawCorners = rawCorners,
            ceilingHeight = 2.8f,
            method = CalibrationMethod.MANUAL,
            confidence = 1.0f
        )

        assertNotNull(result)
        result!!

        // Verify basis vectors are normalized (length = 1.0)
        assertEquals(1.0f, result.xAxis.length(), 1e-4f)
        assertEquals(1.0f, result.yAxis.length(), 1e-4f)
        assertEquals(1.0f, result.zAxis.length(), 1e-4f)

        // Verify orthogonality (dot products = 0)
        assertEquals(0.0f, result.xAxis.dot(result.yAxis), 1e-4f)
        assertEquals(0.0f, result.xAxis.dot(result.zAxis), 1e-4f)
        assertEquals(0.0f, result.yAxis.dot(result.zAxis), 1e-4f)

        // Verify room origin equals Corner 1
        assertEquals(result.calibratedCornersWorld[0], result.roomOrigin)
    }

    @Test
    fun coordinateTransforms_arePerfectInverses() {
        val corners = listOf(
            SpatialVector3(1f, 0.1f, 1f),
            SpatialVector3(5f, 0.1f, 1f),
            SpatialVector3(5f, 0.1f, 5f),
            SpatialVector3(1f, 0.1f, 5f)
        )

        val result = calibrationManager.buildCalibrationResult(
            rawCorners = corners,
            ceilingHeight = 3.0f,
            method = CalibrationMethod.AUTOMATIC,
            confidence = 0.9f
        )!!

        val transform = CoordinateTransform(result)

        // Test points in World space
        val testPointsWorld = listOf(
            SpatialVector3(1f, 0.1f, 1f), // Corner 1 (Origin)
            SpatialVector3(3f, 1.5f, 3f), // Inside room
            SpatialVector3(10f, -2f, 5f)  // Arbitrary outside
        )

        for (ptWorld in testPointsWorld) {
            val ptRoom = transform.worldToRoom(ptWorld)
            val ptWorldBack = transform.roomToWorld(ptRoom)

            assertEquals(ptWorld.x, ptWorldBack.x, 1e-4f)
            assertEquals(ptWorld.y, ptWorldBack.y, 1e-4f)
            assertEquals(ptWorld.z, ptWorldBack.z, 1e-4f)
        }

        // Specifically check that Corner 1 (Origin) transforms to (0, 0, 0) in Room space
        val originRoom = transform.worldToRoom(result.roomOrigin)
        assertEquals(0f, originRoom.x, 1e-4f)
        assertEquals(0f, originRoom.y, 1e-4f)
        assertEquals(0f, originRoom.z, 1e-4f)
    }

    @Test
    fun wallDefinitions_haveInwardPointingNormals() {
        val corners = listOf(
            SpatialVector3(0f, 0f, 0f),
            SpatialVector3(4f, 0f, 0f),
            SpatialVector3(4f, 0f, 4f),
            SpatialVector3(0f, 0f, 4f)
        )

        val result = calibrationManager.buildCalibrationResult(
            rawCorners = corners,
            ceilingHeight = 2.5f,
            method = CalibrationMethod.MANUAL,
            confidence = 1.0f
        )!!

        val walls = result.wallDefinitions
        assertEquals(4, walls.size)

        // The center of this square room is at Room Space (2, 0, 2)
        val roomCenter = SpatialVector3(2f, 0f, 2f)

        for (wall in walls) {
            // Verify wall dimensions match
            assertEquals(4.0f, wall.length, 1e-4f)
            assertEquals(2.5f, wall.height, 1e-4f)

            // Verify normal vector dot product with vector pointing to center is strictly positive,
            // proving that the normal vector points *into* the room.
            val toCenter = roomCenter - wall.originInRoom
            val dot = toCenter.dot(wall.zAxis)
            assertTrue("Normal vector for ${wall.id} points outwards!", dot > 0f)

            // Verify rigid transformations for Wall Space -> Room Space
            // A point u=2, v=1.2, w=0 (on wall surface, middle horizontally and vertically)
            val transform = CoordinateTransform(result)
            val ptWall = SpatialVector3(2f, 1.2f, 0f)
            val ptRoom = transform.wallToRoom(ptWall, wall)
            val ptWallBack = transform.roomToWall(ptRoom, wall)

            assertEquals(ptWall.x, ptWallBack.x, 1e-4f)
            assertEquals(ptWall.y, ptWallBack.y, 1e-4f)
            assertEquals(ptWall.z, ptWallBack.z, 1e-4f)
        }
    }
}
