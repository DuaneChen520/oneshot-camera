package dev.oneshot.camera

import android.view.KeyEvent
import dev.oneshot.data.ParameterStore
import dev.oneshot.data.VolumeKeyAction

class HardwareKeyMapper(
    private val cameraController: CameraController,
    private val parameterStore: ParameterStore,
) {
    /**
     * Intercepts volume key events and routes them to user-defined camera actions.
     * @return true if the event was consumed, false to allow default system handling.
     */
    fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (event?.repeatCount ?: 0 > 0) {
            // Ignore long-press repeating events to avoid firing rapid duplicate captures
            return true
        }

        val settings = parameterStore.settings.value
        val action = when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> settings.volumeKeyUpAction
            KeyEvent.KEYCODE_VOLUME_DOWN -> settings.volumeKeyDownAction
            else -> return false
        }

        return executeAction(action)
    }

    fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN -> true
            else -> false
        }
    }

    private fun executeAction(action: VolumeKeyAction): Boolean {
        val status = cameraController.recordingStatus.value
        when (action) {
            VolumeKeyAction.STILL_SHUTTER -> {
                if (!status.isRecording && !status.isMotionCapturing) {
                    cameraController.takeStillPhoto()
                }
            }
            VolumeKeyAction.VIDEO_SHUTTER -> {
                if (status.isRecording) {
                    cameraController.stopRecording()
                } else if (!status.isStillCapturing && !status.isMotionCapturing) {
                    cameraController.startRecording()
                }
            }
            VolumeKeyAction.MOTION_PHOTO -> {
                if (!status.isRecording && !status.isStillCapturing && !status.isMotionCapturing) {
                    cameraController.captureMotionPhoto()
                }
            }
            VolumeKeyAction.ZOOM_IN -> {
                val current = cameraController.currentZoom.value
                cameraController.setZoom(current + 0.5f)
            }
            VolumeKeyAction.ZOOM_OUT -> {
                val current = cameraController.currentZoom.value
                cameraController.setZoom((current - 0.5f).coerceAtLeast(0.6f))
            }
            VolumeKeyAction.NONE -> {
                // Consumed but no-op
            }
        }
        return true
    }
}
