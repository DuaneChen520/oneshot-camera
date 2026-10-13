package dev.oneshot

import android.content.Context
import dev.oneshot.camera.CameraController
import dev.oneshot.camera.HapticManager
import dev.oneshot.data.ParameterStore

class AppGraph(context: Context) {
    val hapticManager: HapticManager = HapticManager(context)
    val parameterStore: ParameterStore = ParameterStore(context)
    val cameraController: CameraController = CameraController(context, parameterStore, hapticManager)
}
