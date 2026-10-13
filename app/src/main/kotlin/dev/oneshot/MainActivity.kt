package dev.oneshot

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dev.oneshot.camera.HardwareKeyMapper
import dev.oneshot.ui.ShootingScreen

class MainActivity : ComponentActivity() {

    private lateinit var hardwareKeyMapper: HardwareKeyMapper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as OneshotApp
        hardwareKeyMapper = HardwareKeyMapper(
            cameraController = app.appGraph.cameraController,
            parameterStore = app.appGraph.parameterStore,
        )

        setContent {
            ShootingScreen(
                cameraController = app.appGraph.cameraController,
                parameterStore = app.appGraph.parameterStore,
                hapticManager = app.appGraph.hapticManager,
            )
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (::hardwareKeyMapper.isInitialized && hardwareKeyMapper.onKeyDown(keyCode, event)) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (::hardwareKeyMapper.isInitialized && hardwareKeyMapper.onKeyUp(keyCode, event)) {
            return true
        }
        return super.onKeyUp(keyCode, event)
    }
}
