package com.example.ar

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ar.capability.ArAvailability
import com.example.ar.capability.ArCapabilityChecker
import com.example.ar.session.ArRuntimeState
import com.example.ar.session.ArSessionManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ArFoundationTest {

    private lateinit var context: Context
    private lateinit var capabilityChecker: ArCapabilityChecker
    private lateinit var sessionManager: ArSessionManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        capabilityChecker = ArCapabilityChecker(context)
        sessionManager = ArSessionManager(capabilityChecker)
    }

    @Test
    fun arRuntimeState_instantiatesExpectedValues() {
        val trackingState = ArRuntimeState.Tracking(isDepthSupported = true)
        assertTrue(trackingState.isDepthSupported)

        val trackingLost = ArRuntimeState.TrackingLost("Excessive motion")
        assertEquals("Excessive motion", trackingLost.reason)

        val unavailable = ArRuntimeState.Unavailable("Device not capable")
        assertEquals("Device not capable", unavailable.reason)

        val error = ArRuntimeState.Error("Camera busy")
        assertEquals("Camera busy", error.message)
    }

    @Test
    fun arCapabilityChecker_permissionCheck_returnsFalseWhenNotGranted() {
        // By default in Robolectric test without shadow grant, camera permission is not granted
        val hasPermission = capabilityChecker.hasCameraPermission()
        assertFalse(hasPermission)
    }

    @Test
    fun arSessionManager_initialState_isReady() {
        assertEquals(ArRuntimeState.Ready, sessionManager.state.value)
        assertNull(sessionManager.getSession())
    }

    @Test
    fun arSessionManager_createWithoutCameraPermission_transitionsToPermissionRequired() {
        val session = sessionManager.create(context)
        assertNull(session)
        assertEquals(ArRuntimeState.PermissionRequired, sessionManager.state.value)
    }

    @Test
    fun arSessionManager_pauseAndClose_clearsStateCleanly() {
        sessionManager.pause()
        assertEquals(ArRuntimeState.Paused, sessionManager.state.value)

        sessionManager.close()
        assertEquals(ArRuntimeState.Ready, sessionManager.state.value)
        assertNull(sessionManager.getSession())
    }

    @Test
    fun arAvailability_enum_containsAllRequiredCases() {
        val cases = ArAvailability.values().map { it.name }
        assertTrue(cases.contains("CHECKING"))
        assertTrue(cases.contains("SUPPORTED_INSTALLED"))
        assertTrue(cases.contains("SUPPORTED_NOT_INSTALLED"))
        assertTrue(cases.contains("SUPPORTED_APK_TOO_OLD"))
        assertTrue(cases.contains("UNSUPPORTED_DEVICE_NOT_CAPABLE"))
        assertTrue(cases.contains("CHECK_ERROR"))
    }
}
