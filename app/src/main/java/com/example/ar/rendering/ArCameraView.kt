package com.example.ar.rendering

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.Display
import android.view.WindowManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.ar.session.ArSessionManager

/**
 * Jetpack Compose host for the ARCore OpenGL ES camera background viewport.
 * Manages the GLSurfaceView lifecycle safely in tandem with Compose lifecycle events.
 */
@Composable
fun ArCameraView(
    sessionManager: ArSessionManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val windowManager = remember { context.getSystemService(Context.WINDOW_SERVICE) as WindowManager }
    val renderer = remember { ArCameraRenderer(sessionManager) }

    val glSurfaceView = remember {
        GLSurfaceView(context).apply {
            preserveEGLContextOnPause = true
            setEGLContextClientVersion(2)
            setRenderer(renderer)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    @Suppress("DEPRECATION")
                    val display: Display? = windowManager.defaultDisplay
                    renderer.displayRotation = display?.rotation ?: 0
                    glSurfaceView.onResume()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    glSurfaceView.onPause()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            glSurfaceView.onPause()
        }
    }

    AndroidView(
        factory = { glSurfaceView },
        modifier = modifier.fillMaxSize()
    )
}
