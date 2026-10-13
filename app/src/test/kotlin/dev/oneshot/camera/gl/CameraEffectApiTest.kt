package dev.oneshot.camera.gl

import androidx.camera.core.CameraEffect
import androidx.camera.core.SurfaceOutput
import androidx.camera.core.SurfaceProcessor
import androidx.camera.core.SurfaceRequest
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull

import androidx.core.util.Consumer
import java.util.concurrent.Executor

class CameraEffectApiTest {
    @Test
    fun testCameraEffectApi() {
        val dummyProcessor = object : SurfaceProcessor {
            override fun onInputSurface(request: SurfaceRequest) {}
            override fun onOutputSurface(surfaceOutput: SurfaceOutput) {}
        }
        val effect = OneshotCameraEffect(
            CameraEffect.PREVIEW,
            { runnable -> runnable.run() },
            dummyProcessor
        )
        assertNotNull(effect)
        assertEquals(CameraEffect.PREVIEW, effect.targets)
    }
}
