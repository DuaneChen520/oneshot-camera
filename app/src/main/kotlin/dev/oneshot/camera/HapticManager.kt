package dev.oneshot.camera

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class HapticManager(private val context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    /**
     * Subtle ratchet tick when dragging dials/parameters (EV, ISO, Shutter, Zoom)
     */
    fun tick() {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                vibrator?.vibrate(
                    VibrationEffect.startComposition()
                        .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.8f, 0)
                        .compose()
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(15)
            }
        }
    }

    /**
     * Mechanical shutter click with curtain snap & mirror recoil
     */
    fun shutterClick() {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                vibrator?.vibrate(
                    VibrationEffect.startComposition()
                        .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, 0)
                        .addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, 0.75f, 10)
                        .compose()
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(35)
            }
        }
    }

    /**
     * Focus lock confirmation micro-tick
     */
    fun focusLocked() {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                vibrator?.vibrate(
                    VibrationEffect.startComposition()
                        .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.6f, 0)
                        .compose()
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(10)
            }
        }
    }

    /**
     * Stage 1: Tap to record video (light press feedback)
     */
    fun recordButtonPress() {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(30, 180))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(30)
            }
        }
    }

    /**
     * Stage 2: HAL actual start confirm (VideoRecordEvent.Start received)
     */
    fun recordingStartConfirm() {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(70, 255))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(70)
            }
        }
    }

    /**
     * Recording stop vibration
     */
    fun recordingStop() {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 40, 50, 60)
                val amplitudes = intArrayOf(0, 150, 0, 220)
                vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(100)
            }
        }
    }

    /**
     * Motion photo captured pulse
     */
    fun motionPhotoCaptured() {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 30, 40, 30)
                val amplitudes = intArrayOf(0, 200, 0, 200)
                vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(50)
            }
        }
    }
}
