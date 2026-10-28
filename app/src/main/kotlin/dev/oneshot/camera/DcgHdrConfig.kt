package dev.oneshot.camera

import android.hardware.camera2.CaptureRequest

object DcgHdrConfig {
    const val QUALCOMM_DCG_VENDOR_TAG = "org.codeaurora.qcamera3.sessionParameters.EnableHDRDCGMode"

    val VENDOR_KEY: CaptureRequest.Key<Int> by lazy {
        CaptureRequest.Key(QUALCOMM_DCG_VENDOR_TAG, Int::class.java)
    }

    fun resolveDcgValue(enabled: Boolean, supported: Boolean): Int {
        return if (enabled && supported) 1 else 0
    }
}
