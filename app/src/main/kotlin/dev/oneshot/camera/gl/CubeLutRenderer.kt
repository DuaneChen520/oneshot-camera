package dev.oneshot.camera.gl

import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLES30
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * OpenGL ES 3.0 renderer supporting 3D LUT grading, Focus Peaking, and Zebra Pattern.
 */
class CubeLutRenderer {

    private var program = 0
    private var aPositionLoc = -1
    private var aTexCoordLoc = -1
    private var uTexMatrixLoc = -1
    private var uTextureLoc = -1
    private var uLut3dLoc = -1
    private var uLutEnabledLoc = -1
    private var uPeakingEnabledLoc = -1
    private var uZebraEnabledLoc = -1
    private var uTexelSizeLoc = -1
    private var uTimeLoc = -1

    private val lutTextureId = IntArray(1)
    private var currentLutData: Lut3dData? = null

    private val vertexBuffer: FloatBuffer = ByteBuffer.allocateDirect(QUAD_COORDS.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply {
            put(QUAD_COORDS)
            position(0)
        }

    private val texCoordBuffer: FloatBuffer = ByteBuffer.allocateDirect(TEX_COORDS.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply {
            put(TEX_COORDS)
            position(0)
        }

    fun init() {
        program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        aPositionLoc = GLES20.glGetAttribLocation(program, "aPosition")
        aTexCoordLoc = GLES20.glGetAttribLocation(program, "aTexCoord")
        uTexMatrixLoc = GLES20.glGetUniformLocation(program, "uTexMatrix")
        uTextureLoc = GLES20.glGetUniformLocation(program, "uTexture")
        uLut3dLoc = GLES20.glGetUniformLocation(program, "uLut3d")
        uLutEnabledLoc = GLES20.glGetUniformLocation(program, "uLutEnabled")
        uPeakingEnabledLoc = GLES20.glGetUniformLocation(program, "uPeakingEnabled")
        uZebraEnabledLoc = GLES20.glGetUniformLocation(program, "uZebraEnabled")
        uTexelSizeLoc = GLES20.glGetUniformLocation(program, "uTexelSize")
        uTimeLoc = GLES20.glGetUniformLocation(program, "uTime")

        // Allocate 3D texture handle
        GLES30.glGenTextures(1, lutTextureId, 0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_3D, lutTextureId[0])
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_3D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_3D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_3D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_3D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_3D, GLES30.GL_TEXTURE_WRAP_R, GLES30.GL_CLAMP_TO_EDGE)
    }

    fun createOesTexture(): Int {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0])
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        return textures[0]
    }

    fun loadLut(lutData: Lut3dData) {
        if (lutData === currentLutData) return
        currentLutData = lutData

        GLES30.glBindTexture(GLES30.GL_TEXTURE_3D, lutTextureId[0])
        GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1)
        lutData.buffer.rewind()
        GLES30.glTexImage3D(
            GLES30.GL_TEXTURE_3D,
            0,
            GLES30.GL_RGB,
            lutData.size,
            lutData.size,
            lutData.size,
            0,
            GLES30.GL_RGB,
            GLES30.GL_UNSIGNED_BYTE,
            lutData.buffer
        )
    }

    fun drawFrame(
        oesTextureId: Int,
        texMatrix: FloatArray,
        viewportWidth: Int,
        viewportHeight: Int,
        lutEnabled: Boolean,
        peakingEnabled: Boolean,
        zebraEnabled: Boolean,
        timeSeconds: Float,
    ) {
        GLES20.glViewport(0, 0, viewportWidth, viewportHeight)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        GLES20.glUseProgram(program)

        // Bind OES Camera Texture to unit 0
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTextureId)
        GLES20.glUniform1i(uTextureLoc, 0)

        // Bind 3D LUT Texture to unit 1
        GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_3D, lutTextureId[0])
        GLES20.glUniform1i(uLut3dLoc, 1)

        // Set uniforms
        GLES20.glUniformMatrix4fv(uTexMatrixLoc, 1, false, texMatrix, 0)
        GLES20.glUniform1i(uLutEnabledLoc, if (lutEnabled && currentLutData != null) 1 else 0)
        GLES20.glUniform1i(uPeakingEnabledLoc, if (peakingEnabled) 1 else 0)
        GLES20.glUniform1i(uZebraEnabledLoc, if (zebraEnabled) 1 else 0)
        GLES20.glUniform2f(uTexelSizeLoc, 1f / viewportWidth.coerceAtLeast(1), 1f / viewportHeight.coerceAtLeast(1))
        GLES20.glUniform1f(uTimeLoc, timeSeconds)

        // Attributes
        GLES20.glEnableVertexAttribArray(aPositionLoc)
        GLES20.glVertexAttribPointer(aPositionLoc, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        GLES20.glEnableVertexAttribArray(aTexCoordLoc)
        GLES20.glVertexAttribPointer(aTexCoordLoc, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer)

        // Draw quad
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(aPositionLoc)
        GLES20.glDisableVertexAttribArray(aTexCoordLoc)
    }

    fun release() {
        if (lutTextureId[0] != 0) {
            GLES30.glDeleteTextures(1, lutTextureId, 0)
            lutTextureId[0] = 0
        }
        if (program != 0) {
            GLES20.glDeleteProgram(program)
            program = 0
        }
    }

    private companion object {
        val QUAD_COORDS = floatArrayOf(
            -1.0f, -1.0f,
             1.0f, -1.0f,
            -1.0f,  1.0f,
             1.0f,  1.0f,
        )

        val TEX_COORDS = floatArrayOf(
            0.0f, 0.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f,
        )

        const val VERTEX_SHADER = """#version 300 es
in vec4 aPosition;
in vec2 aTexCoord;
uniform mat4 uTexMatrix;
out vec2 vTexCoord;

void main() {
    gl_Position = aPosition;
    vTexCoord = (uTexMatrix * vec4(aTexCoord, 0.0, 1.0)).xy;
}
"""

        const val FRAGMENT_SHADER = """#version 300 es
#extension GL_OES_EGL_image_external_essl3 : require
precision mediump float;
precision mediump sampler3D;

uniform samplerExternalOES uTexture;
uniform sampler3D uLut3d;
uniform int uLutEnabled;
uniform int uPeakingEnabled;
uniform int uZebraEnabled;
uniform vec2 uTexelSize;
uniform float uTime;

in vec2 vTexCoord;
out vec4 fragColor;

void main() {
    vec4 src = texture(uTexture, vTexCoord);
    vec3 col = src.rgb;

    // 1. 3D LUT Color Grading
    if (uLutEnabled == 1) {
        vec3 lutCoord = clamp(col, 0.0, 1.0);
        col = texture(uLut3d, lutCoord).rgb;
    }

    float luma = dot(col, vec3(0.299, 0.587, 0.114));

    // 2. Zebra Pattern (Overexposure indicator > 95% luma)
    if (uZebraEnabled == 1 && luma > 0.95) {
        float stripe = mod(gl_FragCoord.x + gl_FragCoord.y + uTime * 20.0, 16.0);
        if (stripe < 5.0) {
            col = vec3(1.0, 0.1, 0.1);
        }
    }

    // 3. Focus Peaking (High-frequency Sobel edge detection)
    if (uPeakingEnabled == 1) {
        float leftLuma  = dot(texture(uTexture, vTexCoord - vec2(uTexelSize.x, 0.0)).rgb, vec3(0.299, 0.587, 0.114));
        float rightLuma = dot(texture(uTexture, vTexCoord + vec2(uTexelSize.x, 0.0)).rgb, vec3(0.299, 0.587, 0.114));
        float topLuma   = dot(texture(uTexture, vTexCoord - vec2(0.0, uTexelSize.y)).rgb, vec3(0.299, 0.587, 0.114));
        float botLuma   = dot(texture(uTexture, vTexCoord + vec2(0.0, uTexelSize.y)).rgb, vec3(0.299, 0.587, 0.114));

        float dx = rightLuma - leftLuma;
        float dy = botLuma - topLuma;
        float edge = sqrt(dx * dx + dy * dy);

        if (edge > 0.18) {
            col = mix(col, vec3(0.0, 1.0, 0.3), 0.85);
        }
    }

    fragColor = vec4(col, src.a);
}
"""

        fun loadShader(type: Int, shaderCode: String): Int {
            val shader = GLES20.glCreateShader(type)
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
            val compiled = IntArray(1)
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
            if (compiled[0] == 0) {
                val error = GLES20.glGetShaderInfoLog(shader)
                GLES20.glDeleteShader(shader)
                throw RuntimeException("Could not compile shader $type: $error")
            }
            return shader
        }

        fun createProgram(vertexSource: String, fragmentSource: String): Int {
            val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource)
            val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
            val program = GLES20.glCreateProgram()
            GLES20.glAttachShader(program, vertexShader)
            GLES20.glAttachShader(program, fragmentShader)
            GLES20.glLinkProgram(program)
            val linkStatus = IntArray(1)
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus[0] == 0) {
                val error = GLES20.glGetProgramInfoLog(program)
                GLES20.glDeleteProgram(program)
                throw RuntimeException("Could not link program: $error")
            }
            return program
        }
    }
}
