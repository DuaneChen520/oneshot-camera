package dev.oneshot.camera.gl

import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLExt
import android.opengl.EGLSurface
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.view.Surface
import androidx.camera.core.CameraEffect
import androidx.camera.core.SurfaceOutput
import androidx.camera.core.SurfaceProcessor
import androidx.camera.core.SurfaceRequest
import androidx.core.util.Consumer
import dev.oneshot.data.CameraSettings
import dev.oneshot.data.LutPreset
import java.util.concurrent.Executor

class OneshotCameraEffect(
    targets: Int,
    executor: Executor,
    val processor: SurfaceProcessor,
) : CameraEffect(targets, executor, processor, Consumer { })

class OneshotGlProcessor(
    private val customLutManager: CustomLutManager? = null,
) : SurfaceProcessor {

    private val glThread = HandlerThread("OneshotGlThread").apply { start() }
    private val glHandler = Handler(glThread.looper)

    private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
    private var eglConfig: EGLConfig? = null
    private var eglPbufferSurface: EGLSurface = EGL14.EGL_NO_SURFACE

    private var renderer: CubeLutRenderer? = null
    private var inputOesTextureId: Int = 0
    private var inputSurfaceTexture: SurfaceTexture? = null
    private var inputSurface: Surface? = null

    private val outputSurfaces = mutableListOf<OutputTarget>()

    private val texMatrix = FloatArray(16)
    private val combinedMatrix = FloatArray(16)
    private val startTimeMs = SystemClock.uptimeMillis()

    // Active visual effect state
    @Volatile private var isLutEnabled: Boolean = false
    @Volatile private var currentPreset: LutPreset = LutPreset.TEAL_AND_ORANGE
    @Volatile private var currentCustomLutId: String? = null
    @Volatile private var isPeakingEnabled: Boolean = false
    @Volatile private var isZebraEnabled: Boolean = false

    private class OutputTarget(
        val surfaceOutput: SurfaceOutput,
        val surface: Surface,
        var eglSurface: EGLSurface,
        val width: Int,
        val height: Int,
    )

    init {
        glHandler.post {
            initEgl()
        }
    }

    fun updateSettings(settings: CameraSettings) {
        val oldLutEnabled = isLutEnabled
        val oldPreset = currentPreset
        val oldCustomId = currentCustomLutId

        isLutEnabled = settings.lutEnabled
        currentPreset = settings.lutPreset
        currentCustomLutId = settings.selectedCustomLutId
        isPeakingEnabled = settings.focusPeakingEnabled
        isZebraEnabled = settings.zebraPatternEnabled

        val lutChanged = oldLutEnabled != isLutEnabled ||
                         oldPreset != currentPreset ||
                         oldCustomId != currentCustomLutId

        if (lutChanged && isLutEnabled) {
            glHandler.post {
                val rend = renderer ?: return@post
                val lutData: Lut3dData = if (settings.selectedCustomLutId != null && customLutManager != null) {
                    val custom = settings.customLuts.find { it.id == settings.selectedCustomLutId }
                    if (custom != null) {
                        customLutManager.getLutData(custom) ?: getBuiltInLut(currentPreset)
                    } else {
                        getBuiltInLut(currentPreset)
                    }
                } else {
                    getBuiltInLut(currentPreset)
                }
                rend.loadLut(lutData)
            }
        }
    }

    private fun getBuiltInLut(preset: LutPreset): Lut3dData = when (preset) {
        LutPreset.TEAL_AND_ORANGE -> CubeLutParser.createTealAndOrange()
        LutPreset.KODAK_PORTRA -> CubeLutParser.createKodakPortra()
        LutPreset.FLAT_LOG -> CubeLutParser.createFlatLog()
    }

    private fun initEgl() {
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        val version = IntArray(2)
        EGL14.eglInitialize(eglDisplay, version, 0, version, 1)

        val configAttribs = intArrayOf(
            EGL14.EGL_RENDERABLE_TYPE, EGLExt.EGL_OPENGL_ES3_BIT_KHR,
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_DEPTH_SIZE, 0,
            EGL14.EGL_STENCIL_SIZE, 0,
            EGL14.EGL_NONE
        )

        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        EGL14.eglChooseConfig(eglDisplay, configAttribs, 0, configs, 0, 1, numConfigs, 0)
        eglConfig = configs[0]

        val contextAttribs = intArrayOf(
            EGL14.EGL_CONTEXT_CLIENT_VERSION, 3,
            EGL14.EGL_NONE
        )
        eglContext = EGL14.eglCreateContext(eglDisplay, eglConfig, EGL14.EGL_NO_CONTEXT, contextAttribs, 0)

        val pbufferAttribs = intArrayOf(
            EGL14.EGL_WIDTH, 1,
            EGL14.EGL_HEIGHT, 1,
            EGL14.EGL_NONE
        )
        eglPbufferSurface = EGL14.eglCreatePbufferSurface(eglDisplay, eglConfig, pbufferAttribs, 0)
        EGL14.eglMakeCurrent(eglDisplay, eglPbufferSurface, eglPbufferSurface, eglContext)

        val rend = CubeLutRenderer()
        rend.init()
        inputOesTextureId = rend.createOesTexture()
        rend.loadLut(CubeLutParser.createTealAndOrange())
        renderer = rend
    }

    override fun onInputSurface(surfaceRequest: SurfaceRequest) {
        glHandler.post {
            val rend = renderer ?: return@post
            inputSurfaceTexture?.release()
            inputSurface?.release()

            val st = SurfaceTexture(inputOesTextureId).apply {
                setDefaultBufferSize(surfaceRequest.resolution.width, surfaceRequest.resolution.height)
                setOnFrameAvailableListener({
                    glHandler.post { renderFrame() }
                }, glHandler)
            }
            inputSurfaceTexture = st

            val s = Surface(st)
            inputSurface = s

            surfaceRequest.provideSurface(s, { runnable -> glHandler.post(runnable) }) {
                glHandler.post {
                    st.release()
                    s.release()
                    if (inputSurfaceTexture === st) inputSurfaceTexture = null
                    if (inputSurface === s) inputSurface = null
                }
            }
        }
    }

    override fun onOutputSurface(surfaceOutput: SurfaceOutput) {
        val surface = surfaceOutput.getSurface({ runnable -> glHandler.post(runnable) }) { event ->
            // CameraX requests surface output close (e.g. lifecycle unbind or configuration change)
            glHandler.post {
                val iterator = outputSurfaces.iterator()
                while (iterator.hasNext()) {
                    val target = iterator.next()
                    if (target.surfaceOutput === surfaceOutput) {
                        if (target.eglSurface != EGL14.EGL_NO_SURFACE) {
                            EGL14.eglDestroySurface(eglDisplay, target.eglSurface)
                        }
                        iterator.remove()
                        break
                    }
                }
            }
        }
        glHandler.post {
            val surfaceAttribs = intArrayOf(EGL14.EGL_NONE)
            val eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, eglConfig, surface, surfaceAttribs, 0)
            val target = OutputTarget(
                surfaceOutput = surfaceOutput,
                surface = surface,
                eglSurface = eglSurface,
                width = surfaceOutput.size.width,
                height = surfaceOutput.size.height
            )
            outputSurfaces.add(target)
        }
    }

    private fun renderFrame() {
        val st = inputSurfaceTexture ?: return
        val rend = renderer ?: return

        st.updateTexImage()
        st.getTransformMatrix(texMatrix)
        val timestampNs = st.timestamp
        val timeSeconds = (SystemClock.uptimeMillis() - startTimeMs) / 1000f

        val targetsToRemove = mutableListOf<OutputTarget>()

        for (target in outputSurfaces) {
            if (target.eglSurface == EGL14.EGL_NO_SURFACE) continue

            EGL14.eglMakeCurrent(eglDisplay, target.eglSurface, target.eglSurface, eglContext)

            target.surfaceOutput.updateTransformMatrix(combinedMatrix, texMatrix)

            rend.drawFrame(
                oesTextureId = inputOesTextureId,
                texMatrix = combinedMatrix,
                viewportWidth = target.width,
                viewportHeight = target.height,
                lutEnabled = isLutEnabled,
                peakingEnabled = isPeakingEnabled,
                zebraEnabled = isZebraEnabled,
                timeSeconds = timeSeconds
            )

            EGLExt.eglPresentationTimeANDROID(eglDisplay, target.eglSurface, timestampNs)
            if (!EGL14.eglSwapBuffers(eglDisplay, target.eglSurface)) {
                targetsToRemove.add(target)
            }
        }

        if (targetsToRemove.isNotEmpty()) {
            for (target in targetsToRemove) {
                EGL14.eglDestroySurface(eglDisplay, target.eglSurface)
                target.surfaceOutput.close()
                outputSurfaces.remove(target)
            }
        }
    }

    fun release() {
        glHandler.post {
            for (target in outputSurfaces) {
                EGL14.eglDestroySurface(eglDisplay, target.eglSurface)
                target.surfaceOutput.close()
            }
            outputSurfaces.clear()

            inputSurfaceTexture?.release()
            inputSurface?.release()
            inputSurfaceTexture = null
            inputSurface = null

            if (inputOesTextureId != 0) {
                val textures = intArrayOf(inputOesTextureId)
                android.opengl.GLES20.glDeleteTextures(1, textures, 0)
                inputOesTextureId = 0
            }

            renderer?.release()
            renderer = null

            if (eglPbufferSurface != EGL14.EGL_NO_SURFACE) {
                EGL14.eglDestroySurface(eglDisplay, eglPbufferSurface)
                eglPbufferSurface = EGL14.EGL_NO_SURFACE
            }
            if (eglContext != EGL14.EGL_NO_CONTEXT) {
                EGL14.eglDestroyContext(eglDisplay, eglContext)
                eglContext = EGL14.EGL_NO_CONTEXT
            }
            if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
                EGL14.eglTerminate(eglDisplay)
                eglDisplay = EGL14.EGL_NO_DISPLAY
            }

            glThread.quitSafely()
        }
    }
}
