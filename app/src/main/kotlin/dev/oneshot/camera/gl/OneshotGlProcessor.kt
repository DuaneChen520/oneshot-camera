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
import android.opengl.GLES20
import androidx.core.util.Consumer
import dev.oneshot.data.CameraSettings
import dev.oneshot.data.HistogramMode
import dev.oneshot.data.LutPreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import java.nio.ByteOrder
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
    @Volatile private var histogramMode: HistogramMode = HistogramMode.OFF

    // Real-time histogram state & sampling buffers
    private val _histogramData = MutableStateFlow<HistogramData?>(null)
    val histogramData: StateFlow<HistogramData?> = _histogramData.asStateFlow()

    private val histFbo = IntArray(1)
    private val histTexture = IntArray(1)
    private val histBuffer = ByteBuffer.allocateDirect(HIST_SAMPLE_WIDTH * HIST_SAMPLE_HEIGHT * 4).order(ByteOrder.nativeOrder())
    private var lastHistSampleTimeMs = 0L

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
        val oldHistMode = histogramMode
        histogramMode = settings.histogramMode
        if (oldHistMode != HistogramMode.OFF && histogramMode == HistogramMode.OFF) {
            _histogramData.value = null
        }

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

        // Initialize 64x36 offscreen FBO for downsampled real-time histogram sampling
        GLES20.glGenFramebuffers(1, histFbo, 0)
        GLES20.glGenTextures(1, histTexture, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, histTexture[0])
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
            HIST_SAMPLE_WIDTH, HIST_SAMPLE_HEIGHT, 0,
            GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null
        )
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, histFbo[0])
        GLES20.glFramebufferTexture2D(
            GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
            GLES20.GL_TEXTURE_2D, histTexture[0], 0
        )
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
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
            if (!surface.isValid) {
                return@post
            }
            val surfaceAttribs = intArrayOf(EGL14.EGL_NONE)
            val eglSurface = runCatching {
                EGL14.eglCreateWindowSurface(eglDisplay, eglConfig, surface, surfaceAttribs, 0)
            }.getOrDefault(EGL14.EGL_NO_SURFACE)

            if (eglSurface != EGL14.EGL_NO_SURFACE) {
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
            if (target.eglSurface == EGL14.EGL_NO_SURFACE || !target.surface.isValid) continue

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

        // Real-time histogram sampling (~13 fps, ultra-low GPU overhead)
        if (histogramMode != HistogramMode.OFF && histFbo[0] != 0 && outputSurfaces.isNotEmpty()) {
            val now = SystemClock.uptimeMillis()
            if (now - lastHistSampleTimeMs >= 75L) {
                lastHistSampleTimeMs = now
                val activeTarget = outputSurfaces.firstOrNull { it.eglSurface != EGL14.EGL_NO_SURFACE }
                if (activeTarget != null) {
                    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, histFbo[0])
                    activeTarget.surfaceOutput.updateTransformMatrix(combinedMatrix, texMatrix)
                    rend.drawFrame(
                        oesTextureId = inputOesTextureId,
                        texMatrix = combinedMatrix,
                        viewportWidth = HIST_SAMPLE_WIDTH,
                        viewportHeight = HIST_SAMPLE_HEIGHT,
                        lutEnabled = isLutEnabled,
                        peakingEnabled = false,
                        zebraEnabled = false,
                        timeSeconds = timeSeconds
                    )
                    histBuffer.rewind()
                    GLES20.glReadPixels(
                        0, 0, HIST_SAMPLE_WIDTH, HIST_SAMPLE_HEIGHT,
                        GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, histBuffer
                    )
                    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)

                    histBuffer.rewind()
                    _histogramData.value = HistogramData.computeFromRgba(
                        HIST_SAMPLE_WIDTH, HIST_SAMPLE_HEIGHT, histBuffer
                    )
                }
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

            if (histFbo[0] != 0) {
                GLES20.glDeleteFramebuffers(1, histFbo, 0)
                histFbo[0] = 0
            }
            if (histTexture[0] != 0) {
                GLES20.glDeleteTextures(1, histTexture, 0)
                histTexture[0] = 0
            }
            _histogramData.value = null

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

    companion object {
        const val HIST_SAMPLE_WIDTH = 64
        const val HIST_SAMPLE_HEIGHT = 36
    }
}
