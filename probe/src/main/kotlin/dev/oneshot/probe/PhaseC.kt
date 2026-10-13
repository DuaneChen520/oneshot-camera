package dev.oneshot.probe

import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import android.view.Surface
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Phase C: ask the HAL directly whether each candidate stream combination is supported,
 * before we invest in binding it through CameraX.
 *
 * Note on the plan's wording ("query without opening the camera"): that is not possible.
 * `isSessionConfigurationSupported` is an instance method on [CameraDevice], so the device
 * must be open — but no capture session is created, which keeps this far cheaper and far
 * less flaky than a real bind. Phase D remains the authoritative test.
 */
class PhaseC(private val cameraManager: CameraManager) {

    private val thread = HandlerThread("probe-phase-c").apply { start() }
    private val handler = Handler(thread.looper)

    fun close() {
        thread.quitSafely()
    }

    fun runFor(cameraId: String, chars: CameraCharacteristics): JsonObject {
        val candidates = Candidates.forCamera(chars)

        val device = runCatching { open(cameraId) }.getOrNull()
            ?: return buildJsonObject {
                put("cameraId", cameraId)
                put("error", "could not open CameraDevice")
                put("results", buildJsonArray { })
            }

        return try {
            buildJsonObject {
                put("cameraId", cameraId)
                put("candidateCount", candidates.size)
                put("results", buildJsonArray {
                    candidates.forEach { add(check(device, it)) }
                })
            }
        } finally {
            device.close()
        }
    }

    private fun check(device: CameraDevice, candidate: StreamCandidate): JsonObject {
        // Dummy surfaces must outlive the query: a collected SurfaceTexture invalidates its
        // Surface, and the configuration would then be rejected for reasons unrelated to the HAL.
        val held = mutableListOf<() -> Unit>()
        return buildJsonObject {
            put("label", candidate.label)
            put("reason", candidate.reason)
            put("recordSize", candidate.recordSize.describe())
            put("recordAspect", "%.4f".format(candidate.recordSize.aspect()))
            put("previewSize", candidate.previewSize?.describe())
            put("dynamicRangeProfile", dynamicRangeName(candidate.dynamicRangeProfile))
            put("includeStill", candidate.includeStill)
            try {
                val outputs = buildOutputs(candidate, held)
                put("outputCount", outputs.size)
                val session = SessionConfiguration(
                    SessionConfiguration.SESSION_REGULAR,
                    outputs,
                    { runnable -> runnable.run() },
                    NoOpStateCallback,
                )
                val supported = runCatching { device.isSessionConfigurationSupported(session) }
                put("isSessionConfigurationSupported", supported.getOrNull())
                put("error", supported.exceptionOrNull()?.let(::describe))
            } catch (t: Throwable) {
                put("isSessionConfigurationSupported", JsonNull)
                put("constructionError", describe(t))
            } finally {
                held.forEach { runCatching(it) }
            }
        }
    }

    private fun buildOutputs(candidate: StreamCandidate, held: MutableList<() -> Unit>): List<OutputConfiguration> {
        val outputs = mutableListOf<OutputConfiguration>()

        candidate.previewSize?.let { size ->
            outputs += privateOutput(size, held).apply { useCase(PREVIEW) }
        }

        outputs += privateOutput(candidate.recordSize, held).apply {
            useCase(VIDEO_RECORD)
            if (candidate.dynamicRangeProfile != STANDARD) {
                runCatching { setDynamicRangeProfile(candidate.dynamicRangeProfile) }
            }
        }

        if (candidate.includeStill) {
            val stillSize = stillSizeFor(candidate.recordSize)
            outputs += stillOutput(stillSize, held).apply { useCase(STILL_CAPTURE) }
        }

        return outputs
    }

    private fun privateOutput(size: Size, held: MutableList<() -> Unit>): OutputConfiguration =
        OutputConfiguration(dummySurface(held))

    /**
     * ImageReader-backed surface for the still stream. ImageReader implements [AutoCloseable],
     * so both kinds of resource go through one cleanup list.
     */
    private fun stillOutput(size: Size, held: MutableList<() -> Unit>): OutputConfiguration {
        val reader = ImageReader.newInstance(size.width, size.height, ImageFormat.JPEG, 2)
        held += { reader.close() }
        return OutputConfiguration(reader.surface)
    }

    private fun dummySurface(held: MutableList<() -> Unit>): Surface {
        val texture = SurfaceTexture(0)
        val surface = Surface(texture)
        held += { surface.release(); texture.release() }
        return surface
    }

    /** A still stream at 4:3, no taller than the record stream. */
    private fun stillSizeFor(record: Size): Size {
        val height = (record.width * 3 / 4).coerceAtLeast(1).coerceAtMost(record.height)
        return Size(record.width, height)
    }

    private fun open(cameraId: String): CameraDevice? {
        val latch = CountDownLatch(1)
        val result = AtomicReference<CameraDevice>()
        runCatching {
            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(device: CameraDevice) { result.set(device); latch.countDown() }
                override fun onDisconnected(device: CameraDevice) { device.close(); latch.countDown() }
                override fun onError(device: CameraDevice, error: Int) {
                    device.close(); latch.countDown()
                }
            }, handler)
        }
        latch.await(10, TimeUnit.SECONDS)
        return result.get()
    }

    private fun describe(t: Throwable): String = "${t.javaClass.simpleName}: ${t.message?.take(400)}"

    private object NoOpStateCallback : CameraCaptureSession.StateCallback() {
        override fun onConfigured(session: CameraCaptureSession) { /* session is never created */ }
        override fun onConfigureFailed(session: CameraCaptureSession) { /* ditto */ }
    }

    private companion object {
        const val PREVIEW = 1L
        const val VIDEO_RECORD = 3L
        const val STILL_CAPTURE = 2L
        const val STANDARD = 0x1L
    }
}

private fun OutputConfiguration.useCase(value: Long) {
    runCatching { setStreamUseCase(value) }
}

private fun Size.describe(): String = "${width}x$height"
