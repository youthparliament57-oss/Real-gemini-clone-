package com.example.ar.rendering

import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log
import com.example.ar.session.ArSessionManager
import com.google.ar.core.Coordinates2d
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Official OpenGL ES 2.0 / 3.0 renderer foundation for displaying the ARCore camera background feed.
 * Uses an external OES texture quad and coordinates transformation provided by ARCore Frame.
 */
class ArCameraRenderer(
    private val sessionManager: ArSessionManager
) : GLSurfaceView.Renderer {

    companion object {
        private const val TAG = "ArCameraRenderer"

        private const val VERTEX_SHADER = """
            attribute vec4 a_Position;
            attribute vec2 a_TexCoord;
            varying vec2 v_TexCoord;
            void main() {
               gl_Position = a_Position;
               v_TexCoord = a_TexCoord;
            }
        """

        private const val FRAGMENT_SHADER = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;
            varying vec2 v_TexCoord;
            uniform samplerExternalOES u_Texture;
            void main() {
                gl_FragColor = texture2D(u_Texture, v_TexCoord);
            }
        """

        // Fullscreen quad in Normalized Device Coordinates (NDC)
        private val QUAD_COORDS = floatArrayOf(
            -1.0f, -1.0f,
            +1.0f, -1.0f,
            -1.0f, +1.0f,
            +1.0f, +1.0f
        )

        // View-normalized quad coordinates for ARCore transformCoordinates2d
        private val QUAD_VIEW_COORDS = floatArrayOf(
            0.0f, 1.0f,
            1.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f
        )
    }

    private var programId = 0
    private var positionHandle = 0
    private var texCoordHandle = 0
    private var textureHandle = 0
    private var textureId = 0

    private val quadVertices: FloatBuffer = ByteBuffer.allocateDirect(QUAD_COORDS.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply {
            put(QUAD_COORDS)
            position(0)
        }

    private val quadViewCoords: FloatBuffer = ByteBuffer.allocateDirect(QUAD_VIEW_COORDS.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply {
            put(QUAD_VIEW_COORDS)
            position(0)
        }

    private val quadTexCoords: FloatBuffer = ByteBuffer.allocateDirect(QUAD_VIEW_COORDS.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()

    var displayRotation: Int = 0
    private var lastLogTime = 0L

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        Log.d(TAG, "onSurfaceCreated: GLSurfaceView surface created successfully.")
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)

        // Generate OpenGL external texture
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textureId = textures[0]
        Log.d(TAG, "onSurfaceCreated: Generated external OES texture with ID: $textureId")

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        // Bind texture ID to ARCore session
        Log.d(TAG, "onSurfaceCreated: Binding texture ID $textureId to ARCore session.")
        sessionManager.setCameraTextureNames(intArrayOf(textureId))

        // Compile and link shaders
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)

        programId = GLES20.glCreateProgram().also { program ->
            GLES20.glAttachShader(program, vertexShader)
            GLES20.glAttachShader(program, fragmentShader)
            GLES20.glLinkProgram(program)
            GLES20.glUseProgram(program)
        }

        positionHandle = GLES20.glGetAttribLocation(programId, "a_Position")
        texCoordHandle = GLES20.glGetAttribLocation(programId, "a_TexCoord")
        textureHandle = GLES20.glGetUniformLocation(programId, "u_Texture")
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        Log.d(TAG, "onSurfaceChanged: Viewport width = $width, height = $height, displayRotation = $displayRotation")
        GLES20.glViewport(0, 0, width, height)
        sessionManager.setDisplayGeometry(displayRotation, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        val frame = sessionManager.updateFrame()
        val currentTime = System.currentTimeMillis()
        val shouldLog = currentTime - lastLogTime > 3000
        if (shouldLog) {
            lastLogTime = currentTime
            Log.d(TAG, "onDrawFrame: Frame update success = ${frame != null}, SessionState = ${sessionManager.state.value}")
            val glErr = GLES20.glGetError()
            if (glErr != GLES20.GL_NO_ERROR) {
                Log.e(TAG, "onDrawFrame: OpenGL error detected = $glErr")
            }
        }

        if (frame == null) return

        // Compute UV transform matching the device display orientation and aspect ratio
        frame.transformCoordinates2d(
            Coordinates2d.VIEW_NORMALIZED,
            quadViewCoords,
            Coordinates2d.TEXTURE_NORMALIZED,
            quadTexCoords
        )

        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthMask(false)

        GLES20.glUseProgram(programId)

        // Bind OES texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES20.glUniform1i(textureHandle, 0)

        // Pass vertices
        quadVertices.position(0)
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, quadVertices)
        GLES20.glEnableVertexAttribArray(positionHandle)

        // Pass texture coordinates
        quadTexCoords.position(0)
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, quadTexCoords)
        GLES20.glEnableVertexAttribArray(texCoordHandle)

        // Draw quad
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)

        GLES20.glDepthMask(true)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        return GLES20.glCreateShader(type).also { shader ->
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
            val compiled = IntArray(1)
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
            if (compiled[0] == 0) {
                val info = GLES20.glGetShaderInfoLog(shader)
                Log.e(TAG, "Shader compile error: $info")
                GLES20.glDeleteShader(shader)
            }
        }
    }
}
