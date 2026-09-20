package com.cristian.galaxyclash

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.cristian.galaxyclash.ui.GalaxyClashApp
import com.cristian.galaxyclash.ui.theme.GalaxyClashTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GalaxyClashTheme(darkTheme = true, dynamicColor = false) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    GalaxyClashApp()
                }
            }
        }
    }
}