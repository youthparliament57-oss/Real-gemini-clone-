package com.example.ar

import com.example.ar.analysis.model.SpatialPoint2D
import com.example.ar.analysis.model.SpatialVector3
import com.example.ar.calibration.CalibrationManager
import com.example.ar.calibration.CalibrationMethod
import com.example.ar.calibration.CoordinateTransform
import com.example.ar.calibration.model.CalibrationConfidence
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
        assertTrue(errorReason?.contains("deviates") == true || errorReason?.contains("floor plane") == true)
    }

    @Test
    fun validateCalibration_rejectsSelfIntersectingHourglassPolygon() {
        // Crossed corners: C0(0,0) -> C1(4,4) -> C2(4,0) -> C3(0,4) forming a bowtie
        val hourglassCorners = listOf(
            SpatialVector3(0f, 0f, 0f),
            SpatialVector3(4f, 0f, 4f),
            SpatialVector3(4f, 0f, 0f),
            SpatialVector3(0f, 0f, 4f)
        )
        val (isValid, errorReason) = calibrationManager.validateCalibration(hourglassCorners, 2.5f)
        assertFalse(isValid)
        assertTrue(errorReason?.contains("self-intersecting") == true)
    }

    @Test
    fun validateCalibration_rejectsDegenerateCollinearCorners() {
        // 3 collinear corners in a straight line: (0,0), (2,0), (4,0), (2,4)
        val collinearCorners = listOf(
            SpatialVector3(0f, 0f, 0f),
            SpatialVector3(2f, 0f, 0f),
            SpatialVector3(4f, 0f, 0f),
            SpatialVector3(2f, 0f, 4f)
        )
        val (isValid, errorReason) = calibrationManager.validateCalibration(collinearCorners, 2.5f)
        assertFalse(isValid)
        assertTrue(errorReason?.contains("collinear") == true || errorReason?.contains("degenerate") == true)
    }

    @Test
    fun validateCalibration_rejectsInvalidDimensionsAndCeilingHeight() {
        // Room width too small (area < 1.0m^2)
        val tinyCorners = listOf(
            SpatialVector3(0f, 0f, 0f),
            SpatialVector3(0.5f, 0f, 0f),
            SpatialVector3(0.5f, 0f, 0.5f),
            SpatialVector3(0f, 0f, 0.5f)
        )
        val (isValidArea, errorArea) = calibrationManager.validateCalibration(tinyCorners, 2.5f)
        assertFalse(isValidArea)
        assertTrue(errorArea?.contains("small") == true)

        // Valid corners with invalid ceiling height (0.5m < 1.5m)
        val validCorners = listOf(
            SpatialVector3(0f, 0f, 0f),
            SpatialVector3(4f, 0f, 0f),
            SpatialVector3(4f, 0f, 4f),
            SpatialVector3(0f, 0f, 4f)
        )
        val (isValidCeilingLow, errorCeilingLow) = calibrationManager.validateCalibration(validCorners, 0.8f)
        assertFalse(isValidCeilingLow)
        assertTrue(errorCeilingLow?.contains("Ceiling height") == true)

        val (isValidCeilingHigh, errorCeilingHigh) = calibrationManager.validateCalibration(validCorners, 15.0f)
        assertFalse(isValidCeilingHigh)
        assertTrue(errorCeilingHigh?.contains("Ceiling height") == true)
    }

    @Test
    fun validateCalibration_acceptsValidIrregularQuadrilateral() {
        // Non-rectangular trapezoid room
        val trapezoidCorners = listOf(
            SpatialVector3(0f, 0f, 0f),
            SpatialVector3(5f, 0.02f, 0f),
            SpatialVector3(3.5f, -0.01f, 4f),
            SpatialVector3(0.5f, 0f, 3.5f)
        )
        val (isValid, errorReason) = calibrationManager.validateCalibration(trapezoidCorners, 2.8f)
        assertTrue(errorReason, isValid)
    }

    @Test
    fun buildCalibrationResult_generatesStrictlyOrthonormalBasisAndOrigin() {
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

        // Verify right-handedness: X x Y = Z
        val crossZ = result.xAxis.cross(result.yAxis)
        assertEquals(crossZ.x, result.zAxis.x, 1e-4f)
        assertEquals(crossZ.y, result.zAxis.y, 1e-4f)
        assertEquals(crossZ.z, result.zAxis.z, 1e-4f)

        // Verify room origin equals Corner 1
        assertEquals(result.calibratedCornersWorld[0], result.roomOrigin)

        // Verify RoomModel export
        val roomModel = result.toRoomModel()
        assertEquals(result.roomOrigin, roomModel.origin)
        assertEquals(4, roomModel.walls.size)
        assertEquals(2.8f, roomModel.ceilingHeightMeters, 1e-4f)
    }

    @Test
    fun coordinateTransforms_performExactRoundTrips() {
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

        val transform = result.coordinateTransform
        assertTrue(transform.isOrthonormal())

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

        // Test directional vector transforms (translation-independent)
        val dirWorld = SpatialVector3(0f, 1f, 0f)
        val dirRoom = transform.worldToRoomDir(dirWorld)
        val dirWorldBack = transform.roomToWorldDir(dirRoom)
        assertEquals(dirWorld.x, dirWorldBack.x, 1e-4f)
        assertEquals(dirWorld.y, dirWorldBack.y, 1e-4f)
        assertEquals(dirWorld.z, dirWorldBack.z, 1e-4f)

        // Specifically check that Corner 1 (Origin) transforms to (0, 0, 0) in Room space
        val originRoom = transform.worldToRoom(result.roomOrigin)
        assertEquals(0f, originRoom.x, 1e-4f)
        assertEquals(0f, originRoom.y, 1e-4f)
        assertEquals(0f, originRoom.z, 1e-4f)
    }

    @Test
    fun wallModels_satisfyLocalCoordinateFramesAndInwardNormals() {
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

        val walls = result.walls
        assertEquals(4, walls.size)

        val roomCenter = SpatialVector3(2f, 0f, 2f)

        for (wall in walls) {
            // Verify wall dimensions match
            assertEquals(4.0f, wall.lengthMeters, 1e-4f)
            assertEquals(2.5f, wall.heightMeters, 1e-4f)

            // Verify orthonormal basis for wall (tangent, up, normal)
            assertEquals(1.0f, wall.tangent.length(), 1e-4f)
            assertEquals(1.0f, wall.upDirection.length(), 1e-4f)
            assertEquals(1.0f, wall.normal.length(), 1e-4f)
            assertEquals(0.0f, wall.tangent.dot(wall.upDirection), 1e-4f)
            assertEquals(0.0f, wall.tangent.dot(wall.normal), 1e-4f)
            assertEquals(0.0f, wall.upDirection.dot(wall.normal), 1e-4f)

            // Verify normal vector points into the interior of the room
            val toCenter = roomCenter - wall.wallOrigin
            val dot = toCenter.dot(wall.normal)
            assertTrue("Normal vector for ${wall.wallId} points outwards!", dot > 0f)

            // Verify local coordinate round-trip
            // Local point: u=2.0 (mid-length), v=1.2 (mid-height), n=0.0 (surface)
            val ptWorld = wall.wallLocalToWorld(2.0f, 1.2f, 0.0f)
            val ptLocalBack = wall.worldToWallLocal(ptWorld)

            assertEquals(2.0f, ptLocalBack.x, 1e-4f)
            assertEquals(1.2f, ptLocalBack.y, 1e-4f)
            assertEquals(0.0f, ptLocalBack.z, 1e-4f)
        }
    }

    @Test
    fun calculateConfidence_accuratelyCategorizesCalibrationQuality() {
        // Pristine rectangle with coplanar corners
        val pristineCorners = listOf(
            SpatialVector3(0f, 0f, 0f),
            SpatialVector3(4f, 0f, 0f),
            SpatialVector3(4f, 0f, 4f),
            SpatialVector3(0f, 0f, 4f)
        )
        val (scorePristine, ratingPristine) = calibrationManager.calculateConfidence(
            pristineCorners,
            CalibrationMethod.MANUAL
        )
        assertTrue(scorePristine >= 0.80f)
        assertEquals(CalibrationConfidence.HIGH, ratingPristine)

        // Skewed / irregular quadrilateral with slight height deviation
        val skewedCorners = listOf(
            SpatialVector3(0f, 0f, 0f),
            SpatialVector3(4f, 0.10f, 0f),
            SpatialVector3(2.5f, 0.05f, 3.5f),
            SpatialVector3(0.5f, 0f, 2.5f)
        )
        val (_, ratingSkewed) = calibrationManager.calculateConfidence(
            skewedCorners,
            CalibrationMethod.MANUAL
        )
        assertTrue(ratingSkewed == CalibrationConfidence.MEDIUM || ratingSkewed == CalibrationConfidence.HIGH)

        // Non-coplanar corners
        val invalidCorners = listOf(
            SpatialVector3(0f, 0f, 0f),
            SpatialVector3(4f, 0.5f, 0f), // 0.5m deviation
            SpatialVector3(4f, 0f, 4f),
            SpatialVector3(0f, 0f, 4f)
        )
        val (scoreInvalid, ratingInvalid) = calibrationManager.calculateConfidence(
            invalidCorners,
            CalibrationMethod.MANUAL
        )
        assertEquals(0f, scoreInvalid, 1e-4f)
        assertEquals(CalibrationConfidence.INVALID, ratingInvalid)
    }

    @Test
    fun segmentIntersection_accuratelyDetectsIntersections() {
        val p1 = SpatialPoint2D(0f, 0f)
        val p2 = SpatialPoint2D(4f, 4f)
        val p3 = SpatialPoint2D(0f, 4f)
        val p4 = SpatialPoint2D(4f, 0f)

        // X-crossing segments
        assertTrue(calibrationManager.doSegmentsIntersect(p1, p2, p3, p4))

        // Parallel non-intersecting segments (y = x and y = x + 2)
        val p5 = SpatialPoint2D(0f, 2f)
        val p6 = SpatialPoint2D(4f, 6f)
        assertFalse(calibrationManager.doSegmentsIntersect(p1, p2, p5, p6))
    }
}
