package dev.oneshot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Placeholder() }
    }
}

/**
 * M0 placeholder. The shooting screen lands in M1; this exists so the manifest's activity
 * target resolves and so the Compose + CameraX dependency set is proven to compile.
 */
@Composable
private fun Placeholder() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("OneShot — M0 placeholder")
    }
}
