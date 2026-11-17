package com.jungangtuth.nexusai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import com.jungangtuth.nexusai.ui.NexusApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF7C4DFF),
                    secondary = Color(0xFF00B0FF),
                    background = Color(0xFF0B0F14),
                    surface = Color(0xFF141A22),
                    onBackground = Color(0xFFE6EDF3),
                    onSurface = Color(0xFFE6EDF3)
                )
            ) { NexusApp() }
        }
    }
}
