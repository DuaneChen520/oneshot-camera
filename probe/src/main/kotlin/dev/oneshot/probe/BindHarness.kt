package dev.oneshot.probe

import android.graphics.SurfaceTexture
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.view.Surface
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.SessionConfig
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

/**
 * Shared runtime scaffolding for the bind phases.
 *
 * `bindToLifecycle` asserts the main thread while the probe body runs on a worker thread, and
 * every surface we hand to CameraX has to outlive the bind. Both live here so phases B and D
 * cannot leak one or the other.
 */
class BindHarness(
    private val owner: LifecycleOwner,
) : AutoCloseable {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val bgThread = HandlerThread("probe-bind").apply { start() }
    private val bgHandler = Handler(bgThread.looper)

    val executor: Executor = Executor { command -> bgHandler.post(command) }

    private val held = mutableListOf<AutoCloseable>()

    /** Runs [block] on the main thread and blocks this thread until it returns. */
    fun <T> onMain(block: () -> T): T {
        if (Looper.myLooper() == Looper.getMainLooper()) return block()
        val latch = CountDownLatch(1)
        var value: Any? = null
        var failure: Throwable? = null
        mainHandler.post {
            try {
                value = block()
            } catch (t: Throwable) {
                failure = t
            } finally {
                latch.countDown()
            }
        }
        if (!latch.await(MAIN_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
            throw IllegalStateException("main-thread call timed out after ${MAIN_TIMEOUT_MS}ms")
        }
        failure?.let { throw it }
        @Suppress("UNCHECKED_CAST")
        return value as T
    }

    fun bind(provider: ProcessCameraProvider, selector: CameraSelector, config: SessionConfig): Camera =
        onMain { provider.bindToLifecycle(owner, selector, config) }

    fun releaseAll(provider: ProcessCameraProvider) {
        onMain { runCatching { provider.unbindAll() } }
        synchronized(this) {
            held.forEach { runCatching { it.close() } }
            held.clear()
        }
    }

    /** An offscreen surface CameraX may render into; released by [releaseAll]. */
    fun newSurface(width: Int, height: Int): Surface {
        val texture = SurfaceTexture(0)
        texture.setDefaultBufferSize(width.coerceAtLeast(1), height.coerceAtLeast(1))
        val surface = Surface(texture)
        synchronized(this) {
            held += AutoCloseable { surface.release() }
            held += AutoCloseable { texture.release() }
        }
        return surface
    }

    fun settle(ms: Long) {
        runCatching { Thread.sleep(ms) }
    }

    override fun close() {
        bgThread.quitSafely()
    }

    private companion object {
        const val MAIN_TIMEOUT_MS = 30_000L
    }
}
