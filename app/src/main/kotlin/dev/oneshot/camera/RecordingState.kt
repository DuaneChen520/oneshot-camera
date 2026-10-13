package dev.oneshot.camera

import android.net.Uri
import java.io.File

enum class CapturedMediaType {
    VIDEO,
    PHOTO,
    MOTION_PHOTO,
}

data class RecordingStatus(
    val isRecording: Boolean = false,
    val isPaused: Boolean = false,
    val durationSeconds: Long = 0,
    val recordedBytes: Long = 0,
    val currentFile: File? = null,
    val savedUri: Uri? = null,
    val lastError: String? = null,
    val userNotice: String? = null,
    val isStillCapturing: Boolean = false,
    val isMotionCapturing: Boolean = false,
    val lastMediaType: CapturedMediaType? = null,
    val shutterAnimationTick: Long = 0L,
)
